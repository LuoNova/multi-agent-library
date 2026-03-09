package com.library.service;

import com.library.constant.LibraryConstants;
import com.library.entity.BookBorrow;
import com.library.entity.BookCopy;
import com.library.entity.BookReservation;
import com.library.entity.User;
import com.library.exception.BusinessException;
import com.library.mapper.BookBorrowMapper;
import com.library.mapper.BookCopyMapper;
import com.library.mapper.BookReservationMapper;
import com.library.mapper.UserMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.library.constant.LibraryConstants.RESERVE_HOURS;

//预约队列兑现服务(复用到还书/超期释放等场景)
@Slf4j
@Service
public class ReservationFulfillmentService {

    @Autowired
    private BookReservationMapper reservationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private BookBorrowMapper borrowMapper;

    @Autowired
    private TransferService transferService;

    @Autowired
    private BookCopyService bookCopyService;

    //为某个AVAILABLE副本尝试兑现下一位预约者
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentResult fulfillNextForAvailableCopy(Long copyId) {
        BookCopy copy = copyMapper.selectById(copyId);
        if (copy == null) {
            throw new BusinessException("副本不存在,copyId=" + copyId);
        }
        if (!LibraryConstants.COPY_STATUS_AVAILABLE.equals(copy.getStatus())) {
            return FulfillmentResult.none("副本当前非AVAILABLE,无需兑现");
        }

        Long biblioId = copy.getBiblioId();
        Long currentLibraryId = copy.getLibraryId();

        while (true) {
            BookReservation reservation = reservationMapper.selectFirstPendingByBiblio(biblioId);
            if (reservation == null) {
                return FulfillmentResult.none("无待处理预约");
            }

            Long reservatorId = reservation.getUserId();
            User reservator = userMapper.selectById(reservatorId);
            boolean canBorrow = reservator != null
                    && reservator.getCurrentBorrowCount() < reservator.getMaxBorrowCount()
                    && LibraryConstants.USER_STATUS_ACTIVE.equals(reservator.getStatus());

            if (!canBorrow) {
                //当前预约者无效,取消并继续查找下一位
                reservation.setStatus(LibraryConstants.RESERVATION_STATUS_CANCELED);
                reservationMapper.updateById(reservation);
                log.warn("预约者{}无法借书,自动取消并查找下一位", reservatorId);
                continue;
            }

            boolean isLocal = reservation.getPickupLibraryId().equals(currentLibraryId);
            if (isLocal) {
                return fulfillLocal(copy, reservation, reservatorId, currentLibraryId);
            } else {
                return fulfillByTransfer(copy, reservation, reservatorId, currentLibraryId, reservation.getPickupLibraryId());
            }
        }
    }

    //就地预留兑现
    private FulfillmentResult fulfillLocal(BookCopy copy, BookReservation reservation,
                                          Long reservatorId, Long currentLibraryId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusHours(RESERVE_HOURS);

        //1.副本置为RESERVED,到馆待取
        copy.setStatus(LibraryConstants.COPY_STATUS_RESERVED);
        copy.setUpdateTime(now);
        copyMapper.updateById(copy);

        //2.预约置为FULFILLED并绑定copy
        reservation.setStatus(LibraryConstants.RESERVATION_STATUS_FULFILLED);
        reservation.setCopyId(copy.getId());
        reservation.setFulfillTime(now);
        reservationMapper.updateById(reservation);

        //3.创建Borrow(RESERVED),写入pickupDeadline用于超期释放
        BookBorrow borrow = new BookBorrow();
        borrow.setCopyId(copy.getId());
        borrow.setUserId(reservatorId);
        borrow.setBorrowTime(now);
        borrow.setReservedTime(now);
        borrow.setPickupDeadline(expireTime);
        borrow.setPickupLibraryId(currentLibraryId);
        //dueTime不能为空,预留阶段承载到期时间
        borrow.setDueTime(expireTime);
        borrow.setStatus(LibraryConstants.BORROW_STATUS_RESERVED);
        borrowMapper.insert(borrow);

        //4.用户借阅计数+1
        User user = userMapper.selectById(reservatorId);
        user.setCurrentBorrowCount(user.getCurrentBorrowCount() + 1);
        userMapper.updateById(user);

        log.info("就地预留兑现完成: reservationId={}, borrowId={}", reservation.getId(), borrow.getId());
        return FulfillmentResult.localReserved(reservation.getId(), borrow.getId(), expireTime);
    }

    //跨馆调拨兑现(书进入运输中,到货后由TransferService.completeTransfer创建Borrow并计数+1)
    private FulfillmentResult fulfillByTransfer(BookCopy copy, BookReservation reservation,
                                                Long reservatorId, Long fromLibraryId, Long toLibraryId) {
        LocalDateTime now = LocalDateTime.now();

        //1.预约置为FULFILLED并绑定copy(completeTransfer会通过copyId查到该预约)
        reservation.setStatus(LibraryConstants.RESERVATION_STATUS_FULFILLED);
        reservation.setCopyId(copy.getId());
        reservation.setFulfillTime(now);
        reservationMapper.updateById(reservation);

        //2.创建调拨记录(确保estimatedArrivalTime写入)
        //写入接收用户ID,用于用户调拨列表查询
        Long transferId = transferService.createTransfer(copy.getId(), fromLibraryId, toLibraryId, reservation.getId(), reservatorId);

        //3.执行调拨(副本置IN_TRANSIT,源馆库存-1)
        boolean success = bookCopyService.executeTransfer(copy.getId(), fromLibraryId, toLibraryId, copy.getBiblioId());
        if (!success) {
            throw new BusinessException("预约调拨执行失败(副本状态异常或并发冲突)");
        }

        log.info("跨馆调拨兑现完成: reservationId={}, transferId={}", reservation.getId(), transferId);
        return FulfillmentResult.transferInitiated(reservation.getId(), transferId);
    }

    @Data
    public static class FulfillmentResult {
        //NONE/LOCAL_RESERVE/TRANSFER
        private String type;
        private String message;
        private Long reservationId;
        private Long borrowId;
        private Long transferId;
        private LocalDateTime pickupDeadline;

        public static FulfillmentResult none(String msg) {
            FulfillmentResult r = new FulfillmentResult();
            r.type = "NONE";
            r.message = msg;
            return r;
        }

        public static FulfillmentResult localReserved(Long reservationId, Long borrowId, LocalDateTime pickupDeadline) {
            FulfillmentResult r = new FulfillmentResult();
            r.type = "LOCAL_RESERVE";
            r.message = "就地预留兑现完成";
            r.reservationId = reservationId;
            r.borrowId = borrowId;
            r.pickupDeadline = pickupDeadline;
            return r;
        }

        public static FulfillmentResult transferInitiated(Long reservationId, Long transferId) {
            FulfillmentResult r = new FulfillmentResult();
            r.type = "TRANSFER";
            r.message = "跨馆调拨已发起";
            r.reservationId = reservationId;
            r.transferId = transferId;
            return r;
        }
    }
}

