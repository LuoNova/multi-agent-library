package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.Result;
import com.library.dto.BatchTransferProgressDTO;
import com.library.dto.TransferProgressDTO;
import com.library.entity.BookTransfer;
import com.library.mapper.BookTransferMapper;
import com.library.service.TransferProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//调拨进度查询接口
@Slf4j
@RestController
@RequestMapping("/api/transfer")
@Tag(name = "调拨进度", description = "调拨进度查询接口")
public class TransferProgressController {

    @Autowired
    private TransferProgressService progressService;
    
    @Autowired
    private BookTransferMapper transferMapper;

    @GetMapping("/progress/{transferId}")
    @Operation(summary = "查询单个调拨进度", description = "根据调拨记录ID查询详细进度信息")
    public Result<TransferProgressDTO> getTransferProgress(
            @Parameter(description = "调拨记录ID", required = true)
            @PathVariable Long transferId) {
        
        log.info("查询单个调拨进度: transferId={}", transferId);
        
        TransferProgressDTO progress = progressService.getTransferProgress(transferId);
        
        if (progress == null) {
            return Result.fail("调拨记录不存在");
        }
        
        return Result.success("查询成功", progress);
    }

    @GetMapping("/progress/batch/{orderId}")
    @Operation(summary = "查询批量调拨进度", description = "根据调拨单ID查询批量调拨的整体进度")
    public Result<BatchTransferProgressDTO> getBatchTransferProgress(
            @Parameter(description = "调拨单ID", required = true)
            @PathVariable Long orderId) {
        
        log.info("查询批量调拨进度: orderId={}", orderId);
        
        BatchTransferProgressDTO progress = progressService.getBatchTransferProgress(orderId);
        
        if (progress == null) {
            return Result.fail("调拨单不存在");
        }
        
        return Result.success("查询成功", progress);
    }

    @GetMapping("/my-transfers")
    @Operation(summary = "查询用户调拨列表", description = "查询当前用户的所有调拨记录")
    public Result<Map<String, Object>> getUserTransfers(
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "状态筛选(可选)")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码", required = false)
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", required = false)
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("查询用户调拨列表: userId={}, status={}, page={}, size={}", userId, status, page, size);
        
        //TODO: 实现用户调拨列表查询
        //需要关联BookBorrow表,找到用户相关的调拨记录
        
        //暂时返回空列表
        Map<String, Object> result = new HashMap<>();
        result.put("total", 0);
        result.put("page", page);
        result.put("size", size);
        result.put("records", List.of());
        
        return Result.success("查询成功", result);
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有调拨记录", description = "查询所有调拨记录(管理员用)")
    public Result<Map<String, Object>> getAllTransfers(
            @Parameter(description = "状态筛选(可选)")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码", required = false)
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", required = false)
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("查询所有调拨记录: status={}, page={}, size={}", status, page, size);
        
        //构建查询条件
        LambdaQueryWrapper<BookTransfer> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BookTransfer::getStatus, status);
        }
        wrapper.orderByDesc(BookTransfer::getRequestTime);
        
        //分页查询
        Page<BookTransfer> pageObj = new Page<>(page, size);
        Page<BookTransfer> result = transferMapper.selectPage(pageObj, wrapper);
        
        //构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("total", result.getTotal());
        response.put("page", page);
        response.put("size", size);
        response.put("records", result.getRecords());
        
        return Result.success("查询成功", response);
    }
}
