package com.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

//创建座位预约成功返回数据
@Data
@Schema(description = "座位预约结果")
public class SeatReservationResultDTO {

    @Schema(description = "预约记录ID")
    private Long reservationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "馆ID")
    private Long libraryId;

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

    @Schema(description = "预约日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Schema(description = "预约来源")
    private String source;
}
