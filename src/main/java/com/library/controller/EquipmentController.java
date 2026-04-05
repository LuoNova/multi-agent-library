package com.library.controller;

import com.library.common.Result;
import com.library.dto.EquipmentStatusUpdateRequest;
import com.library.entity.Equipment;
import com.library.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备状态与故障工单双向联动")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新设备状态", description = "NORMAL/FAULT/MAINTAIN/DISABLED；FAULT 且无未结工单时自动建 SYSTEM 工单；从 FAULT 改为 NORMAL 时关闭未结设备工单")
    public Result<Equipment> updateStatus(
            @PathVariable Long id,
            @RequestBody EquipmentStatusUpdateRequest request) {
        return Result.success(equipmentService.updateStatus(id, request.getStatus()));
    }
}
