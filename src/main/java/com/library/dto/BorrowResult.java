package com.library.dto;

import lombok.Data;

//借书结果DTO
@Data
public class BorrowResult {
    /**
     * 策略: LOCAL_LOAN-本地借阅, TRANSFER_PROVIDE-跨馆调拨, RESERVATION-预约排队
     */
    private String strategy;

    /**
     * 借阅记录ID(本地借阅时)
     */
    private Long borrowId;

    /**
     * 预约记录ID(预约排队时)
     */
    private Long reservationId;

    /**
     * 调拨单ID(跨馆调拨时)
     */
    private Long transferId;

    /**
     * 图书副本ID
     */
    private Long copyId;

    /**
     * 源馆ID(跨馆调拨时)
     */
    private Long sourceLibraryId;

    /**
     * 目标馆ID
     */
    private Long targetLibraryId;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 预计到达时间(分钟,跨馆调拨时)
     */
    private Integer estimatedArrivalMinutes;

    /**
     * 状态: SUCCESS/FAILED
     */
    private String status;

    //推荐占座的馆ID(本地借阅等场景)
    private Long recommendSeatLibraryId;

    //推荐预约座位的日期(yyyy-MM-dd)
    private String recommendSeatDate;

    //推荐原因:LOCAL_LOAN/TRANSFER_PICKUP等
    private String recommendSeatReason;

    //是否建议前端展示“预约座位”入口
    private Boolean actionRecommendSeat;
}
