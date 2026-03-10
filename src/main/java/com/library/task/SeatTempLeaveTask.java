package com.library.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.dto.NotificationMessage;
import com.library.entity.SeatReservation;
import com.library.mapper.SeatMapper;
import com.library.mapper.SeatReservationMapper;
import com.library.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//座位暂离处理定时任务
@Slf4j
@Component
public class SeatTempLeaveTask {

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired
    private SeatReservationMapper seatReservationMapper;

    @Autowired
    private SeatMapper seatMapper;

    //每5分钟执行一次: 0 */5 * * * ?
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processTempLeave() {
        LocalDateTime now = LocalDateTime.now();
        log.info("========== 开始执行座位暂离处理任务, now={} ==========", now);

        //1.处理超过基础暂离时长但未超过强制截止时间的记录:发送提醒
        LambdaQueryWrapper<SeatReservation> warnWrapper = new LambdaQueryWrapper<>();
        warnWrapper.eq(SeatReservation::getStatus, "ACTIVE")
                .eq(SeatReservation::getTempLeaveStatus, "IN_TEMP_LEAVE")
                .isNotNull(SeatReservation::getTempLeaveDeadline)
                .le(SeatReservation::getTempLeaveDeadline, now)
                .ge(SeatReservation::getTempLeaveForceDeadline, now);
        List<SeatReservation> warnList = seatReservationMapper.selectList(warnWrapper);
        log.info("发现{}条需要发送暂离超时提醒的记录", warnList.size());
        for (SeatReservation r : warnList) {
            try {
                sendTempLeaveWarning(r);
                r.setTempLeaveStatus("TIMEOUT_WARNED");
                seatReservationMapper.updateById(r);
            } catch (Exception e) {
                log.error("处理暂离超时提醒失败: reservationId={}", r.getId(), e);
            }
        }

        //2.处理超过强制截止时间的记录:结束使用并释放座位
        LambdaQueryWrapper<SeatReservation> forceWrapper = new LambdaQueryWrapper<>();
        forceWrapper.eq(SeatReservation::getStatus, "ACTIVE")
                .in(SeatReservation::getTempLeaveStatus, "IN_TEMP_LEAVE", "TIMEOUT_WARNED")
                .isNotNull(SeatReservation::getTempLeaveForceDeadline)
                .lt(SeatReservation::getTempLeaveForceDeadline, now);
        List<SeatReservation> forceList = seatReservationMapper.selectList(forceWrapper);
        log.info("发现{}条暂离严重超时需要结束使用的记录", forceList.size());
        for (SeatReservation r : forceList) {
            try {
                Long seatId = r.getSeatId();
                r.setStatus("COMPLETED");
                r.setTempLeaveStatus("ENDED");
                r.setTempLeaveStartTime(null);
                r.setTempLeaveDeadline(null);
                r.setTempLeaveForceDeadline(null);
                seatReservationMapper.updateById(r);
                if (seatId != null) {
                    seatMapper.updateStatus(seatId, "AVAILABLE");
                }
                log.info("暂离严重超时处理完成: reservationId={}, seatId={}", r.getId(), seatId);
            } catch (Exception e) {
                log.error("处理暂离严重超时失败: reservationId={}", r.getId(), e);
            }
        }

        log.info("========== 座位暂离处理任务结束 ==========");
    }

    //发送暂离超时提醒
    private void sendTempLeaveWarning(SeatReservation r) {
        if (notificationService == null) {
            log.info("通知服务未启用,跳过暂离超时提醒: reservationId={}", r.getId());
            return;
        }
        try {
            NotificationMessage msg = new NotificationMessage();
            msg.setUserId(r.getUserId());
            msg.setType("SEAT_TEMP_LEAVE_TIMEOUT_WARNING");
            msg.setChannel("IN_APP");
            Map<String, String> params = new HashMap<>();
            params.put("reservationId", String.valueOf(r.getId()));
            params.put("seatId", r.getSeatId() == null ? "" : String.valueOf(r.getSeatId()));
            params.put("deadline", r.getTempLeaveDeadline() == null ? "" : r.getTempLeaveDeadline().toString());
            msg.setTemplateParams(params);
            notificationService.sendNotification(msg);
            log.info("暂离超时提醒通知已发送: reservationId={}, userId={}", r.getId(), r.getUserId());
        } catch (Exception e) {
            log.error("发送暂离超时提醒通知失败: reservationId={}", r.getId(), e);
            throw e;
        }
    }
}

