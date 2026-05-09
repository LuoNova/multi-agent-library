package com.library.dto.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端调拨记录一行（只读，含馆名与书名）")
public class AdminTransferRowDTO {

    @Schema(description = "调拨记录ID")
    private Long transferId;

    @Schema(description = "副本ID")
    private Long copyId;

    @Schema(description = "书目ID")
    private Long biblioId;

    @Schema(description = "书名")
    private String bookTitle;

    @Schema(description = "源馆ID")
    private Long fromLibraryId;

    @Schema(description = "源馆名称")
    private String fromLibraryName;

    @Schema(description = "目标馆ID")
    private Long toLibraryId;

    @Schema(description = "目标馆名称")
    private String toLibraryName;

    @Schema(description = "状态：PENDING/IN_TRANSIT/COMPLETED/CANCELED 等")
    private String status;

    @Schema(description = "调拨单ID")
    private Long orderId;

    @Schema(description = "调拨建议ID")
    private Long suggestionId;

    @Schema(description = "调拨原因")
    private String transferReason;

    @Schema(description = "接收用户ID")
    private Long receiverUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请时间")
    private LocalDateTime requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预计到达")
    private LocalDateTime estimatedArrivalTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际到达")
    private LocalDateTime actualArrivalTime;
}
