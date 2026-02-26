package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.constant.LibraryConstants;
import com.library.entity.*;
import com.library.mapper.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.library.constant.LibraryConstants.TRANSFER_MINUTES;

//调拨履约服务（TransferAgent）
//职责：管理调拨全生命周期（创建->运输->完成），确保库存、借阅记录、用户计数的一致性
@Slf4j
@Service
public class TransferService {

    @Autowired
    private BookTransferMapper transferMapper;
    @Autowired
    private BookCopyMapper copyMapper;
    @Autowired
    private LibraryBiblioStatsMapper statsMapper;
    @Autowired
    private BookReservationMapper reservationMapper;
    @Autowired
    private BookBorrowMapper bookBorrowMapper;
    @Autowired
    private UserMapper userMapper;

    //创建调拨记录（还书时调用，替代原BookTransferService.createTransferRecord）
    //修改说明：统一使用IN_TRANSIT状态（跳过PENDING），简化流程
    @Transactional
    public Long createTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long reservationId) {
        log.info("创建调拨记录：副本{}从馆{}到馆{}，关联预约{}",
                copyId, fromLibraryId, toLibraryId, reservationId);

        BookTransfer transfer = new BookTransfer();
        transfer.setCopyId(copyId);
        transfer.setFromLibraryId(fromLibraryId);
        transfer.setToLibraryId(toLibraryId);
        transfer.setStatus(LibraryConstants.TRANSFER_STATUS_IN_TRANSIT);
        transfer.setRequestTime(LocalDateTime.now());
        //预计30分钟后到达（用于定时任务自动完成）
        transfer.setCompleteTime(LocalDateTime.now().plusMinutes(TRANSFER_MINUTES));

        transferMapper.insert(transfer);
        log.info("调拨单创建成功：{}", transfer.getId());

        return transfer.getId();
    }

    //调拨完成回调（核心方法）
    //由物流系统、定时任务或馆员确认调用，完成调拨闭环
    @Transactional
    public TransferCompleteResult completeTransfer(Long transferId, LocalDateTime actualArriveTime) {
        log.info("处理调拨完成回调：transferId={}, 实际到达时间={}", transferId, actualArriveTime);

        //1.校验调拨记录
        BookTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            log.error("调拨单不存在：{}", transferId);
            return TransferCompleteResult.fail("调拨单不存在");
        }

        //校验状态必须为运输中
        if (!LibraryConstants.TRANSFER_STATUS_IN_TRANSIT.equals(transfer.getStatus())) {
            log.warn("调拨单状态异常：当前{}, 期望IN_TRANSIT", transfer.getStatus());
            return TransferCompleteResult.fail("调拨单状态异常，当前状态：" + transfer.getStatus());
        }

        //2.更新调拨单状态为已完成
        transfer.setStatus(LibraryConstants.TRANSFER_STATUS_COMPLETED);
        transfer.setCompleteTime(actualArriveTime != null ? actualArriveTime : LocalDateTime.now());
        transferMapper.updateById(transfer);
        log.info("调拨单{}状态更新为COMPLETED", transferId);

        //3.更新副本物理位置到目标馆，状态改为预留（等待用户取书）
        BookCopy copy = copyMapper.selectById(transfer.getCopyId());
        if (copy == null) {
            log.error("调拨关联的副本不存在：{}", transfer.getCopyId());
            return TransferCompleteResult.fail("副本不存在");
        }

        copy.setLibraryId(transfer.getToLibraryId());
        copy.setStatus(LibraryConstants.COPY_STATUS_RESERVED);
        //修改点：设置默认位置（方案B），馆员后续可通过管理后台修改到精确位置
        copy.setLocation("馆" + transfer.getToLibraryId() + "-调拨暂存区");
        copy.setUpdateTime(LocalDateTime.now());
        copyMapper.updateById(copy);
        log.info("副本{}已移至馆{}，状态改为RESERVED", copy.getId(), transfer.getToLibraryId());

        //4.目标馆库存+1（物理到货）
        LibraryBiblioStats stats = statsMapper.selectByLibraryAndBiblio(
                transfer.getToLibraryId(), copy.getBiblioId());
        if (stats != null) {
            stats.setStockCount(stats.getStockCount() + 1);
            stats.setLastCalculatedTime(LocalDateTime.now());
            statsMapper.updateById(stats);
            log.info("馆{}库存+1，当前库存{}", transfer.getToLibraryId(), stats.getStockCount());
        } else {
            //极端情况：目标馆没有该书记录，新建统计记录
            stats = new LibraryBiblioStats();
            stats.setLibraryId(transfer.getToLibraryId());
            stats.setBiblioId(copy.getBiblioId());
            stats.setStockCount(1);
            stats.setLastCalculatedTime(LocalDateTime.now());
            statsMapper.insert(stats);
            log.info("为目标馆{}新建书目{}的统计记录，库存1", transfer.getToLibraryId(), copy.getBiblioId());
        }

        //5.查找关联的接收人（兼容借书调拨和还书调拨两种场景）
        //借书调拨：发起时创建了Borrow(TRANSFERRING)，直接查Borrow表
        //还书调拨：发起时创建了Reservation(FULFILLED)，查Reservation表
        BookBorrow existingBorrow = bookBorrowMapper.selectOne(
                new LambdaQueryWrapper<BookBorrow>()
                        .eq(BookBorrow::getCopyId, copy.getId())
                        .eq(BookBorrow::getStatus, LibraryConstants.BORROW_STATUS_TRANSFERRING)
        );

        Long reservatorId;
        boolean isBorrowTransfer = false; //标记是否为借书调拨（发起时已创建Borrow）

        if (existingBorrow != null) {
            //场景A：借书调拨（UserDemandAgent发起）
            reservatorId = existingBorrow.getUserId();
            isBorrowTransfer = true;
            log.info("借书调拨完成：找到运输中的Borrow记录，用户{}", reservatorId);
        } else {
            //场景B：还书调拨（BookReturnService发起）
            BookReservation reservation = reservationMapper.selectByCopyIdAndStatus(
                    copy.getId(), LibraryConstants.RESERVATION_STATUS_FULFILLED);

            if (reservation == null) {
                log.error("调拨完成但未找到关联记录，副本{}", copy.getId());
                return TransferCompleteResult.fail("未找到关联的借阅或预约记录");
            }
            reservatorId = reservation.getUserId();
            log.info("还书调拨完成：找到预约记录，用户{}", reservatorId);
        }

