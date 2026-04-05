package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.config.BusinessRulesProperties;
import com.library.constant.FaultConstants;
import com.library.dto.fault.*;
import com.library.entity.FaultReport;
import com.library.exception.BusinessException;
import com.library.mapper.FaultReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaultService {

    private final FaultReportMapper faultReportMapper;
    private final BusinessRulesProperties businessRulesProperties;

    public FaultReportVO createReport(FaultReportCreateRequest req) {
        if (!hasAnyTarget(req)) {
            throw new BusinessException("报修目标不能全部为空，请至少填写 libraryId、areaId、seatId、equipmentId 之一");
        }
        if (!StringUtils.hasText(req.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (!StringUtils.hasText(req.getFaultType()) || !FaultConstants.FAULT_TYPES.contains(req.getFaultType())) {
            throw new BusinessException("faultType 无效，允许值: " + FaultConstants.FAULT_TYPES);
        }
        if (!StringUtils.hasText(req.getSeverity()) || !FaultConstants.SEVERITIES.contains(req.getSeverity())) {
            throw new BusinessException("severity 无效，允许值: low, medium, high");
        }
        if (!StringUtils.hasText(req.getReportSource()) || !FaultConstants.REPORT_SOURCES.contains(req.getReportSource())) {
            throw new BusinessException("reportSource 无效，允许值: USER, MONITOR, ADMIN, SYSTEM");
        }

        FaultReport e = new FaultReport();
        e.setLibraryId(req.getLibraryId());
        e.setAreaId(req.getAreaId());
        e.setSeatId(req.getSeatId());
        e.setEquipmentId(req.getEquipmentId());
        e.setFaultType(req.getFaultType());
        e.setSeverity(req.getSeverity());
        e.setStatus(FaultConstants.STATUS_REPORTED);
        e.setTitle(req.getTitle().trim());
        e.setDescription(req.getDescription());
        e.setReportSource(req.getReportSource());
        e.setReportUserId(req.getReportUserId());
        e.setCreatedTime(LocalDateTime.now());
        e.setUpdatedTime(LocalDateTime.now());

        faultReportMapper.insert(e);
        return toVO(faultReportMapper.selectById(e.getId()));
    }

    public FaultReportVO getById(Long id) {
        FaultReport e = faultReportMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(404, "工单不存在");
        }
        return toVO(e);
    }

    public Map<String, Object> list(Long libraryId, Long areaId, Long seatId, Long equipmentId,
                                    String status, String faultType, String severity,
                                    LocalDateTime startTime, LocalDateTime endTime,
                                    int page, int size) {
        LambdaQueryWrapper<FaultReport> w = new LambdaQueryWrapper<>();
        if (libraryId != null) {
            w.eq(FaultReport::getLibraryId, libraryId);
        }
        if (areaId != null) {
            w.eq(FaultReport::getAreaId, areaId);
        }
        if (seatId != null) {
            w.eq(FaultReport::getSeatId, seatId);
        }
        if (equipmentId != null) {
            w.eq(FaultReport::getEquipmentId, equipmentId);
        }
        if (StringUtils.hasText(status)) {
            w.eq(FaultReport::getStatus, status.trim());
        }
        if (StringUtils.hasText(faultType)) {
            w.eq(FaultReport::getFaultType, faultType.trim());
        }
        if (StringUtils.hasText(severity)) {
            w.eq(FaultReport::getSeverity, severity.trim());
        }
        if (startTime != null) {
            w.ge(FaultReport::getCreatedTime, startTime);
        }
        if (endTime != null) {
            w.le(FaultReport::getCreatedTime, endTime);
        }
        w.orderByDesc(FaultReport::getCreatedTime);

        Page<FaultReport> p = new Page<>(page, size);
        Page<FaultReport> result = faultReportMapper.selectPage(p, w);

        List<FaultReportVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, Object> out = new HashMap<>();
        out.put("total", result.getTotal());
        out.put("page", page);
        out.put("size", size);
        out.put("records", records);
        return out;
    }

    public FaultReportVO patchStatus(Long id, FaultStatusPatchRequest req) {
        if (!StringUtils.hasText(req.getStatus()) || !FaultConstants.STATUSES.contains(req.getStatus())) {
            throw new BusinessException("status 无效，允许值: " + FaultConstants.STATUSES);
        }
        FaultReport e = faultReportMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(404, "工单不存在");
        }

        LambdaUpdateWrapper<FaultReport> u = new LambdaUpdateWrapper<>();
        u.eq(FaultReport::getId, id);
        u.set(FaultReport::getStatus, req.getStatus());
        u.set(FaultReport::getUpdatedTime, LocalDateTime.now());
        if (req.getAssignee() != null) {
            u.set(FaultReport::getAssignee, req.getAssignee());
        }
        if (req.getAdminRemark() != null) {
            u.set(FaultReport::getAdminRemark, req.getAdminRemark());
        }
        if (FaultConstants.STATUS_RESTORED.equals(req.getStatus()) || FaultConstants.STATUS_CLOSED.equals(req.getStatus())) {
            u.set(FaultReport::getResolvedTime, LocalDateTime.now());
        } else {
            u.set(FaultReport::getResolvedTime, null);
        }
        faultReportMapper.update(null, u);
        return toVO(faultReportMapper.selectById(id));
    }

    public FaultHealthQueryResponse healthQuery(FaultHealthQueryRequest request) {
        BusinessRulesProperties.FaultRules rules = businessRulesProperties.getFault();
        List<String> active = rules.getActiveStatuses();
        if (active == null || active.isEmpty()) {
            active = List.of("REPORTED", "ACCEPTED", "IN_PROGRESS");
        }
        List<String> excludeSev = rules.getExcludeSeverities();
        if (excludeSev == null) {
            excludeSev = List.of("medium", "high");
        }
        final List<String> activeFinal = active;
        final List<String> excludeFinal = excludeSev;

        FaultHealthQueryResponse resp = new FaultHealthQueryResponse();
        if (request.getResources() == null) {
            return resp;
        }
        for (FaultHealthQueryRequest.FaultHealthResourceRef ref : request.getResources()) {
            if (ref == null || ref.getResourceId() == null || !StringUtils.hasText(ref.getResourceType())) {
                continue;
            }
            String type = ref.getResourceType().trim().toUpperCase();
            if (!FaultConstants.HEALTH_RESOURCE_TYPES.contains(type)) {
                throw new BusinessException("resourceType 无效: " + ref.getResourceType() + "，允许: LIBRARY, SEAT_AREA, SEAT, EQUIPMENT");
            }
            FaultHealthQueryResponse.FaultHealthItemVO item = evaluateHealth(
                    type, ref.getResourceId(), activeFinal, excludeFinal, rules.isLibraryExcludeOnlyHigh());
            resp.getResults().add(item);
        }
        return resp;
    }

    private FaultHealthQueryResponse.FaultHealthItemVO evaluateHealth(
            String resourceType, Long resourceId,
            List<String> activeStatuses, List<String> excludeSeverities,
            boolean libraryExcludeOnlyHigh) {

        LambdaQueryWrapper<FaultReport> w = new LambdaQueryWrapper<>();
        w.in(FaultReport::getStatus, activeStatuses);
        switch (resourceType) {
            case "LIBRARY":
                w.eq(FaultReport::getLibraryId, resourceId);
                break;
            case "SEAT_AREA":
                w.eq(FaultReport::getAreaId, resourceId);
                break;
            case "SEAT":
                w.eq(FaultReport::getSeatId, resourceId);
                break;
            case "EQUIPMENT":
                w.eq(FaultReport::getEquipmentId, resourceId);
                break;
            default:
                break;
        }
        w.orderByDesc(FaultReport::getCreatedTime);
        List<FaultReport> list = faultReportMapper.selectList(w);

        List<FaultReport> excluding = list.stream()
                .filter(f -> shouldExclude(f, resourceType, excludeSeverities, libraryExcludeOnlyHigh))
                .collect(Collectors.toList());

        boolean available = excluding.isEmpty();
        String summary = excluding.stream()
                .max(Comparator.comparing(FaultReport::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(FaultReport::getTitle)
                .orElse(null);

        boolean critical = !excluding.isEmpty();
        return new FaultHealthQueryResponse.FaultHealthItemVO(resourceType, resourceId, available, critical, summary);
    }

    private boolean shouldExclude(FaultReport f, String resourceType, List<String> excludeSeverities,
                                  boolean libraryExcludeOnlyHigh) {
        String sev = f.getSeverity();
        if (sev == null) {
            return false;
        }
        if ("LIBRARY".equals(resourceType) && libraryExcludeOnlyHigh) {
            return "high".equalsIgnoreCase(sev);
        }
        return excludeSeverities.stream().anyMatch(s -> s.equalsIgnoreCase(sev));
    }

    private boolean hasAnyTarget(FaultReportCreateRequest req) {
        return req.getLibraryId() != null || req.getAreaId() != null
                || req.getSeatId() != null || req.getEquipmentId() != null;
    }

    private FaultReportVO toVO(FaultReport e) {
        if (e == null) {
            return null;
        }
        FaultReportVO vo = new FaultReportVO();
        vo.setId(e.getId());
        vo.setLibraryId(e.getLibraryId());
        vo.setAreaId(e.getAreaId());
        vo.setSeatId(e.getSeatId());
        vo.setEquipmentId(e.getEquipmentId());
        vo.setFaultType(e.getFaultType());
        vo.setSeverity(e.getSeverity());
        vo.setStatus(e.getStatus());
        vo.setTitle(e.getTitle());
        vo.setDescription(e.getDescription());
        vo.setAdminRemark(e.getAdminRemark());
        vo.setReportSource(e.getReportSource());
        vo.setReportUserId(e.getReportUserId());
        vo.setAssignee(e.getAssignee());
        vo.setCreatedTime(e.getCreatedTime());
        vo.setUpdatedTime(e.getUpdatedTime());
        vo.setResolvedTime(e.getResolvedTime());
        return vo;
    }
}
