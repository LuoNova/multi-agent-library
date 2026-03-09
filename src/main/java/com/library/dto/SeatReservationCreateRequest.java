package com.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

//创建座位预约请求体
@Data
@Schema(description = "创建座位预约请求")
public class SeatReservationCreateRequest {

    @Schema(description = "用户ID", required = true, example = "1")
    private Long userId;

    @Schema(description = "目标座位ID", required = true, example = "1001")
    private Long seatId;

    @Schema(description = "馆ID(用于校验座位所属馆)", required = true, example = "1")
    private Long libraryId;

    @Schema(description = "预约日期(yyyy-MM-dd)", required = true, example = "2026-03-10")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @Schema(description = "开始时间(HH:mm)", required = true, example = "09:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @Schema(description = "结束时间(HH:mm)", required = true, example = "11:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Schema(description = "关联借阅ID(借书/取书推荐入口时填写)")
    private Long borrowId;

    @Schema(description = "预约来源(WALK_IN/BORROW_PICKUP),不传时由后端根据borrowId推断")
    private String source;
}
