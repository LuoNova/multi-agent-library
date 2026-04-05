package com.library.constant;

import java.util.Set;

/**
 * 故障报修模块：存库枚举值（与 tb_fault_report、接口约定一致）
 */
public final class FaultConstants {

    private FaultConstants() {
    }

    public static final Set<String> FAULT_TYPES = Set.of(
            "seat_broken", "power_failure", "env_issue", "network_fault", "other");

    public static final Set<String> SEVERITIES = Set.of("low", "medium", "high");

    public static final Set<String> STATUSES = Set.of(
            "REPORTED", "ACCEPTED", "IN_PROGRESS", "RESTORED", "CLOSED");

    public static final Set<String> REPORT_SOURCES = Set.of("USER", "MONITOR", "ADMIN", "SYSTEM");

    /** 健康查询请求中的资源类型 */
    public static final Set<String> HEALTH_RESOURCE_TYPES = Set.of(
            "LIBRARY", "SEAT_AREA", "SEAT", "EQUIPMENT");

    public static final String STATUS_REPORTED = "REPORTED";
    public static final String STATUS_RESTORED = "RESTORED";
    public static final String STATUS_CLOSED = "CLOSED";
}
