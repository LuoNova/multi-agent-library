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
}
