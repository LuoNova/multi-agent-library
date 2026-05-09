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
@Schema(description = "待取书一行（RESERVED 等）")
public class PickupPendingRowDTO {

    @Schema(description = "借阅记录ID")
    private Long borrowId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "副本ID")
    private Long copyId;

    @Schema(description = "书名")
    private String bookTitle;

    @Schema(description = "借阅状态")
    private String status;

    @Schema(description = "取书馆ID")
    private Long pickupLibraryId;

    @Schema(description = "取书馆名称")
    private String pickupLibraryName;

    @Schema(description = "取书截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupDeadline;
}
