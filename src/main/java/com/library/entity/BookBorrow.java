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

    //记录状态:BORROWING/RETURNED
    private String status;

    //创建时间
    private LocalDateTime createTime;
}