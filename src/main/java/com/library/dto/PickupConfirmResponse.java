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
}
