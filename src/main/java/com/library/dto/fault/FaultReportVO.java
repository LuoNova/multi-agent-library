package com.library.dto.fault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "故障工单")
public class FaultReportVO {

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
