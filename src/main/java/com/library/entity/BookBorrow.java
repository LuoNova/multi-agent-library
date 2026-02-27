package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//借阅记录表(新增)
@Data
@TableName("tb_book_borrow")
public class BookBorrow {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //副本ID
    private Long copyId;

    //借阅人ID
    private Long userId;

    //借出时间
    private LocalDateTime borrowTime;

    //应还时间
    private LocalDateTime dueTime;

    //实际归还时间
    private LocalDateTime returnTime;

    //记录状态:RESERVED-已预留,BORROWING-借阅中,RETURNED-已归还,CANCELLED-已取消
    private String status;


    //预留开始时间(调拨完成时间)
    private LocalDateTime reservedTime;

    //取书截止时间(预留开始时间+24小时)
    private LocalDateTime pickupDeadline;

    //实际取书时间
    private LocalDateTime actualPickupTime;

    //取书馆ID
    private Long pickupLibraryId;

    //创建时间
    private LocalDateTime createTime;
}