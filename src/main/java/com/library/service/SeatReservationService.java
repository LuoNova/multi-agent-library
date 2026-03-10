package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.config.BusinessRulesProperties;
import com.library.dto.SeatReservationCreateRequest;
import com.library.dto.SeatReservationItemDTO;
import com.library.dto.SeatReservationResultDTO;
import com.library.entity.Library;
import com.library.entity.Seat;
import com.library.entity.SeatArea;
import com.library.entity.SeatReservation;
import com.library.mapper.SeatMapper;
import com.library.mapper.SeatAreaMapper;
import com.library.mapper.SeatReservationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//座位预约创建服务
@Slf4j
@Service
public class SeatReservationService {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private SeatAreaMapper seatAreaMapper;

    @Autowired
    private SeatReservationMapper seatReservationMapper;

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private BusinessRulesProperties businessRulesProperties;

    //创建座位预约(事务内完成校验、插入、更新座位状态)
    @Transactional(rollbackFor = Exception.class)
    public SeatReservationResultDTO createReservation(SeatReservationCreateRequest request) {
        LocalDate reservationDate = request.getReservationDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        Long userId = request.getUserId();
        Long seatId = request.getSeatId();
        Long libraryId = request.getLibraryId();

        validateTimeRange(reservationDate, startTime, endTime);

        Library library = libraryService.getById(libraryId);
        if (library == null) {
            throw new IllegalArgumentException("馆不存在");
        }
        validateLibraryOpenTime(library, startTime, endTime);

        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new IllegalArgumentException("座位不存在");
        }
        if (!"AVAILABLE".equals(seat.getStatus())) {
            throw new IllegalArgumentException("座位不存在或不可用");
        }

        SeatArea area = seatAreaMapper.selectById(seat.getAreaId());
        if (area == null) {
            throw new IllegalArgumentException("区域不存在");
        }
        if (!area.getLibraryId().equals(libraryId)) {
            throw new IllegalArgumentException("座位不属于该馆");
        }
        if (!"OPEN".equals(area.getStatus())) {
            throw new IllegalArgumentException("区域未开放");
        }

        //优先校验用户维度冲突,便于优先提示用户自身冲突
        List<SeatReservation> userReservations = listActiveByUserAndLibraryAndDate(userId, libraryId, reservationDate);
        if (hasTimeConflict(startTime, endTime, userReservations)) {
            throw new IllegalArgumentException("您在该时间段已预约其他座位,请先取消或调整时间");
        }

        List<SeatReservation> seatReservations = listActiveBySeatAndDate(seatId, reservationDate);
        if (hasTimeConflict(startTime, endTime, seatReservations)) {
            throw new IllegalArgumentException("该时间段座位已被占用,请更换时间或座位");
        }

        String source = inferSource(request.getSource(), request.getBorrowId());

        SeatReservation entity = new SeatReservation();
        entity.setSeatId(seatId);
        entity.setUserId(userId);
        entity.setReservationDate(reservationDate);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setStatus("ACTIVE");
        entity.setCheckInTime(null);
        entity.setLibraryId(area.getLibraryId());
        entity.setBorrowId(request.getBorrowId());
        entity.setSource(source);
        seatReservationMapper.insert(entity);

        seatMapper.updateStatus(seatId, "OCCUPIED");

        log.info("创建座位预约成功: reservationId={}, userId={}, seatId={}, date={}, {}~{}",
                entity.getId(), userId, seatId, reservationDate, startTime, endTime);

