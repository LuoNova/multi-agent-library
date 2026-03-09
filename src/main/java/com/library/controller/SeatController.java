package com.library.controller;

import com.library.common.Result;
import com.library.dto.SeatAvailabilityResultDTO;
import com.library.dto.SeatReservationCreateRequest;
import com.library.dto.SeatReservationResultDTO;
import com.library.service.SeatAvailabilityService;
import com.library.service.SeatReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

//座位相关接口
@Slf4j
@RestController
@RequestMapping("/api/seat")
@Tag(name = "座位管理", description = "座位可用性查询与动态分配、创建预约")
public class SeatController {

    @Autowired
    private SeatAvailabilityService seatAvailabilityService;

    @Autowired
    private SeatReservationService seatReservationService;

    @GetMapping("/available")
    @Operation(summary = "查询可用座位/自动分配", description = "根据馆、日期、时间段等条件查询可用座位列表或自动分配一个推荐座位")
    public Result<SeatAvailabilityResultDTO> queryAvailableSeats(
            @Parameter(description = "馆ID", required = true)
            @RequestParam Long libraryId,
            @Parameter(description = "预约日期(yyyy-MM-dd)", required = true, schema = @Schema(type = "string", format = "date", example = "2026-03-10"))
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reservationDate,
            @Parameter(description = "开始时间(HH:mm)", required = true, schema = @Schema(type = "string", format = "time", example = "09:00"))
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @Parameter(description = "结束时间(HH:mm)", required = true, schema = @Schema(type = "string", format = "time", example = "11:00"))
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @Parameter(description = "区域ID(可选)")
            @RequestParam(required = false) Long areaId,
            @Parameter(description = "是否只要有电源座位(可选)")
            @RequestParam(required = false) Boolean hasPower,
            @Parameter(description = "是否启用自动分配,默认true")
            @RequestParam(required = false, defaultValue = "true") Boolean autoAssign) {
        try {
            log.info("查询可用座位: libraryId={}, date={}, start={}, end={}, areaId={}, hasPower={}, autoAssign={}",
                    libraryId, reservationDate, startTime, endTime, areaId, hasPower, autoAssign);

            SeatAvailabilityResultDTO dto = seatAvailabilityService.queryAvailableSeats(
                    libraryId, reservationDate, startTime, endTime, areaId, hasPower, autoAssign);

            //统一使用code=200返回,通过total/availableTotal判断有无可用座位
            if ((autoAssign == null || autoAssign) && (dto.getAvailableTotal() == null || dto.getAvailableTotal() == 0)) {
                return Result.success("当前条件下无可用座位", dto);
            }
            if (Boolean.FALSE.equals(autoAssign) && (dto.getTotal() == null || dto.getTotal() == 0)) {
                return Result.success("当前条件下无可用座位", dto);
            }
            return Result.success("查询成功", dto);
        } catch (IllegalArgumentException e) {
            log.warn("查询可用座位参数非法: {}", e.getMessage());
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("查询可用座位异常", e);
            return Result.fail("查询可用座位失败:" + e.getMessage());
        }
    }

    @PostMapping("/reservation")
    @Operation(summary = "创建座位预约", description = "选定座位与时间段后创建预约并占用座位")
    public Result<SeatReservationResultDTO> createReservation(
            @Parameter(description = "创建预约请求体", required = true)
            @RequestBody SeatReservationCreateRequest request) {
        try {
            log.info("创建座位预约: userId={}, seatId={}, libraryId={}, date={}, {}~{}",
                    request.getUserId(), request.getSeatId(), request.getLibraryId(),
                    request.getReservationDate(), request.getStartTime(), request.getEndTime());
            SeatReservationResultDTO dto = seatReservationService.createReservation(request);
            return Result.success("座位预约创建成功", dto);
        } catch (IllegalArgumentException e) {
            log.warn("创建座位预约参数或业务校验失败: {}", e.getMessage());
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建座位预约异常", e);
            return Result.fail("创建座位预约失败:" + e.getMessage());
        }
    }

    @PostMapping("/reservation/cancel")
    @Operation(summary = "取消座位预约", description = "用户主动取消预约,座位置回可用")
    public Result<Void> cancelReservation(
            @Parameter(description = "预约记录ID", required = true) @RequestParam Long reservationId,
            @Parameter(description = "用户ID(校验本人)", required = true) @RequestParam Long userId) {
        try {
            log.info("取消座位预约: reservationId={}, userId={}", reservationId, userId);
            seatReservationService.cancelReservation(reservationId, userId);
            return Result.success("取消成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("取消座位预约失败: {}", e.getMessage());
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("取消座位预约异常", e);
            return Result.fail("取消座位预约失败:" + e.getMessage());
        }
    }
}

