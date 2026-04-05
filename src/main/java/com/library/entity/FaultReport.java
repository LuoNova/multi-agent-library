package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_fault_report")
public class FaultReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long libraryId;
    private Long areaId;
    private Long seatId;
    private Long equipmentId;

    private String faultType;
    private String severity;
    private String status;

    private String title;
    private String description;
    private String adminRemark;

    private String reportSource;
    private Long reportUserId;
    private String assignee;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private LocalDateTime resolvedTime;
}
