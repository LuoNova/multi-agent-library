package com.library.controller;

import com.library.common.Result;
import com.library.service.TransferService;
import com.library.service.TransferService.TransferCompleteResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

//调拨履约接口控制器
//提供调拨完成回调接口，供物流系统或管理后台调用
@Slf4j
@RestController
@RequestMapping("/api/transfer")
@Tag(name = "图书调拨", description = "处理跨馆调拨的履约与回调")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @PostMapping("/complete")
    @Operation(summary = "调拨完成回调", description = "物流系统或馆员确认图书已到达目标馆，完成调拨闭环")
    public Result<TransferCompleteResult> completeTransfer(
            @Parameter(description = "调拨完成请求", required = true)
            @RequestBody TransferCompleteRequest request) {

        log.info("收到调拨完成请求：transferId={}, 到达时间={}",
                request.getTransferId(), request.getActualArriveTime());

        TransferCompleteResult result = transferService.completeTransfer(
                request.getTransferId(),
                request.getActualArriveTime()
        );
        
        if (result.isSuccess()) {
            return Result.success(result.getMessage(), result);
        } else {
            return Result.fail(result.getMessage());
        }
    }

    //调拨完成请求参数
    @Schema(description = "调拨完成回调请求参数")
    @Data
    public static class TransferCompleteRequest {
        @Schema(description = "调拨单ID", example = "20", required = true)
        private Long transferId;

        @Schema(description = "实际到达时间（可选，默认为当前时间）", example = "2024-01-15T14:30:00")
        private LocalDateTime actualArriveTime;
    }
}