        return buildResult(entity, seat, area);
    }

    private void validateTimeRange(LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
        BusinessRulesProperties.SeatRules seatRules = businessRulesProperties.getSeat();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, reservationDate);
        if (days < seatRules.getEarliestBookingDays() || days > seatRules.getLatestBookingDays()) {
            throw new IllegalArgumentException("预约日期不在允许范围内");
        }
        if (endTime == null || startTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("结束时间必须大于开始时间");
        }
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes < seatRules.getMinDurationMinutes() || minutes > seatRules.getMaxDurationMinutes()) {
            throw new IllegalArgumentException("预约时长必须在" +
                    seatRules.getMinDurationMinutes() + "~" + seatRules.getMaxDurationMinutes() + "分钟之间");
        }
        if (minutes % seatRules.getSlotStepMinutes() != 0) {
            throw new IllegalArgumentException("预约时长必须是" + seatRules.getSlotStepMinutes() + "分钟的整数倍");
        }
    }

    //仅按馆开放时间校验(约定区域开放时间与馆一致)
    private void validateLibraryOpenTime(Library library, LocalTime startTime, LocalTime endTime) {
        String openTime = library.getOpenTime();
        if (openTime == null || openTime.trim().isEmpty()) {
            return;
        }
        String[] parts = openTime.trim().split("-");
        if (parts.length != 2) {
            return;
        }
        try {
            LocalTime openStart = LocalTime.parse(parts[0].trim());
            LocalTime openEnd = LocalTime.parse(parts[1].trim());
            if (startTime.isBefore(openStart) || endTime.isAfter(openEnd)) {
                throw new IllegalArgumentException("预约时段不在馆开放时间内(" + openTime + ")");
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException && e.getMessage().startsWith("预约时段不在馆")) {
                throw (IllegalArgumentException) e;
            }
            log.warn("解析馆开放时间失败: openTime={}", openTime);
        }
    }

    private List<SeatReservation> listActiveByUserAndLibraryAndDate(Long userId, Long libraryId, LocalDate date) {
        LambdaQueryWrapper<SeatReservation> w = new LambdaQueryWrapper<>();
        w.eq(SeatReservation::getUserId, userId);
        w.eq(SeatReservation::getLibraryId, libraryId);
        w.eq(SeatReservation::getReservationDate, date);
        w.eq(SeatReservation::getStatus, "ACTIVE");
        return seatReservationMapper.selectList(w);
    }

    private List<SeatReservation> listActiveBySeatAndDate(Long seatId, LocalDate date) {
        LambdaQueryWrapper<SeatReservation> w = new LambdaQueryWrapper<>();
        w.eq(SeatReservation::getSeatId, seatId);
        w.eq(SeatReservation::getReservationDate, date);
        w.eq(SeatReservation::getStatus, "ACTIVE");
        return seatReservationMapper.selectList(w);
    }

    private boolean hasTimeConflict(LocalTime startTime, LocalTime endTime, List<SeatReservation> reservations) {
        for (SeatReservation r : reservations) {
            LocalTime existStart = r.getStartTime();
            LocalTime existEnd = r.getEndTime();
            if (existStart == null || existEnd == null) {
                continue;
            }
            boolean noOverlap = endTime.compareTo(existStart) <= 0 || startTime.compareTo(existEnd) >= 0;
            if (!noOverlap) {
                return true;
            }
        }
        return false;
    }

    private String inferSource(String requestSource, Long borrowId) {
        if (requestSource != null && !requestSource.isBlank()) {
            return requestSource;
        }
        return borrowId != null ? "BORROW_PICKUP" : "WALK_IN";
    }

    private SeatReservationResultDTO buildResult(SeatReservation entity, Seat seat, SeatArea area) {
        SeatReservationResultDTO dto = new SeatReservationResultDTO();
        dto.setReservationId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setLibraryId(entity.getLibraryId());
        dto.setSeatId(seat.getId());
        dto.setAreaId(area.getId());
        dto.setAreaName(area.getName());
        dto.setFloor(area.getFloor());
        dto.setSeatNo(seat.getSeatNo());
        dto.setHasPower(seat.getHasPower());
        dto.setReservationDate(entity.getReservationDate());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setSource(entity.getSource());
        return dto;
    }

    //取消座位预约(仅限本人、仅限ACTIVE状态)
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long reservationId, Long userId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("预约已取消或已结束,无法取消");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能取消本人的预约");
        }
        reservation.setStatus("CANCELED");
        seatReservationMapper.updateById(reservation);
        seatMapper.updateStatus(reservation.getSeatId(), "AVAILABLE");
        log.info("取消座位预约成功: reservationId={}, userId={}, seatId={}", reservationId, userId, reservation.getSeatId());
    }

    //座位签到(仅限本人、仅限ACTIVE状态、支持提前15分钟和迟到no-show窗口内签到)
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long reservationId, Long userId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能签到本人的预约");
        }
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("当前预约状态不可签到");
        }

        LocalDate reservationDate = reservation.getReservationDate();
        LocalTime startTime = reservation.getStartTime();
        if (reservationDate == null || startTime == null) {
            throw new IllegalArgumentException("预约时间信息不完整,无法签到");
        }

        LocalDate today = LocalDate.now();
        if (!today.equals(reservationDate)) {
            throw new IllegalArgumentException("仅可在预约当日签到");
        }

        BusinessRulesProperties.SeatRules seatRules = businessRulesProperties.getSeat();
        int noShowMinutes = seatRules.getNoShowAfterMinutes();
        int earlyMinutes = 15;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = LocalDateTime.of(reservationDate, startTime);
        LocalDateTime earliestCheckInTime = startDateTime.minusMinutes(earlyMinutes);
        LocalDateTime latestCheckInTime = startDateTime.plusMinutes(noShowMinutes);

        if (now.isBefore(earliestCheckInTime)) {
            throw new IllegalArgumentException("尚未到可签到时间(可提前" + earlyMinutes + "分钟签到)");
        }
        if (now.isAfter(latestCheckInTime)) {
            throw new IllegalArgumentException("已超过可签到时间,请重新预约");
        }

        reservation.setCheckInTime(now);
        seatReservationMapper.updateById(reservation);
        log.info("座位签到成功: reservationId={}, userId={}, seatId={}, checkInTime={}",
                reservationId, userId, reservation.getSeatId(), now);
    }

    //暂离(仅限本人、仅限当日、已签到且ACTIVE状态)
    @Transactional(rollbackFor = Exception.class)
    public void tempLeave(Long reservationId, Long userId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能操作本人的预约");
        }
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("当前预约状态不可暂离");
        }
        if (reservation.getCheckInTime() == null) {
            throw new IllegalArgumentException("仅支持对已签到的预约暂离");
        }

        LocalDate reservationDate = reservation.getReservationDate();
        LocalTime startTime = reservation.getStartTime();
        LocalTime endTime = reservation.getEndTime();
        if (reservationDate == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("预约时间信息不完整,无法暂离");
        }

        LocalDate today = LocalDate.now();
        if (!today.equals(reservationDate)) {
            throw new IllegalArgumentException("仅可在预约当日暂离");
        }

        BusinessRulesProperties.SeatRules seatRules = businessRulesProperties.getSeat();
        LocalTime nowTime = LocalTime.now();
        int baseMinutes = seatRules.getTempLeaveDefaultMinutes();

        BusinessRulesProperties.TempLeaveRule lunchRule = seatRules.getTempLeaveLunch();
        if (isInRuleRange(nowTime, lunchRule)) {
            baseMinutes = lunchRule.getMaxMinutes();
        } else {
            BusinessRulesProperties.TempLeaveRule dinnerRule = seatRules.getTempLeaveDinner();
            if (isInRuleRange(nowTime, dinnerRule)) {
                baseMinutes = dinnerRule.getMaxMinutes();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationEnd = LocalDateTime.of(reservationDate, endTime);
        LocalDateTime baseDeadline = now.plusMinutes(baseMinutes);
        if (baseDeadline.isAfter(reservationEnd)) {
            baseDeadline = reservationEnd;
        }
        LocalDateTime forceDeadline = baseDeadline.plusMinutes(30);

        reservation.setTempLeaveStartTime(now);
        reservation.setTempLeaveDeadline(baseDeadline);
        reservation.setTempLeaveForceDeadline(forceDeadline);
        reservation.setTempLeaveStatus("IN_TEMP_LEAVE");
        seatReservationMapper.updateById(reservation);

        log.info("暂离开始: reservationId={}, userId={}, baseDeadline={}, forceDeadline={}",
                reservationId, userId, baseDeadline, forceDeadline);
    }

    //结束暂离(仅限本人、仅限ACTIVE且处于暂离状态,且未超过强制截止时间)
    @Transactional(rollbackFor = Exception.class)
    public void endTempLeave(Long reservationId, Long userId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能操作本人的预约");
        }
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("当前预约状态不可结束暂离");
        }
        String tempStatus = reservation.getTempLeaveStatus();
        if (!"IN_TEMP_LEAVE".equals(tempStatus) && !"TIMEOUT_WARNED".equals(tempStatus)) {
            throw new IllegalArgumentException("当前预约未处于暂离状态");
        }
        LocalDateTime forceDeadline = reservation.getTempLeaveForceDeadline();
        if (forceDeadline != null && LocalDateTime.now().isAfter(forceDeadline)) {
            throw new IllegalArgumentException("暂离已超时,请重新预约");
        }

        reservation.setTempLeaveStartTime(null);
        reservation.setTempLeaveDeadline(null);
        reservation.setTempLeaveForceDeadline(null);
        reservation.setTempLeaveStatus("NONE");
        seatReservationMapper.updateById(reservation);

        log.info("结束暂离成功: reservationId={}, userId={}", reservationId, userId);
    }

    //结束使用(主动离席,仅限本人、仅限ACTIVE状态)
    @Transactional(rollbackFor = Exception.class)
    public void finishUse(Long reservationId, Long userId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能操作本人的预约");
        }
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("当前预约状态不可结束使用");
        }
        reservation.setStatus("COMPLETED");
        reservation.setTempLeaveStartTime(null);
        reservation.setTempLeaveDeadline(null);
        reservation.setTempLeaveForceDeadline(null);
        reservation.setTempLeaveStatus("NONE");
        seatReservationMapper.updateById(reservation);
        Long seatId = reservation.getSeatId();
        if (seatId != null) {
            seatMapper.updateStatus(seatId, "AVAILABLE");
        }
        log.info("结束使用成功: reservationId={}, userId={}, seatId={}", reservationId, userId, seatId);
    }

    //我的座位预约列表(分页,默认仅查今天及以后)
    public Map<String, Object> getMyReservations(Long userId, String status,
                                                 LocalDate fromDate, LocalDate toDate,
                                                 int page, int size) {
        LambdaQueryWrapper<SeatReservation> w = new LambdaQueryWrapper<>();
        w.eq(SeatReservation::getUserId, userId);
        if (status != null && !status.trim().isEmpty()) {
            w.eq(SeatReservation::getStatus, status.trim());
        }
        if (fromDate == null && toDate == null) {
            w.ge(SeatReservation::getReservationDate, LocalDate.now());
        } else {
            if (fromDate != null) {
                w.ge(SeatReservation::getReservationDate, fromDate);
            }
            if (toDate != null) {
                w.le(SeatReservation::getReservationDate, toDate);
            }
        }
        w.orderByAsc(SeatReservation::getReservationDate, SeatReservation::getStartTime);

        Page<SeatReservation> pageObj = new Page<>(page, size);
        Page<SeatReservation> result = seatReservationMapper.selectPage(pageObj, w);
        List<SeatReservation> list = result.getRecords();
        if (list.isEmpty()) {
            Map<String, Object> out = new HashMap<>();
            out.put("total", 0L);
            out.put("page", page);
            out.put("size", size);
            out.put("records", new ArrayList<SeatReservationItemDTO>());
            return out;
        }

        List<Long> seatIds = list.stream().map(SeatReservation::getSeatId).distinct().collect(Collectors.toList());
        List<Seat> seats = seatMapper.selectBatchIds(seatIds);
        Map<Long, Seat> seatMap = seats.stream().collect(Collectors.toMap(Seat::getId, s -> s));
        List<Long> areaIds = seats.stream().map(Seat::getAreaId).distinct().collect(Collectors.toList());
        List<SeatArea> areas = seatAreaMapper.selectBatchIds(areaIds);
        Map<Long, SeatArea> areaMap = areas.stream().collect(Collectors.toMap(SeatArea::getId, a -> a));

        List<SeatReservationItemDTO> records = new ArrayList<>();
        for (SeatReservation r : list) {
            SeatReservationItemDTO dto = new SeatReservationItemDTO();
            dto.setReservationId(r.getId());
            dto.setStatus(r.getStatus());
            dto.setReservationDate(r.getReservationDate());
            dto.setStartTime(r.getStartTime());
            dto.setEndTime(r.getEndTime());
            dto.setCheckInTime(r.getCheckInTime());
            dto.setLibraryId(r.getLibraryId());
            dto.setSource(r.getSource());
            dto.setBorrowId(r.getBorrowId());
            Seat seat = seatMap.get(r.getSeatId());
            if (seat != null) {
                dto.setSeatId(seat.getId());
                dto.setSeatNo(seat.getSeatNo());
                dto.setHasPower(seat.getHasPower());
                dto.setAreaId(seat.getAreaId());
                SeatArea area = areaMap.get(seat.getAreaId());
                if (area != null) {
                    dto.setAreaName(area.getName());
                    dto.setFloor(area.getFloor());
                }
            }
            records.add(dto);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("total", result.getTotal());
        out.put("page", page);
        out.put("size", size);
        out.put("records", records);
        return out;
    }

    //判断当前时间是否在暂离规则区间内
    private boolean isInRuleRange(LocalTime now, BusinessRulesProperties.TempLeaveRule rule) {
        if (rule == null || rule.getStart() == null || rule.getEnd() == null) {
            return false;
        }
        try {
            LocalTime start = LocalTime.parse(rule.getStart().trim());
            LocalTime end = LocalTime.parse(rule.getEnd().trim());
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            log.warn("解析暂离规则时间失败: start={}, end={}", rule.getStart(), rule.getEnd());
            return false;
        }
    }
}
