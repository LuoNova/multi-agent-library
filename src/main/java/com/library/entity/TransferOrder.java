package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

//调拨单表（管理批量调拨）
@Data
@TableName("tb_transfer_order")
public class TransferOrder {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //关联的调拨建议ID
    private Long suggestionId;

    //源馆ID
    private Long fromLibraryId;

    //目标馆ID
    private Long toLibraryId;

    //书目ID
    private Long biblioId;

    //计划调拨数量
    private Integer plannedQuantity;

    //实际调拨数量
    private Integer actualQuantity;

    //状态:PENDING-待执行,IN_PROGRESS-执行中,COMPLETED-已完成,CANCELED-已取消
    private String status;

    //创建时间
    private LocalDateTime createTime;

    //完成时间
    private LocalDateTime completeTime;

    //操作人ID
    private Long operatorId;

    //备注
    private String remark;
}
