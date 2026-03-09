package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.config.BusinessRulesProperties;
import com.library.dto.SeatReservationCreateRequest;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
}
