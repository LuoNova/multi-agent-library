package com.library.service;

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

import static com.library.constant.LibraryConstants.*;

//还书服务（包含基础还书+预约自动兑现）
@Slf4j
@Service
public class BookReturnService {

    @Autowired
    private BookCopyMapper bookCopyMapper;

    @Autowired
    private BookBorrowMapper bookBorrowMapper;

    @Autowired
    private BookReservationMapper bookReservationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LibraryBiblioStatsMapper statsMapper;

    @Autowired
    private BookTransferMapper bookTransferMapper;

    //核心方法：执行还书（包含预约检查与自动兑现）
    @Transactional
    public ReturnResult processReturn(Long copyId, Long userId, Long returnLibraryId) {
        //1. 校验：确认该书当前被此用户借走
        BookBorrow currentBorrow = bookBorrowMapper.selectCurrentBorrowByCopyId(copyId);
        if (currentBorrow == null) {
            return ReturnResult.fail("该书当前无借阅记录，无需归还");
        }
        if (!currentBorrow.getUserId().equals(userId)) {
            return ReturnResult.fail("非借书人本人，无法还书（当前借书人ID: " + currentBorrow.getUserId() + "）");
        }
        if (!LibraryConstants.BORROW_STATUS_BORROWING.equals(currentBorrow.getStatus())) {
            return ReturnResult.fail("该书当前状态非借阅中，无法归还");
        }

        //获取副本信息
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null) {
            return ReturnResult.fail("副本不存在");
        }
        Long biblioId = copy.getBiblioId();

        log.info("处理还书：副本{}，书目{}，用户{}，还书馆{}",
                copyId, biblioId, userId, returnLibraryId);

        //2. 基础还书（释放库存）
        //2.1 更新副本状态为AVAILABLE（临时，如有预约会再改）
        copy.setLibraryId(returnLibraryId);
        copy.setStatus(LibraryConstants.COPY_STATUS_AVAILABLE);
        copy.setUpdateTime(LocalDateTime.now());
        bookCopyMapper.updateById(copy);

        //2.2 更新借阅记录为已归还
        currentBorrow.setStatus(LibraryConstants.BORROW_STATUS_RETURNED);
        currentBorrow.setReturnTime(LocalDateTime.now());
        bookBorrowMapper.updateById(currentBorrow);

        //2.3 减少还书人借阅计数
        User returnUser = userMapper.selectById(userId);
        if (returnUser != null && returnUser.getCurrentBorrowCount() > 0) {
            returnUser.setCurrentBorrowCount(returnUser.getCurrentBorrowCount() - 1);
            userMapper.updateById(returnUser);
        }

        //2.4 增加还书馆库存统计
        LibraryBiblioStats stats = statsMapper.selectByLibraryAndBiblio(returnLibraryId, biblioId);
        if (stats != null) {
            stats.setStockCount(stats.getStockCount() + 1);
            stats.setLastCalculatedTime(LocalDateTime.now());
            statsMapper.updateById(stats);
        }

        //2.5信用分计算（新增：逾期扣分逻辑）
        checkCredit(userId, currentBorrow, returnUser);

        log.info("基础还书完成，副本{}已释放至馆{}", copyId, returnLibraryId);

        //3.循环处理预约队列（关键修复：递归查找有效预约者）
        boolean isLocal = false;

        while (true) {
            //获取当前第一个待处理预约
            BookReservation reservation = bookReservationMapper.selectFirstPendingByBiblio(biblioId);
            if (reservation == null) {
                //无预约，退出循环
                break;
            }

            Long reservatorId = reservation.getUserId();
            User reservator = userMapper.selectById(reservatorId);

            //检查预约者是否具备借书资格
            boolean canBorrow = reservator != null
                    && reservator.getCurrentBorrowCount() < reservator.getMaxBorrowCount()
                    && !"FROZEN".equals(reservator.getStatus());

            if (!canBorrow) {
                //当前预约者无效，取消并继续查找下一个
                reservation.setStatus(LibraryConstants.RESERVATION_STATUS_CANCELED);
                bookReservationMapper.updateById(reservation);

                log.warn("预约者{}无法借书（当前借阅{}/{}或状态{}），自动取消并查找下一个",
                        reservatorId,
                        reservator != null ? reservator.getCurrentBorrowCount() : "N/A",
                        reservator != null ? reservator.getMaxBorrowCount() : "N/A",
                        reservator != null ? reservator.getStatus() : "NULL");
                continue; //关键：继续while循环查找下一个预约者
            }

            //找到有效预约者，执行兑现逻辑
            isLocal = reservation.getPickupLibraryId().equals(returnLibraryId);

            if (isLocal) {
                //场景B：就地预留，直接返回结果
                return processLocalReservation(copy, reservation, reservatorId, returnLibraryId);
            } else {
                //场景C：跨馆调拨，直接返回结果
                return processTransferReservation(copy, reservation, reservatorId, returnLibraryId,
                        reservation.getPickupLibraryId());
            }
            //注意：break已不需要，因为return直接结束方法
        }

