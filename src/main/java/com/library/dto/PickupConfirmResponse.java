package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 取书确认响应DTO
 */
@Data
@Schema(description = "取书确认响应")
public class PickupConfirmResponse {

    @Schema(description = "借阅记录ID")
    private Long borrowId;

    @Schema(description = "图书名称")
    private String bookTitle;

    @Schema(description = "借阅日期")
    private LocalDateTime borrowDate;

    @Schema(description = "应还日期")
    private LocalDateTime dueDate;

    @Schema(description = "取书馆名称")
    private String libraryName;

    //以下为座位推荐相关字段(取书成功场景)

    @Schema(description = "推荐占座的馆ID(通常为取书馆ID)")
    private Long recommendSeatLibraryId;

    @Schema(description = "推荐预约座位的日期(yyyy-MM-dd)")
    private String recommendSeatDate;

    @Schema(description = "座位推荐原因:TRANSFER_PICKUP/RESERVATION_PICKUP等")
    private String recommendSeatReason;

    @Schema(description = "是否建议前端展示“预约座位”入口")
    private Boolean actionRecommendSeat;
}
