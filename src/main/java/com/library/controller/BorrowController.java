package com.library.controller;

import com.library.agent.dto.TaskResultHolder;
import com.library.common.Result;
import com.alibaba.fastjson.JSON;
import com.library.dto.BorrowResult;
import com.library.util.AgentTaskManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/borrow")
@Tag(name = "图书借还", description = "跨馆借书协商与调拨")
public class BorrowController {

    @Autowired
    private AgentTaskManager taskManager;

    @PostMapping("/request")
    @Operation(summary = "发起借书请求", description = "用户发起借书，系统自动判断本地/跨馆调拨")
    public Result<BorrowResult> borrowRequest(
            @Parameter(description = "借书请求参数", required = true)
            @RequestBody BorrowRequest request) {
        try {
            String taskId = taskManager.submitTask(
                    request.getUserId(),
                    request.getBiblioId(),
                    request.getPreferredLibraryId()
            );

            log.info("提交借书请求: {}", taskId);

            TaskResultHolder holder = taskManager.getHolder(taskId);
            boolean completed = holder.await(5, TimeUnit.SECONDS);

            if (completed) {
                // 解析JSON字符串为对象
                BorrowResult borrowResult = JSON.parseObject(holder.getResultJson(), BorrowResult.class);
                
                // 根据status判断成功或失败
                if ("SUCCESS".equals(borrowResult.getStatus())) {
                    return Result.success(borrowResult.getMessage(), borrowResult);
                } else {
                    return Result.fail(borrowResult.getMessage());
                }
            } else {
                return Result.fail("处理超时,任务ID: " + taskId);
            }

        } catch (Exception e) {
            log.error("提交借书请求失败", e);
            return Result.fail("提交借书请求失败: " + e.getMessage());
        }
    }

    @Schema(description = "借书请求参数")
    @Data
    public static class BorrowRequest {
        @Schema(description = "用户ID", example = "1", required = true)
        private Long userId;

        @Schema(description = "书目ID", example = "1", required = true)
        private Long biblioId;

        @Schema(description = "期望取书馆ID", example = "1", required = true)
        private Long preferredLibraryId;
    }
}