package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

//座位基础信息DTO
@Data
@Schema(description = "座位基础信息")
public class SeatInfoDTO {

    @Schema(description = "座位ID")
    private Long seatId;

    @Schema(description = "区域ID")
    private Long areaId;

    @Schema(description = "区域名称")
    private String areaName;

    @Schema(description = "楼层")
    private Integer floor;

    @Schema(description = "座位编号")
    private String seatNo;

    @Schema(description = "是否有电源(1-有,0-无)")
    private Integer hasPower;
}

