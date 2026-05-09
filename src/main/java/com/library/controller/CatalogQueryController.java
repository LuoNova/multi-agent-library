package com.library.controller;

import com.library.common.Result;
import com.library.dto.catalog.*;
import com.library.service.CatalogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 目录与只读查询接口：供前端「查询回显」；不修改既有借还调拨等写接口。
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "目录查询", description = "图书馆、书目、在借、待取书、调拨/工单管理只读分页、座位区域/座位、设备分页等")
public class CatalogQueryController {

    private final CatalogQueryService catalogQueryService;

    @GetMapping("/libraries")
    @Operation(summary = "图书馆列表", description = "全量馆列表，用于下拉选择")
    public Result<List<LibraryItemDTO>> listLibraries() {
        return Result.success("查询成功", catalogQueryService.listLibraries());
    }

    @GetMapping("/biblios")
    @Operation(summary = "书目检索", description = "按书名/ISBN/作者模糊匹配；keyword 为空时返回前若干条")
    public Result<List<BiblioItemDTO>> searchBiblios(
            @Parameter(description = "关键词，可空") @RequestParam(required = false) String keyword,
            @Parameter(description = "条数上限，默认30，最大100") @RequestParam(defaultValue = "30") int limit) {
        return Result.success("查询成功", catalogQueryService.searchBiblios(keyword, limit));
    }

    @GetMapping("/borrows/active")
    @Operation(summary = "当前在借列表", description = "status=BORROWING，用于还书选书")
    public Result<List<ActiveLoanRowDTO>> listActiveLoans(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        return Result.success("查询成功", catalogQueryService.listActiveLoans(userId));
    }

    @GetMapping("/borrows/pickup-pending")
    @Operation(summary = "待取书列表", description = "状态为 RESERVED 或 TRANSFERRING，用于取书确认选单")
    public Result<List<PickupPendingRowDTO>> listPickupPending(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        return Result.success("查询成功", catalogQueryService.listPickupPending(userId));
    }

    @GetMapping("/borrows/history")
    @Operation(summary = "借阅历史分页", description = "当前用户全部借阅记录（多状态），可选 status 精确筛选；分页结构同 equipment")
    public Result<Map<String, Object>> pageBorrowHistory(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "状态精确匹配，可空。如 BORROWING、RETURNED、RESERVED 等") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大50") @RequestParam(defaultValue = "10") int size) {
        return Result.success("查询成功", catalogQueryService.pageBorrowHistory(userId, status, page, size));
    }

    @GetMapping("/seat-areas")
    @Operation(summary = "座位区域列表", description = "按馆查询区域")
    public Result<List<SeatAreaItemDTO>> listSeatAreas(
            @Parameter(description = "馆ID", required = true) @RequestParam Long libraryId) {
        return Result.success("查询成功", catalogQueryService.listSeatAreas(libraryId));
    }

    @GetMapping("/seats")
    @Operation(summary = "座位列表", description = "按区域查询座位，limit 默认200 最大500")
    public Result<List<SeatItemDTO>> listSeats(
            @Parameter(description = "区域ID", required = true) @RequestParam Long areaId,
            @Parameter(description = "条数上限") @RequestParam(defaultValue = "200") int limit) {
        return Result.success("查询成功", catalogQueryService.listSeats(areaId, limit));
    }

    @GetMapping("/equipment")
    @Operation(summary = "设备分页", description = "可选按馆筛选；结构与工单/调拨分页一致")
    public Result<Map<String, Object>> pageEquipment(
            @Parameter(description = "馆ID，可空") @RequestParam(required = false) Long libraryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success("查询成功", catalogQueryService.pageEquipment(libraryId, page, size));
    }

    @GetMapping("/transfers")
    @Operation(summary = "调拨记录分页（管理只读）", description = "与 GET /api/transfer/list 同条件，records 为 AdminTransferRowDTO（含馆名、书名）；便于管理端表格回显")
    public Result<Map<String, Object>> pageAdminTransfers(
            @Parameter(description = "状态筛选，可空") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大100") @RequestParam(defaultValue = "10") int size) {
        return Result.success("查询成功", catalogQueryService.pageAdminTransfers(status, page, size));
    }

    @GetMapping("/fault-tickets")
    @Operation(summary = "故障工单分页（管理只读）", description = "筛选条件与 GET /api/fault/list 一致，records 为 AdminFaultTicketRowDTO（含 libraryName）；便于管理端表格与馆下拉联动")
    public Result<Map<String, Object>> pageAdminFaultTickets(
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
            @Parameter(description = "每页条数，最大100") @RequestParam(defaultValue = "10") int size) {
        return Result.success("查询成功",
                catalogQueryService.pageAdminFaultTickets(
                        libraryId, areaId, seatId, equipmentId, status, faultType, severity,
                        startTime, endTime, page, size));
    }
}
