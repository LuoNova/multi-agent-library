package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.config.BusinessRulesProperties;
import com.library.dto.SeatAvailabilityResultDTO;
import com.library.dto.SeatInfoDTO;
import com.library.entity.Seat;
import com.library.entity.SeatArea;
import com.library.entity.SeatReservation;
import com.library.mapper.SeatMapper;
import com.library.mapper.SeatAreaMapper;
import com.library.mapper.SeatReservationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

//座位可用性查询与动态分配服务
@Slf4j
@Service
public class SeatAvailabilityService {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private SeatAreaMapper seatAreaMapper;

    @Autowired
    private SeatReservationMapper seatReservationMapper;

    @Autowired
    private BusinessRulesProperties businessRulesProperties;

    //查询可用座位并按需进行动态分配
    public SeatAvailabilityResultDTO queryAvailableSeats(Long libraryId,
                                                         LocalDate reservationDate,
                                                         LocalTime startTime,
                                                         LocalTime endTime,
                                                         Long areaId,
                                                         Boolean hasPower,
                                                         Boolean autoAssign) {
        validateTimeRange(reservationDate, startTime, endTime);

        //查当前馆下OPEN的区域
        List<SeatArea> areas = seatAreaMapper.selectByLibraryId(libraryId)
                .stream()
                .filter(area -> "OPEN".equals(area.getStatus()))
                .collect(Collectors.toList());
        if (areas.isEmpty()) {
            log.info("馆{}下无OPEN状态区域", libraryId);
            SeatAvailabilityResultDTO result = new SeatAvailabilityResultDTO();
            result.setTotal(0L);
            return result;
        }

        Map<Long, SeatArea> areaMap = areas.stream()
                .collect(Collectors.toMap(SeatArea::getId, a -> a));

        //过滤区域
        List<Long> candidateAreaIds = areas.stream()
                .map(SeatArea::getId)
                .collect(Collectors.toList());
        if (areaId != null && areaMap.containsKey(areaId)) {
            candidateAreaIds = Collections.singletonList(areaId);
        } else if (areaId != null && !areaMap.containsKey(areaId)) {
            //指定区域不在当前馆,直接返回空
            SeatAvailabilityResultDTO result = new SeatAvailabilityResultDTO();
            result.setTotal(0L);
            return result;
        }

        //查这些区域下的AVAILABLE座位
        LambdaQueryWrapper<Seat> seatWrapper = new LambdaQueryWrapper<>();
        seatWrapper.in(Seat::getAreaId, candidateAreaIds);
        seatWrapper.eq(Seat::getStatus, "AVAILABLE");
        if (hasPower != null && hasPower) {
            seatWrapper.eq(Seat::getHasPower, 1);
        }
        List<Seat> seatList = seatMapper.selectList(seatWrapper);
        if (seatList.isEmpty()) {
            SeatAvailabilityResultDTO result = new SeatAvailabilityResultDTO();
            result.setTotal(0L);
            return result;
        }

        //查这些座位在当日的ACTIVE预约,用于排除时间冲突
        List<Long> seatIds = seatList.stream().map(Seat::getId).collect(Collectors.toList());
        LambdaQueryWrapper<SeatReservation> resWrapper = new LambdaQueryWrapper<>();
        resWrapper.in(SeatReservation::getSeatId, seatIds);
        resWrapper.eq(SeatReservation::getReservationDate, reservationDate);
        resWrapper.eq(SeatReservation::getStatus, "ACTIVE");
        List<SeatReservation> reservations = seatReservationMapper.selectList(resWrapper);

        Map<Long, List<SeatReservation>> reservationMap = reservations.stream()
                .collect(Collectors.groupingBy(SeatReservation::getSeatId));

        List<SeatInfoDTO> candidates = new ArrayList<>();
        for (Seat seat : seatList) {
            List<SeatReservation> seatReservations = reservationMap.getOrDefault(seat.getId(), Collections.emptyList());
            if (hasTimeConflict(startTime, endTime, seatReservations)) {
                continue;
            }
            SeatArea area = areaMap.get(seat.getAreaId());
            if (area == null) {
                continue;
            }
            SeatInfoDTO dto = new SeatInfoDTO();
            dto.setSeatId(seat.getId());
            dto.setAreaId(area.getId());
            dto.setAreaName(area.getName());
            dto.setFloor(area.getFloor());
            dto.setSeatNo(seat.getSeatNo());
            dto.setHasPower(seat.getHasPower());
            candidates.add(dto);
        }

        //排序:有电源优先 -> 楼层升序 -> 区域名升序 -> 座位编号升序
        candidates.sort(Comparator
                .comparing(SeatInfoDTO::getHasPower, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SeatInfoDTO::getFloor, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SeatInfoDTO::getAreaName, Comparator.nullsLast(String::compareTo))
                .thenComparing(SeatInfoDTO::getSeatNo, Comparator.nullsLast(String::compareTo)));

        SeatAvailabilityResultDTO result = new SeatAvailabilityResultDTO();
        long totalCount = candidates.size();
        result.setAvailableTotal(totalCount);
        boolean assign = autoAssign == null || autoAssign;
        if (assign) {
            if (!candidates.isEmpty()) {
                result.setAssignedSeat(candidates.get(0));
            }
        } else {
            result.setTotal(totalCount);
            result.setSeats(candidates);
        }
        return result;
    }

    //校验日期与时间范围是否符合 seat 配置
    private void validateTimeRange(LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
        BusinessRulesProperties.SeatRules seatRules = businessRulesProperties.getSeat();

        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, reservationDate);
        if (days < seatRules.getEarliestBookingDays() || days > seatRules.getLatestBookingDays()) {
            throw new IllegalArgumentException("预约日期不在允许范围内");
        }

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("结束时间必须大于开始时间");
        }

        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes < seatRules.getMinDurationMinutes() || minutes > seatRules.getMaxDurationMinutes()) {
            throw new IllegalArgumentException("预约时长必须在" +
                    seatRules.getMinDurationMinutes() + "~" +
                    seatRules.getMaxDurationMinutes() + "分钟之间");
        }

        if (minutes % seatRules.getSlotStepMinutes() != 0) {
            throw new IllegalArgumentException("预约时长必须是" +
                    seatRules.getSlotStepMinutes() + "分钟的整数倍");
        }
    }

    //判断给定时间段是否与已有预约列表存在重叠
    private boolean hasTimeConflict(LocalTime startTime,
                                    LocalTime endTime,
                                    List<SeatReservation> reservations) {
        for (SeatReservation r : reservations) {
            LocalTime existStart = r.getStartTime();
            LocalTime existEnd = r.getEndTime();
            if (existStart == null || existEnd == null) {
                continue;
            }
            //不重叠的条件是: newEnd <= existStart 或 newStart >= existEnd
            boolean noOverlap = endTime.compareTo(existStart) <= 0
                    || startTime.compareTo(existEnd) >= 0;
            if (!noOverlap) {
                return true;
            }
        }
        return false;
    }
}

