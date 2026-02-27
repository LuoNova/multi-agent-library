package com.library.controller;

import com.library.dto.PickupConfirmRequest;
import com.library.dto.PickupConfirmResponse;
import com.library.common.Result;
import com.library.service.PickupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 取书确认Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/borrow")
@Tag(name = "取书确认", description = "用户到馆取书确认接口")
public class PickupController {

    @Autowired
    private PickupService pickupService;

    @PostMapping("/confirm-pickup")
    @Operation(summary = "确认取书", description = "用户到馆后确认取书,完成借阅流程")
    public Result<PickupConfirmResponse> confirmPickup(@RequestBody PickupConfirmRequest request) {
        log.info("收到取书确认请求: borrowId={}, userId={}", request.getBorrowId(), request.getUserId());
        PickupConfirmResponse response = pickupService.confirmPickup(request);
        return Result.success("取书成功", response);
    }
}
