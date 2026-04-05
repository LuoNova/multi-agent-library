package com.library.controller;

import com.library.common.Result;
import com.library.dto.fault.*;
import com.library.service.FaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fault")
@RequiredArgsConstructor
@Tag(name = "故障报修", description = "故障工单与资源健康查询（不经 JADE）")
public class FaultController {

    private final FaultService faultService;

    @PostMapping("/report")
    @Operation(summary = "提交故障工单", description = "libraryId/areaId/seatId/equipmentId 至少填一个；创建后 status=REPORTED")
    public Result<FaultReportVO> report(@RequestBody FaultReportCreateRequest request) {
        FaultReportVO vo = faultService.createReport(request);
        return Result.success(vo);
    }

    @GetMapping("/list")
    @Operation(summary = "工单分页列表", description = "分页结构与 GET /api/seat/reservation/my 一致：total/page/size/records")
    public Result<Map<String, Object>> list(
            @Parameter(description = "馆ID") @RequestParam(required = false) Long libraryId,
            @Parameter(description = "区域ID") @RequestParam(required = false) Long areaId,
            @Parameter(description = "座位ID") @RequestParam(required = false) Long seatId,
            @Parameter(description = "设备ID") @RequestParam(required = false) Long equipmentId,
            @Parameter(description = "工单状态") @RequestParam(required = false) String status,
            @Parameter(description = "故障类型") @RequestParam(required = false) String faultType,
            @Parameter(description = "严重程度") @RequestParam(required = false) String severity,
            @Parameter(description = "创建时间起") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "创建时间止") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> data = faultService.list(
                libraryId, areaId, seatId, equipmentId, status, faultType, severity,
                startTime, endTime, page, size);
        return Result.success("查询成功", data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "工单详情")
    public Result<FaultReportVO> detail(@PathVariable Long id) {
        return Result.success(faultService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新工单状态", description = "任意合法状态均可；进入 RESTORED/CLOSED 时后端自动写入 resolvedTime=now()，离开终态则清空 resolvedTime")
    public Result<FaultReportVO> patchStatus(
            @PathVariable Long id,
            @RequestBody FaultStatusPatchRequest request) {
        return Result.success(faultService.patchStatus(id, request));
    }

    @PostMapping("/health/query")
    @Operation(summary = "批量资源健康查询", description = "resourceType: LIBRARY / SEAT_AREA / SEAT / EQUIPMENT")
    public Result<FaultHealthQueryResponse> healthQuery(@RequestBody FaultHealthQueryRequest request) {
        return Result.success(faultService.healthQuery(request));
    }
}
