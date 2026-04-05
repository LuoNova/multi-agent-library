package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新设备状态（会双向联动故障工单）")
public class EquipmentStatusUpdateRequest {

    @Schema(description = "NORMAL/FAULT/MAINTAIN/DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