//6.创建或更新借阅记录（24小时预留倒计时开始）
        LocalDateTime expireTime = LocalDateTime.now().plusHours(LibraryConstants.RESERVE_HOURS);
        Long borrowId;

        if (isBorrowTransfer) {
            //借书调拨：更新已有Borrow记录（TRANSFERRING -> RESERVED）
            existingBorrow.setStatus(LibraryConstants.BORROW_STATUS_RESERVED);
            existingBorrow.setDueTime(expireTime); //正式启动24小时倒计时
            bookBorrowMapper.updateById(existingBorrow);
            borrowId = existingBorrow.getId();
            log.info("更新Borrow记录{}为RESERVED状态，24小时倒计时开始", borrowId);

            //注意：借书调拨的用户借阅计数在发起时已增加，这里不再增加
        } else {
            //还书调拨：创建新的Borrow记录（直接RESERVED）
            BookBorrow newBorrow = new BookBorrow();
            newBorrow.setCopyId(copy.getId());
            newBorrow.setUserId(reservatorId);
            newBorrow.setBorrowTime(LocalDateTime.now());
            newBorrow.setDueTime(expireTime);
            newBorrow.setStatus(LibraryConstants.BORROW_STATUS_RESERVED);
            bookBorrowMapper.insert(newBorrow);
            borrowId = newBorrow.getId();
            log.info("创建Borrow记录{}（还书调拨），24小时倒计时开始", borrowId);

            //还书调拨需要增加用户借阅计数（借书调拨在发起时已增加）
            User user = userMapper.selectById(reservatorId);
            if (user != null) {
                user.setCurrentBorrowCount(user.getCurrentBorrowCount() + 1);
                userMapper.updateById(user);
                log.info("用户{}借阅计数+1（还书调拨场景）", reservatorId);
            }
        }

//7.发送取书通知
        log.info("发送通知给用户{}：图书已到达馆{}-{}，请在24小时内取书",
                reservatorId, transfer.getToLibraryId(), copy.getLocation());

        return TransferCompleteResult.success(
                "调拨完成，图书已预留给用户" + reservatorId,
                borrowId,
                expireTime
        );
    }

    //调拨完成结果内部类
    @Data
    public static class TransferCompleteResult {
        private boolean success;
        private String message;
        private Long borrowId; //创建的预留借阅记录ID
        private LocalDateTime reserveExpireTime; //预留过期时间

        public static TransferCompleteResult success(String msg, Long borrowId, LocalDateTime expireTime) {
            TransferCompleteResult r = new TransferCompleteResult();
            r.success = true;
            r.message = msg;
            r.borrowId = borrowId;
            r.reserveExpireTime = expireTime;
            return r;
        }

        public static TransferCompleteResult fail(String msg) {
            TransferCompleteResult r = new TransferCompleteResult();
            r.success = false;
            r.message = msg;
            return r;
        }
    }
}