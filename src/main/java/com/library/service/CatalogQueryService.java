package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.constant.LibraryConstants;
import com.library.dto.catalog.*;
import com.library.entity.*;
import com.library.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 目录与读者侧查询（只读），供前端「查询回显」使用；不替代既有写接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final LibraryMapper libraryMapper;
    private final BookBiblioMapper bookBiblioMapper;
    private final BookBorrowMapper bookBorrowMapper;
    private final BookCopyMapper bookCopyMapper;
    private final SeatAreaMapper seatAreaMapper;
    private final SeatMapper seatMapper;
    private final EquipmentMapper equipmentMapper;
    private final BookTransferMapper bookTransferMapper;
    private final FaultReportMapper faultReportMapper;

    public List<LibraryItemDTO> listLibraries() {
        List<Library> rows = libraryMapper.selectList(
                Wrappers.<Library>lambdaQuery().orderByAsc(Library::getId));
        return rows.stream()
                .map(l -> LibraryItemDTO.builder().id(l.getId()).name(l.getName()).build())
                .collect(Collectors.toList());
    }

    public List<BiblioItemDTO> searchBiblios(String keyword, int limit) {
        int cap = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<BookBiblio> q = Wrappers.<BookBiblio>lambdaQuery().orderByAsc(BookBiblio::getId).last("LIMIT " + cap);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            q = Wrappers.<BookBiblio>lambdaQuery()
                    .and(w -> w.like(BookBiblio::getTitle, kw)
                            .or().like(BookBiblio::getIsbn, kw)
                            .or().like(BookBiblio::getAuthor, kw))
                    .orderByAsc(BookBiblio::getId)
                    .last("LIMIT " + cap);
        }
        return bookBiblioMapper.selectList(q).stream()
                .map(b -> BiblioItemDTO.builder()
                        .id(b.getId())
                        .title(b.getTitle())
                        .author(b.getAuthor())
                        .isbn(b.getIsbn())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ActiveLoanRowDTO> listActiveLoans(Long userId) {
        List<BookBorrow> borrows = bookBorrowMapper.selectList(
                Wrappers.<BookBorrow>lambdaQuery()
                        .eq(BookBorrow::getUserId, userId)
                        .eq(BookBorrow::getStatus, LibraryConstants.BORROW_STATUS_BORROWING)
                        .orderByDesc(BookBorrow::getId));
        List<ActiveLoanRowDTO> out = new ArrayList<>();
        for (BookBorrow bb : borrows) {
            BookCopy copy = bookCopyMapper.selectById(bb.getCopyId());
            BookBiblio biblio = copy != null ? bookBiblioMapper.selectById(copy.getBiblioId()) : null;
            String title = biblio != null ? biblio.getTitle() : "—";
            Long libId = copy != null ? copy.getLibraryId() : null;
            String libName = resolveLibraryName(libId);
            out.add(ActiveLoanRowDTO.builder()
                    .borrowId(bb.getId())
                    .userId(bb.getUserId())
                    .copyId(bb.getCopyId())
                    .biblioId(copy != null ? copy.getBiblioId() : null)
                    .bookTitle(title)
                    .status(bb.getStatus())
                    .dueTime(bb.getDueTime())
                    .suggestedReturnLibraryId(libId)
                    .suggestedReturnLibraryName(libName)
                    .build());
        }
        return out;
    }

    public List<PickupPendingRowDTO> listPickupPending(Long userId) {
        List<BookBorrow> borrows = bookBorrowMapper.selectList(
                Wrappers.<BookBorrow>lambdaQuery()
                        .eq(BookBorrow::getUserId, userId)
                        .in(BookBorrow::getStatus,
                                Arrays.asList(
                                        LibraryConstants.BORROW_STATUS_RESERVED,
                                        LibraryConstants.BORROW_STATUS_TRANSFERRING))
                        .orderByDesc(BookBorrow::getId));
        List<PickupPendingRowDTO> out = new ArrayList<>();
        for (BookBorrow bb : borrows) {
            BookCopy copy = bookCopyMapper.selectById(bb.getCopyId());
            BookBiblio biblio = copy != null ? bookBiblioMapper.selectById(copy.getBiblioId()) : null;
            String title = biblio != null ? biblio.getTitle() : "—";
            Long pickupLibId = bb.getPickupLibraryId();
            String pickupLibName = resolveLibraryName(pickupLibId);
            out.add(PickupPendingRowDTO.builder()
                    .borrowId(bb.getId())
                    .userId(bb.getUserId())
                    .copyId(bb.getCopyId())
                    .bookTitle(title)
                    .status(bb.getStatus())
                    .pickupLibraryId(pickupLibId)
                    .pickupLibraryName(pickupLibName)
                    .pickupDeadline(bb.getPickupDeadline())
                    .build());
        }
        return out;
    }

    public List<SeatAreaItemDTO> listSeatAreas(Long libraryId) {
        List<SeatArea> rows = seatAreaMapper.selectList(
                Wrappers.<SeatArea>lambdaQuery()
                        .eq(SeatArea::getLibraryId, libraryId)
                        .orderByAsc(SeatArea::getId));
        return rows.stream()
                .map(a -> SeatAreaItemDTO.builder()
                        .id(a.getId())
                        .libraryId(a.getLibraryId())
                        .name(a.getName())
                        .floor(a.getFloor())
                        .build())
                .collect(Collectors.toList());
    }

    public List<SeatItemDTO> listSeats(Long areaId, int limit) {
        int cap = Math.min(Math.max(limit, 1), 500);
        List<Seat> rows = seatMapper.selectList(
                Wrappers.<Seat>lambdaQuery()
                        .eq(Seat::getAreaId, areaId)
                        .orderByAsc(Seat::getId)
                        .last("LIMIT " + cap));
        return rows.stream()
                .map(s -> SeatItemDTO.builder()
                        .id(s.getId())
                        .areaId(s.getAreaId())
                        .seatNo(s.getSeatNo())
                        .hasPower(s.getHasPower())
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    public Map<String, Object> pageEquipment(Long libraryId, int page, int size) {
        Page<Equipment> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<Equipment> w = Wrappers.<Equipment>lambdaQuery().orderByDesc(Equipment::getId);
        if (libraryId != null) {
            w.eq(Equipment::getLibraryId, libraryId);
        }
        Page<Equipment> result = equipmentMapper.selectPage(p, w);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("size", result.getSize());
        map.put("records", result.getRecords());
        return map;
    }

    /**
     * 读者借阅历史：按用户分页，可选状态筛选；含书名、取书馆名称等展示字段。
     */
    public Map<String, Object> pageBorrowHistory(Long userId, String status, int page, int size) {
        Page<BookBorrow> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50));
        LambdaQueryWrapper<BookBorrow> w = Wrappers.<BookBorrow>lambdaQuery()
                .eq(BookBorrow::getUserId, userId)
                .orderByDesc(BookBorrow::getId);
        if (StringUtils.hasText(status)) {
            w.eq(BookBorrow::getStatus, status.trim());
        }
        Page<BookBorrow> result = bookBorrowMapper.selectPage(p, w);
        List<BorrowHistoryRowDTO> rows = new ArrayList<>();
        for (BookBorrow bb : result.getRecords()) {
            BookCopy copy = bb.getCopyId() != null ? bookCopyMapper.selectById(bb.getCopyId()) : null;
            BookBiblio biblio = copy != null ? bookBiblioMapper.selectById(copy.getBiblioId()) : null;
            String title = biblio != null ? biblio.getTitle() : "—";
            rows.add(BorrowHistoryRowDTO.builder()
                    .borrowId(bb.getId())
                    .userId(bb.getUserId())
                    .copyId(bb.getCopyId())
                    .biblioId(copy != null ? copy.getBiblioId() : null)
                    .bookTitle(title)
                    .status(bb.getStatus())
                    .borrowTime(bb.getBorrowTime())
                    .dueTime(bb.getDueTime())
                    .returnTime(bb.getReturnTime())
                    .actualPickupTime(bb.getActualPickupTime())
                    .pickupLibraryId(bb.getPickupLibraryId())
                    .pickupLibraryName(resolveLibraryName(bb.getPickupLibraryId()))
                    .build());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("size", result.getSize());
        map.put("records", rows);
        return map;
    }

    /**
     * 管理端调拨分页：与 {@code GET /api/transfer/list} 数据范围一致，补充源/目标馆名、书名等便于表格回显。
     */
    public Map<String, Object> pageAdminTransfers(String status, int page, int size) {
        Page<BookTransfer> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<BookTransfer> w = Wrappers.<BookTransfer>lambdaQuery().orderByDesc(BookTransfer::getRequestTime);
        if (StringUtils.hasText(status)) {
            w.eq(BookTransfer::getStatus, status.trim());
        }
        Page<BookTransfer> result = bookTransferMapper.selectPage(p, w);
        List<AdminTransferRowDTO> rows = new ArrayList<>();
        for (BookTransfer t : result.getRecords()) {
            BookCopy copy = t.getCopyId() != null ? bookCopyMapper.selectById(t.getCopyId()) : null;
            BookBiblio biblio = copy != null ? bookBiblioMapper.selectById(copy.getBiblioId()) : null;
            rows.add(AdminTransferRowDTO.builder()
                    .transferId(t.getId())
                    .copyId(t.getCopyId())
                    .biblioId(copy != null ? copy.getBiblioId() : null)
                    .bookTitle(biblio != null ? biblio.getTitle() : "—")
                    .fromLibraryId(t.getFromLibraryId())
                    .fromLibraryName(resolveLibraryName(t.getFromLibraryId()))
                    .toLibraryId(t.getToLibraryId())
                    .toLibraryName(resolveLibraryName(t.getToLibraryId()))
                    .status(t.getStatus())
                    .orderId(t.getOrderId())
                    .suggestionId(t.getSuggestionId())
                    .transferReason(t.getTransferReason())
                    .receiverUserId(t.getReceiverUserId())
                    .requestTime(t.getRequestTime())
                    .completeTime(t.getCompleteTime())
                    .estimatedArrivalTime(t.getEstimatedArrivalTime())
                    .actualArrivalTime(t.getActualArrivalTime())
                    .build());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("size", result.getSize());
        map.put("records", rows);
        return map;
    }

    /**
     * 管理端故障工单分页：筛选条件与 {@code GET /api/fault/list} 一致，补充馆名便于下拉+表格展示。
     */
    public Map<String, Object> pageAdminFaultTickets(Long libraryId, Long areaId, Long seatId, Long equipmentId,
                                                     String status, String faultType, String severity,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     int page, int size) {
        Page<FaultReport> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<FaultReport> w = Wrappers.<FaultReport>lambdaQuery();
        if (libraryId != null) {
            w.eq(FaultReport::getLibraryId, libraryId);
        }
        if (areaId != null) {
            w.eq(FaultReport::getAreaId, areaId);
        }
        if (seatId != null) {
            w.eq(FaultReport::getSeatId, seatId);
        }
        if (equipmentId != null) {
            w.eq(FaultReport::getEquipmentId, equipmentId);
        }
        if (StringUtils.hasText(status)) {
            w.eq(FaultReport::getStatus, status.trim());
        }
        if (StringUtils.hasText(faultType)) {
            w.eq(FaultReport::getFaultType, faultType.trim());
        }
        if (StringUtils.hasText(severity)) {
            w.eq(FaultReport::getSeverity, severity.trim());
        }
        if (startTime != null) {
            w.ge(FaultReport::getCreatedTime, startTime);
        }
        if (endTime != null) {
            w.le(FaultReport::getCreatedTime, endTime);
        }
        w.orderByDesc(FaultReport::getCreatedTime);
        Page<FaultReport> result = faultReportMapper.selectPage(p, w);
        List<AdminFaultTicketRowDTO> rows = new ArrayList<>();
        for (FaultReport e : result.getRecords()) {
            rows.add(AdminFaultTicketRowDTO.builder()
                    .id(e.getId())
                    .libraryId(e.getLibraryId())
                    .libraryName(resolveLibraryName(e.getLibraryId()))
                    .areaId(e.getAreaId())
                    .seatId(e.getSeatId())
                    .equipmentId(e.getEquipmentId())
                    .faultType(e.getFaultType())
                    .severity(e.getSeverity())
                    .status(e.getStatus())
                    .title(e.getTitle())
                    .description(e.getDescription())
                    .adminRemark(e.getAdminRemark())
                    .reportSource(e.getReportSource())
                    .reportUserId(e.getReportUserId())
                    .assignee(e.getAssignee())
                    .createdTime(e.getCreatedTime())
                    .updatedTime(e.getUpdatedTime())
                    .resolvedTime(e.getResolvedTime())
                    .build());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("size", result.getSize());
        map.put("records", rows);
        return map;
    }

    private String resolveLibraryName(Long libraryId) {
        if (libraryId == null) {
            return "—";
        }
        Library lib = libraryMapper.selectById(libraryId);
        return lib != null ? lib.getName() : ("馆#" + libraryId);
    }
}
