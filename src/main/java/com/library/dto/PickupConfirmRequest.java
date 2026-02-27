package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取书确认请求DTO
 */
@Data
@Schema(description = "取书确认请求")
public class PickupConfirmRequest {

    @Schema(description = "借阅记录ID", example = "1", required = true)
    private Long borrowId;

    @Schema(description = "用户ID", example = "1", required = true)
    private Long userId;
}
