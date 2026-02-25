package com.library.service;

import com.library.constant.LibraryConstants;
import com.library.entity.BookReservation;
import com.library.mapper.BookReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    //检查用户是否已有该书的有效预约（防止重复预约）
    public boolean hasActiveReservation(Long userId, Long biblioId) {
        //查询该用户该书目的PENDING或FULFILLED但未过期的预约
        //简化实现：假设Mapper有对应方法，这里直接返回false或实现逻辑
        //实际应该查询数据库：
        //SELECT COUNT(*) FROM tb_book_reservation
        //WHERE user_id = #{userId} AND biblio_id = #{biblioId}
        //AND status IN ('PENDING', 'FULFILLED')
        //AND expire_time > NOW()
        return false; //暂时简化，实际应查询数据库
    }
}