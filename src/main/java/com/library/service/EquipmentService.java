package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.constant.EquipmentConstants;
import com.library.entity.Equipment;
import com.library.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final FaultService faultService;

    public Equipment getById(Long id) {
        return equipmentMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Equipment updateStatus(Long equipmentId, String newStatus) {
        if (newStatus == null || !EquipmentConstants.ALL_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("无效的设备状态: " + newStatus);
        }
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("设备不存在: " + equipmentId);
        }
        String old = equipment.getStatus();
        equipment.setStatus(newStatus);
        equipment.setUpdateTime(LocalDateTime.now());
        equipmentMapper.updateById(equipment);

        if (EquipmentConstants.STATUS_FAULT.equals(newStatus)
                && !EquipmentConstants.STATUS_FAULT.equals(old)) {
            faultService.createAutoReportForEquipmentFailure(equipmentId, equipment.getLibraryId());
        }
        if (EquipmentConstants.STATUS_NORMAL.equals(newStatus)
                && EquipmentConstants.STATUS_FAULT.equals(old)) {
            faultService.closeActiveFaultsForEquipment(equipmentId);
        }
        return equipmentMapper.selectById(equipmentId);
    }

    public List<Equipment> listByLibrary(Long libraryId) {
        LambdaQueryWrapper<Equipment> w = new LambdaQueryWrapper<>();
        w.eq(Equipment::getLibraryId, libraryId);
        return equipmentMapper.selectList(w);
    }
}
