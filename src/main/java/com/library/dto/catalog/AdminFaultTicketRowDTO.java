package com.library.dto.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端故障工单一行（只读，含馆名等展示字段）")
public class AdminFaultTicketRowDTO {

    @Schema(description = "工单ID")
    private Long id;

    @Schema(description = "馆ID")
    private Long libraryId;

    @Schema(description = "馆名称")
    private String libraryName;

    @Schema(description = "区域ID")
    private Long areaId;

    @Schema(description = "座位ID")
    private Long seatId;

    @Schema(description = "设备ID")
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedTime;
}
