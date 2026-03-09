package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.config.BusinessRulesProperties;
import com.library.constant.LibraryConstants;
import com.library.entity.*;
import com.library.mapper.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//调拨履约服务（职责：管理调拨全生命周期（创建->运输->完成），确保库存、借阅记录、用户计数的一致性）
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
    @Autowired
    private TransferOrderMapper orderMapper;

    @Autowired
    private TransferNotificationService notificationService;

    @Autowired
    private BookTransferService bookTransferService;

    @Autowired
    private BusinessRulesProperties businessRulesProperties;

    //创建调拨记录（还书时调用，替代原BookTransferService.createTransferRecord）
    //修改说明：统一使用IN_TRANSIT状态（跳过PENDING），简化流程
    @Transactional
    //创建调拨记录（原有方法，保持向后兼容）
    public Long createTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long reservationId) {
        return createTransfer(copyId, fromLibraryId, toLibraryId, reservationId, "USER_REQUEST", null, null, null);
    }

    //创建调拨记录(用户请求场景,写入接收用户ID便于查询个人调拨列表)
    public Long createTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long reservationId, Long receiverUserId) {
        return createTransfer(copyId, fromLibraryId, toLibraryId, reservationId, "USER_REQUEST", null, null, receiverUserId);
    }

    //创建调拨记录（增强方法，支持指定调拨原因）
    public Long createTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long reservationId,
                               String transferReason, Long suggestionId) {
        return createTransfer(copyId, fromLibraryId, toLibraryId, reservationId, transferReason, suggestionId, null, null);
    }

    //创建调拨记录（完整方法，支持指定调拨原因、建议ID和调拨单ID）
    public Long createTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long reservationId,
                               String transferReason, Long suggestionId, Long orderId) {
        return createTransfer(copyId, fromLibraryId, toLibraryId, reservationId, transferReason, suggestionId, orderId, null);
    }

    //创建调拨记录（完整方法，支持指定调拨原因、建议ID、调拨单ID和接收用户ID）
    public Long createTransfer(Long copyId, Long fromLibraryId, Long toLibraryId, Long reservationId,
                               String transferReason, Long suggestionId, Long orderId, Long receiverUserId) {
        log.info("创建调拨记录：副本{}从馆{}到馆{}，关联预约{}，调拨原因{}，建议ID{}，调拨单ID{}",
                copyId, fromLibraryId, toLibraryId, reservationId, transferReason, suggestionId, orderId);

        BookTransfer transfer = new BookTransfer();
        LocalDateTime now = LocalDateTime.now();
        transfer.setCopyId(copyId);
        transfer.setFromLibraryId(fromLibraryId);
        transfer.setToLibraryId(toLibraryId);
        transfer.setStatus(LibraryConstants.TRANSFER_STATUS_IN_TRANSIT);
        transfer.setRequestTime(now);
        //预计到达时间用于进度计算与延迟检测
        transfer.setEstimatedArrivalTime(now.plusMinutes(
                businessRulesProperties.getTransfer().getEstimatedMinutes()));
        //设置调拨原因、建议ID和调拨单ID
        transfer.setTransferReason(transferReason);
        transfer.setSuggestionId(suggestionId);
        transfer.setOrderId(orderId);
        transfer.setReceiverUserId(receiverUserId);

        transferMapper.insert(transfer);
        log.info("调拨记录创建成功：{}", transfer.getId());

        //用户请求调拨可发送发起通知(库存平衡调拨不发送)
        if (receiverUserId != null) {
            notificationService.sendTransferInitiatedNotification(transfer.getId(), receiverUserId);
        }

        return transfer.getId();
    }

    //调拨完成回调（核心方法）
    //由物流系统、定时任务或馆员确认调用，完成调拨闭环
    @Transactional
    public TransferCompleteResult completeTransfer(Long transferId) {
        log.info("处理调拨完成回调：transferId={}", transferId);

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
        LocalDateTime arriveTime = LocalDateTime.now();
        //completeTime用于记录业务完成时间，actualArrivalTime用于进度展示
        transfer.setCompleteTime(arriveTime);
        transfer.setActualArrivalTime(arriveTime);
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
        LocalDateTime expireTime = LocalDateTime.now().plusHours(
                businessRulesProperties.getTransfer().getReserveHours());
        Long borrowId;

        if (isBorrowTransfer) {
            //借书调拨：更新已有Borrow记录（TRANSFERRING -> RESERVED）
            existingBorrow.setStatus(LibraryConstants.BORROW_STATUS_RESERVED);
            existingBorrow.setReservedTime(LocalDateTime.now()); // 预留开始时间
            existingBorrow.setPickupDeadline(expireTime); // 取书截止时间
            //dueTime不能为空，用于承载预留到期时间（取书后会改为30天应还期）
            existingBorrow.setDueTime(expireTime);
            existingBorrow.setPickupLibraryId(transfer.getToLibraryId()); // 取书馆ID
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
            newBorrow.setReservedTime(LocalDateTime.now()); // 预留开始时间
            newBorrow.setPickupDeadline(expireTime); // 取书截止时间
            //dueTime不能为空，用于承载预留到期时间（取书后会改为30天应还期）
            newBorrow.setDueTime(expireTime);
            newBorrow.setPickupLibraryId(transfer.getToLibraryId()); // 取书馆ID
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
        //仅对用户请求调拨发送通知，库存平衡调拨不发送
        notificationService.sendTransferArrivedNotification(transferId, reservatorId);

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

    //批量调拨完成方法
    @Transactional
    public BatchCompleteResult completeBatchTransfer(Long orderId) {
        log.info("处理批量调拨完成回调：orderId={}", orderId);

        //1.校验调拨单
        TransferOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("调拨单不存在：{}", orderId);
            return BatchCompleteResult.fail("调拨单不存在");
        }

        //校验状态必须为IN_PROGRESS
        if (!"IN_PROGRESS".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
            log.warn("调拨单状态异常：当前{}, 期望IN_PROGRESS或COMPLETED", order.getStatus());
            return BatchCompleteResult.fail("调拨单状态异常，当前状态：" + order.getStatus());
        }

        //2.查询所有调拨记录
        List<BookTransfer> transfers = transferMapper.selectList(
                new LambdaQueryWrapper<BookTransfer>()
                        .eq(BookTransfer::getOrderId, orderId)
                        .eq(BookTransfer::getStatus, LibraryConstants.TRANSFER_STATUS_IN_TRANSIT)
        );

        if (transfers.isEmpty()) {
            log.warn("调拨单{}没有运输中的调拨记录", orderId);
            return BatchCompleteResult.fail("没有运输中的调拨记录");
        }

        log.info("找到{}条运输中的调拨记录", transfers.size());

        //3.批量更新调拨记录状态
        LocalDateTime completeTime = LocalDateTime.now();
        int successCount = 0;

        for (BookTransfer transfer : transfers) {
            transfer.setStatus(LibraryConstants.TRANSFER_STATUS_COMPLETED);
            transfer.setCompleteTime(completeTime);
            transfer.setActualArrivalTime(completeTime);
            transferMapper.updateById(transfer);
            successCount++;
        }

        log.info("成功更新{}条调拨记录状态为COMPLETED", successCount);

        //4.批量更新副本状态
        for (BookTransfer transfer : transfers) {
            BookCopy copy = copyMapper.selectById(transfer.getCopyId());
            if (copy == null) {
                log.error("副本不存在：{}", transfer.getCopyId());
                continue;
            }

            copy.setLibraryId(transfer.getToLibraryId());
            copy.setStatus(LibraryConstants.COPY_STATUS_AVAILABLE); //直接上架
            copy.setLocation("馆" + transfer.getToLibraryId() + "-调拨暂存区");
            copy.setUpdateTime(LocalDateTime.now());
            copyMapper.updateById(copy);

            log.info("副本{}已移至馆{}，状态改为AVAILABLE", copy.getId(), transfer.getToLibraryId());
        }

        //5.更新目标馆库存
        LibraryBiblioStats stats = statsMapper.selectByLibraryAndBiblio(
                order.getToLibraryId(), order.getBiblioId());

        if (stats != null) {
            stats.setStockCount(stats.getStockCount() + successCount);
            stats.setLastCalculatedTime(LocalDateTime.now());
            statsMapper.updateById(stats);
            log.info("馆{}库存+{}，当前库存{}", order.getToLibraryId(), successCount, stats.getStockCount());
        } else {
            //极端情况：目标馆没有该书记录，新建统计记录
            stats = new LibraryBiblioStats();
            stats.setLibraryId(order.getToLibraryId());
            stats.setBiblioId(order.getBiblioId());
            stats.setStockCount(successCount);
            stats.setLastCalculatedTime(LocalDateTime.now());
            statsMapper.insert(stats);
            log.info("为目标馆{}新建书目{}的统计记录，库存{}", order.getToLibraryId(), order.getBiblioId(), successCount);
        }

        //6.更新调拨单状态
        order.setStatus("COMPLETED");
        order.setActualQuantity(successCount);
        order.setCompleteTime(completeTime);
        orderMapper.updateById(order);

        log.info("调拨单{}状态更新为COMPLETED，实际调拨{}本", orderId, successCount);

        return BatchCompleteResult.success(
                "批量调拨完成，成功调拨" + successCount + "本",
                orderId,
                successCount
        );
    }

    //批量调拨完成结果内部类
    @Data
    public static class BatchCompleteResult {
        private boolean success;
        private String message;
        private Long orderId; //调拨单ID
        private Integer completedCount; //成功调拨数量

        public static BatchCompleteResult success(String msg, Long orderId, Integer count) {
            BatchCompleteResult r = new BatchCompleteResult();
            r.success = true;
            r.message = msg;
            r.orderId = orderId;
            r.completedCount = count;
            return r;
        }

        public static BatchCompleteResult fail(String msg) {
            BatchCompleteResult r = new BatchCompleteResult();
            r.success = false;
            r.message = msg;
            return r;
        }
    }
}