        //场景A：无预约等待（只有while循环正常结束未return时才会执行到这里）
        return ReturnResult.success("还书成功，书已回到馆" + returnLibraryId + "架上");
    }

    //验证还书时是否逾期
    private void checkCredit(Long userId, BookBorrow currentBorrow, User returnUser) {
        LocalDateTime dueTime = currentBorrow.getDueTime();
        if (dueTime.isBefore(LocalDateTime.now())) {
            long daysOverdue = java.time.Duration.between(dueTime, LocalDateTime.now()).toDays();
            int penalty = (int) Math.min(daysOverdue * BORROW_DAILY_DEDUCTION,
                    BORROW_CEILING_DEDUCTION);

            returnUser.setCreditScore(Math.max(0, returnUser.getCreditScore() - penalty));
            userMapper.updateById(returnUser);

            log.info("用户{}逾期{}天，信用分扣除{}，当前信用分{}", userId, daysOverdue, penalty, returnUser.getCreditScore());

            //如果信用分过低，冻结账号
            if (returnUser.getCreditScore() < 60 && !"FROZEN".equals(returnUser.getStatus())) {
                returnUser.setStatus("FROZEN");
                userMapper.updateById(returnUser);
                log.warn("用户{}信用分过低，账号已冻结", userId);
            }
        }
    }

    //处理就地预留（预约者在还书馆取书）
    private ReturnResult processLocalReservation(BookCopy copy, BookReservation reservation,
                                                 Long reservatorId, Long libraryId) {
        log.info("执行就地预留：副本{}直接预留给用户{}", copy.getId(), reservatorId);

        //TODO: 发送通知给预约者（短信/邮件/微信/站内信），告知"您预约的《书名》已到位，请在24小时内到馆取书"

        //1. 更新副本为预留状态（不再开放借阅）
        copy.setStatus(LibraryConstants.COPY_STATUS_RESERVED);
        copy.setUpdateTime(LocalDateTime.now());
        bookCopyMapper.updateById(copy);

        //2. 更新预约记录为已满足
        reservation.setStatus(LibraryConstants.RESERVATION_STATUS_FULFILLED);
        reservation.setCopyId(copy.getId());
        reservation.setFulfillTime(LocalDateTime.now());
        bookReservationMapper.updateById(reservation);

        //3. 为预约者创建借阅记录（状态为RESERVED，24小时内有效）
        BookBorrow newBorrow = new BookBorrow();
        newBorrow.setCopyId(copy.getId());
        newBorrow.setUserId(reservatorId);
        newBorrow.setBorrowTime(LocalDateTime.now());
        newBorrow.setDueTime(LocalDateTime.now().plusHours(RESERVE_HOURS));
        newBorrow.setStatus(LibraryConstants.BORROW_STATUS_RESERVED);
        bookBorrowMapper.insert(newBorrow);

        //4. 增加预约者借阅计数
        User reservator = userMapper.selectById(reservatorId);
        reservator.setCurrentBorrowCount(reservator.getCurrentBorrowCount() + 1);
        userMapper.updateById(reservator);

        //5. 减少可用库存（因为被预留了，虽然还在同一馆，但状态变了）
        //注意：基础还书时已经+1了，现在预留了，应该保持库存数不变（只是状态从AVAILABLE变RESERVED）
        //但如果严格区分AVAILABLE和RESERVED的库存统计，可能需要调整stats的available_count（如果有这个字段）
        //目前stats只有stock_count，表示物理数量，不区分状态，所以不需要调整

        log.info("就地预留完成，用户{}需在" + RESERVE_HOURS + "小时内取书", reservatorId);

        //修改为增强版构造方法，添加reservationId和过期时间
        return ReturnResult.successWithReservation(
                "还书成功，书已直接预留给预约用户（" + RESERVE_HOURS + "小时内有效）",
                reservatorId,
                true,
                reservation.getId(),
                LocalDateTime.now().plusHours(RESERVE_HOURS)
        );
    }

    //处理跨馆调拨（预约者在其他馆）
    private ReturnResult processTransferReservation(BookCopy copy, BookReservation reservation,
                                                    Long reservatorId, Long fromLibraryId, Long toLibraryId) {
        log.info("执行跨馆调拨预约兑现：副本{}从馆{}调至馆{}给用户{}",
                copy.getId(), fromLibraryId, toLibraryId, reservatorId);

        //TODO: 发送通知给预约者（短信/邮件/微信/站内信），告知"您预约的《书名》已到位，请在24小时内到馆取书"

        //1. 创建调拨记录（运输中）
        BookTransfer transfer = new BookTransfer();
        transfer.setCopyId(copy.getId());
        transfer.setFromLibraryId(fromLibraryId);
        transfer.setToLibraryId(toLibraryId);
        transfer.setStatus(LibraryConstants.TRANSFER_STATUS_IN_TRANSIT);
        transfer.setRequestTime(LocalDateTime.now());
        //预计到达时间（假设30分钟运输时间）
        //transfer.setEstimatedArrival(LocalDateTime.now().plusMinutes(30));
        bookTransferMapper.insert(transfer);

        //2. 更新副本为预留状态（权属已归预约者，但物理位置还在原馆，即将运输）
        //注意：虽然物理还在fromLibraryId，但status改为RESERVED表示已被占用
        copy.setStatus(LibraryConstants.COPY_STATUS_RESERVED);
        copy.setUpdateTime(LocalDateTime.now());
        bookCopyMapper.updateById(copy);

        //3. 更新预约记录为已满足（已分配副本，但在途）
        reservation.setStatus(LibraryConstants.RESERVATION_STATUS_FULFILLED);
        reservation.setCopyId(copy.getId());
        reservation.setFulfillTime(LocalDateTime.now());
        bookReservationMapper.updateById(reservation);

        //4. 调整库存统计：
        //原馆（还书馆）库存-1（因为书被调走了，虽然还没运走，但权属已转移）
        LibraryBiblioStats fromStats = statsMapper.selectByLibraryAndBiblio(fromLibraryId, copy.getBiblioId());
        if (fromStats != null) {
            fromStats.setStockCount(fromStats.getStockCount() - 1);
            statsMapper.updateById(fromStats);
        }

        //目标馆（预约者馆）库存+1（虽然书还没到，但为了统计一致性，先加？或者等调拨完成再加？）
        //建议：等调拨完成（到货）后再加，避免虚高。但这里为了简化，先不加，保持原样。
        //或者添加"在途库存"概念（复杂，暂不实现）

        //5. 注意：此时不创建tb_book_borrow记录（等调拨完成到货后，用户实际取书时再创建）
        //或者可以创建，但状态特殊？目前按业务逻辑，调拨完成后用户收到通知，取书时才创建正式借阅。

        log.info("跨馆调拨预约兑现完成，调拨单ID：{}，预计30分钟到达馆{}",
                transfer.getId(), toLibraryId);

        //修改为增强版构造方法，添加reservationId、transferId和过期时间
        return ReturnResult.successWithTransfer(
                "还书成功，已触发向预约用户指定馆的调拨（预计30分钟到达）",
                reservatorId,
                reservation.getId(),
                transfer.getId(),
                LocalDateTime.now().plusHours(RESERVE_HOURS)
        );
    }

    //还书结果内部类（增强版）
    @Data
    public static class ReturnResult {
        private boolean success;
        private String status; //SUCCESS/FAILED
        private String message;
        private boolean hasReservation;
        private Long reservatorId;
        private boolean localReservation;
        private Long reservationId; //新增：预约记录ID
        private LocalDateTime reserveExpireTime; //新增：预留过期时间（24小时后）
        private Long transferId; //新增：调拨单ID（仅跨馆时有）

        public static ReturnResult success(String msg) {
            ReturnResult r = new ReturnResult();
            r.success = true;
            r.status = "SUCCESS";
            r.message = msg;
            r.hasReservation = false;
            return r;
        }

        //就地预留专用构造（增强参数）
        public static ReturnResult successWithReservation(String msg, Long reservatorId,
                                                          boolean local, Long reservationId,
                                                          LocalDateTime expireTime) {
            ReturnResult r = new ReturnResult();
            r.success = true;
            r.status = "SUCCESS";
            r.message = msg;
            r.hasReservation = true;
            r.reservatorId = reservatorId;
            r.localReservation = local;
            r.reservationId = reservationId;
            r.reserveExpireTime = expireTime;
            return r;
        }

        //跨馆调拨专用构造（增强参数）
        public static ReturnResult successWithTransfer(String msg, Long reservatorId,
                                                       Long reservationId, Long transferId,
                                                       LocalDateTime expireTime) {
            ReturnResult r = new ReturnResult();
            r.success = true;
            r.status = "SUCCESS";
            r.message = msg;
            r.hasReservation = true;
            r.reservatorId = reservatorId;
            r.localReservation = false;
            r.reservationId = reservationId;
            r.transferId = transferId;
            r.reserveExpireTime = expireTime;
            return r;
        }

        public static ReturnResult fail(String msg) {
            ReturnResult r = new ReturnResult();
            r.success = false;
            r.status = "FAILED";
            r.message = msg;
            return r;
        }
    }
}