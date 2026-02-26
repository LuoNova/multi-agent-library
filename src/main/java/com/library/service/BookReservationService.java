package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.constant.LibraryConstants;
import com.library.entity.BookReservation;
import com.library.mapper.BookReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

//图书预约服务（处理无库存时的预约排队）
@Service
public class BookReservationService {

    @Autowired
    private BookReservationMapper reservationMapper;

    //创建预约记录（当全馆无库存时调用）
    public Long createReservation(Long userId, Long biblioId, Long pickupLibraryId) {
        BookReservation reservation = new BookReservation();
        reservation.setBiblioId(biblioId);
        reservation.setUserId(userId);
        reservation.setPickupLibraryId(pickupLibraryId);
        reservation.setCopyId(null); //尚未分配副本
        reservation.setReserveTime(LocalDateTime.now());
        reservation.setExpireTime(LocalDateTime.now().plusDays(
                LibraryConstants.RESERVATION_EXPIRE_DAYS)); //设置预约有效期
        reservation.setStatus(LibraryConstants.RESERVATION_STATUS_PENDING);
        reservation.setNotificationSent(0);

        reservationMapper.insert(reservation);
        return reservation.getId();
    }

    //检查用户是否已存在该书的有效预约（PENDING待到货 或 FULFILLED已到馆待取）
    //返回true表示已存在，禁止重复预约
    public boolean hasActiveReservation(Long userId, Long biblioId) {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getUserId, userId)
                .eq(BookReservation::getBiblioId, biblioId)
                .in(BookReservation::getStatus,
                        Arrays.asList(LibraryConstants.RESERVATION_STATUS_PENDING,
                                LibraryConstants.RESERVATION_STATUS_FULFILLED));
        return reservationMapper.selectCount(wrapper) > 0;
    }
}