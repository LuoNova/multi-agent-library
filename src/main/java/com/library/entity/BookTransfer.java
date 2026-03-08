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

    //预计到达时间
    private LocalDateTime estimatedArrivalTime;

    //实际到达时间
    private LocalDateTime actualArrivalTime;

    //调拨原因:USER_REQUEST-用户请求,INVENTORY_BALANCE-库存平衡
    private String transferReason;

    //关联的调拨建议ID
    private Long suggestionId;

    //关联的调拨单ID
    private Long orderId;

    //操作人ID
    private Long operatorId;
}