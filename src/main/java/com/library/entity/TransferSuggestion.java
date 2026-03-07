package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

//调拨建议表
@Data
@TableName("tb_transfer_suggestion")
public class TransferSuggestion {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //源馆ID
    private Long fromLibraryId;

    //目标馆ID
    private Long toLibraryId;

    //书目ID
    private Long biblioId;

    //建议调拨数量
    private Integer suggestedQuantity;

    //优先级评分
    private BigDecimal priorityScore;

    //调拨原因
    private String reason;

    //状态:PENDING-待审批,APPROVED-已批准,REJECTED-已拒绝,EXECUTED-已执行
    private String status;

    //审批人ID
    private Long approverId;

    //审批时间
    private LocalDateTime approveTime;

    //关联的调拨单ID
    private Long transferId;

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;
}
