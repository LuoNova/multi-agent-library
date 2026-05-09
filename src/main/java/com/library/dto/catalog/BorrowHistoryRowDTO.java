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
@Schema(description = "借阅历史一行（读者查询）")
public class BorrowHistoryRowDTO {

    @Schema(description = "借阅记录ID")
    private Long borrowId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "副本ID")
    private Long copyId;

    @Schema(description = "书目ID")
    private Long biblioId;

    @Schema(description = "书名")
    private String bookTitle;

    @Schema(description = "借阅状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "借出/开始借阅时间")
    private LocalDateTime borrowTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "应还时间")
    private LocalDateTime dueTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际归还时间")
    private LocalDateTime returnTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际取书时间")
    private LocalDateTime actualPickupTime;

    @Schema(description = "取书馆ID")
    private Long pickupLibraryId;

    @Schema(description = "取书馆名称")
    private String pickupLibraryName;
}
