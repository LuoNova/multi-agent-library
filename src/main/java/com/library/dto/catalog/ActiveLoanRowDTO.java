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
@Schema(description = "当前在借一行（用于还书选书）")
public class ActiveLoanRowDTO {

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

    @Schema(description = "应还时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    @Schema(description = "副本当前所在馆ID（建议作为默认还书馆）")
    private Long suggestedReturnLibraryId;

    @Schema(description = "副本当前所在馆名称")
    private String suggestedReturnLibraryName;
}
