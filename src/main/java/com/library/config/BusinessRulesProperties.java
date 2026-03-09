package com.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//业务规则配置属性类，支持动态配置
@Data
@Component
@ConfigurationProperties(prefix = "library.business-rules")
public class BusinessRulesProperties {

    //借阅规则
    private BorrowRules borrow = new BorrowRules();

    //预约规则
    private ReservationRules reservation = new ReservationRules();

    //调拨规则
    private TransferRules transfer = new TransferRules();

    //信用规则
    private CreditRules credit = new CreditRules();

    //座位规则
    private SeatRules seat = new SeatRules();

    @Data
    public static class BorrowRules {
        //最大借阅数量
        private int maxBorrowCount = 10;
        //默认借阅期限（天）
        private int defaultBorrowDays = 30;
    }

    @Data
    public static class ReservationRules {
        //预约有效期（天）
        private int expireDays = 3;
        //预约排队最大人数
        private int maxQueueSize = 100;
    }

    @Data
    public static class TransferRules {
        //调拨后预留时间（小时）
        private int reserveHours = 24;
        //调拨预计时间（分钟）
        private int estimatedMinutes = 30;
        //最大调拨距离（公里）
        private double maxDistance = 50.0;
    }

    @Data
    public static class CreditRules {
        //借书逾期每日扣分
        private int dailyDeduction = 2;
        //借书逾期上限扣分
        private int ceilingDeduction = 20;
    }

    @Data
    public static class SeatRules {
        //单次预约最小时长（分钟）
        private int minDurationMinutes = 60;
        //单次预约最大时长（分钟）
        private int maxDurationMinutes = 840;
        //预约时段步长（分钟）
        private int slotStepMinutes = 60;
        //最早可预约日期（相对今天的天数）
        private int earliestBookingDays = 0;
        //最晚可预约日期（相对今天的天数）
        private int latestBookingDays = 2;
        //未签到多少分钟后视为 NO_SHOW
        private int noShowAfterMinutes = 30;
        //默认暂离允许时长（分钟）
        private int tempLeaveDefaultMinutes = 30;
        //中午饭点暂离规则
        private TempLeaveRule tempLeaveLunch = new TempLeaveRule();
        //晚饭时间暂离规则
        private TempLeaveRule tempLeaveDinner = new TempLeaveRule();
    }

    @Data
    public static class TempLeaveRule {
        //开始时间(如11:30)
        private String start;
        //结束时间(如13:30)
        private String end;
        //最大暂离分钟数
        private int maxMinutes = 120;
    }
}
