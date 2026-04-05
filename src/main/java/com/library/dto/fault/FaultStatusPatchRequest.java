package com.library.dto.fault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新工单状态（简单模式：任意合法状态均可；终态时后端写入 resolvedTime）")
public class FaultStatusPatchRequest {

    @Schema(description = "工单状态", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "负责人")
    private String assignee;

    @Schema(description = "运维备注（覆盖写入 admin_remark）")
    private String adminRemark;
}
