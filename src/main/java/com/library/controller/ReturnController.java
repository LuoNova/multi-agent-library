package com.library.controller;

import com.library.service.BookReturnService;
import com.library.service.BookReturnService.ReturnResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

//还书接口控制器
@Slf4j
@RestController
@RequestMapping("/api/book")
@Tag(name = "图书归还", description = "归还图书并自动处理预约队列")
public class ReturnController {

    @Autowired
    private BookReturnService bookReturnService;

    @PostMapping("/return")
    @Operation(summary = "归还图书", description = "用户归还图书，系统自动检查预约队列并触发调拨或预留")
    public String returnBook(
            @Parameter(description = "还书请求参数", required = true)
            @RequestBody ReturnRequest request) {

        log.info("收到还书请求：副本{}，用户{}，还书馆{}",
                request.getCopyId(), request.getUserId(), request.getReturnLibraryId());

        ReturnResult result = bookReturnService.processReturn(
                request.getCopyId(),
                request.getUserId(),
                request.getReturnLibraryId()
        );

        if (result.isSuccess()) {
            if (result.isHasReservation()) {
                //有预约处理的情况
                return String.format(
                        "{\"status\":\"SUCCESS\",\"message\":\"%s\",\"reservatorId\":%d,\"localReservation\":%b}",
                        result.getMessage(),
                        result.getReservatorId(),
                        result.isLocalReservation()
                );
            } else {
                //普通还书
                return String.format("{\"status\":\"SUCCESS\",\"message\":\"%s\"}", result.getMessage());
            }
        } else {
            return String.format("{\"status\":\"FAILED\",\"message\":\"%s\"}", result.getMessage());
        }
    }

    @Schema(description = "还书请求参数")
    @Data
    public static class ReturnRequest {
        @Schema(description = "图书副本ID（条码号）", example = "1001", required = true)
        private Long copyId;

        @Schema(description = "还书人用户ID", example = "2", required = true)
        private Long userId;

        @Schema(description = "还书所在馆ID（支持跨馆还书）", example = "1", required = true)
        private Long returnLibraryId;
    }
}