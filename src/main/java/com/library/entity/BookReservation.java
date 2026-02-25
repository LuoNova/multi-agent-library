package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//图书预约表
@Data
@TableName("tb_book_reservation")
public class BookReservation {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //书目ID
    private Long biblioId;

    //预约人ID
    private Long userId;

    //期望取书馆ID
    private Long pickupLibraryId;

    //实际分配的副本ID(初始为空)
    private Long copyId;

    //预约时间
    private LocalDateTime reserveTime;

    //预约过期时间(如3天后)
    private LocalDateTime expireTime;

    //实际分配副本时间
    private LocalDateTime fulfillTime;

    //状态:PENDING/FULFILLED/CANCELED/EXPIRED
    private String status;

    //是否已发送取书通知(0-否,1-是)
    private Integer notificationSent;

    //创建时间
    private LocalDateTime createTime;
}