package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

//故障报修表(新增)
@Data
@TableName("tb_repair_ticket")
public class RepairTicket {
    //主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    //设备ID
    private Long equipmentId;

    //报修人ID
    private Long reporterId;

    //故障描述
    private String description;

    //优先级:HIGH/MEDIUM/LOW
    private String priority;

    //状态:PENDING/PROCESSING/COMPLETED/CLOSED
    private String status;

    //处理人ID
    private Long assigneeId;

    //故障图片URL(逗号分隔)
    private String images;

    //报修时间
    private LocalDateTime reportTime;

    //开始处理时间
    private LocalDateTime processTime;

    //完成时间
    private LocalDateTime completeTime;

    //处理反馈
    private String feedback;
}