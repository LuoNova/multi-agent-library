package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

//图书调拨记录表
@Data
@TableName("tb_book_transfer")
public class BookTransfer {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //关联的协商任务ID(新增字段)
    private String requestId;

    //调拨副本ID
    private Long copyId;

    //源馆ID
    private Long fromLibraryId;

    //目标馆ID
    private Long toLibraryId;

    //状态:PENDING/IN_TRANSIT/COMPLETED/CANCELED
    private String status;

    //申请时间
    private LocalDateTime requestTime;

    //完成时间
    private LocalDateTime completeTime;

    //操作人ID
    private Long operatorId;
}