package com.library.controller;

import com.library.common.Result;
import com.library.service.TransferService;
import com.library.service.TransferService.TransferCompleteResult;
import com.library.service.TransferService.BatchCompleteResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

        log.info("收到调拨完成请求：transferId={}", request.getTransferId());

        TransferCompleteResult result = transferService.completeTransfer(request.getTransferId());

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
    }

    //批量调拨完成接口
    @PostMapping("/complete-batch")
    @Operation(summary = "批量调拨完成回调", description = "物流系统或馆员确认批量调拨图书已到达目标馆，完成批量调拨闭环")
    public Result<BatchCompleteResult> completeBatchTransfer(
            @Parameter(description = "批量调拨完成请求", required = true)
            @RequestBody BatchCompleteRequest request) {

        log.info("收到批量调拨完成请求：orderId={}", request.getOrderId());

        BatchCompleteResult result = transferService.completeBatchTransfer(request.getOrderId());

        if (result.isSuccess()) {
            return Result.success(result.getMessage(), result);
        } else {
            return Result.fail(result.getMessage());
        }
    }

    //批量调拨完成请求参数
    @Schema(description = "批量调拨完成回调请求参数")
    @Data
    public static class BatchCompleteRequest {
        @Schema(description = "调拨单ID", example = "1", required = true)
        private Long orderId;
    }
}