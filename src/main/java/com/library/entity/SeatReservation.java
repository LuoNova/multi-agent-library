package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

//座位预约表(时间类型修正)
@Data
@TableName("tb_seat_reservation")
public class SeatReservation {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //座位ID
    private Long seatId;

    //预约人ID
    private Long userId;

    //预约日期
    private LocalDate reservationDate;

    //开始时间
    private LocalTime startTime;

    //结束时间
    private LocalTime endTime;

    //状态:ACTIVE/COMPLETED/CANCELED/NO_SHOW
    private String status;

    //实际签到时间
    private LocalDateTime checkInTime;

    //创建时间
    private LocalDateTime createTime;
}