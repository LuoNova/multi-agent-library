package com.library.dto.fault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "提交故障工单请求")
public class FaultReportCreateRequest {

    @Schema(description = "所属馆ID")
    private Long libraryId;
    @Schema(description = "区域ID")
    private Long areaId;
    @Schema(description = "座位ID")
    private Long seatId;
    @Schema(description = "设备ID(tb_equipment)")
    private Long equipmentId;

    @Schema(description = "故障类型", example = "seat_broken", requiredMode = Schema.RequiredMode.REQUIRED)
    private String faultType;
    @Schema(description = "严重程度", example = "medium", requiredMode = Schema.RequiredMode.REQUIRED)
    private String severity;
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "用户描述")
    private String description;

    @Schema(description = "报修来源 USER/MONITOR/ADMIN/SYSTEM", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reportSource;
    @Schema(description = "报修用户ID，可选")
    private Long reportUserId;
}
