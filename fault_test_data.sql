-- =============================================================================
-- 故障报修模块测试数据
-- 前置：已执行 test_data.sql（用户/馆）、seat_test_data.sql（座位区域与座位）、
--       并已创建 tb_fault_report（见 database_fault_report.sql）
-- 用法：mysql -u root -p library_db < fault_test_data.sql
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 可选：保证有一台设备用于 equipmentId 报修与健康查询（id=1）
INSERT INTO tb_equipment (id, library_id, area_id, type, name, location, status, remark, install_time, create_time, update_time)
VALUES (1, 1, NULL, 'GATE', '东门门禁', '理科馆东门', 'NORMAL', NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  location = VALUES(location),
  update_time = NOW();

-- 清空本模块测试工单（避免重复执行脚本时主键冲突）
DELETE FROM tb_fault_report;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 数据说明（与默认 fault 配置一致：active=REPORTED/ACCEPTED/IN_PROGRESS，
-- exclude=medium+high，library-exclude-only-high=true）
-- -----------------------------------------------------------------------------

-- F1：理科馆 座位 1001，medium + REPORTED → 健康查询 SEAT/1001 应 unavailable
INSERT INTO tb_fault_report (
  library_id, area_id, seat_id, equipment_id,
  fault_type, severity, status, title, description, admin_remark,
  report_source, report_user_id, assignee,
  created_time, updated_time, resolved_time
) VALUES (
  1, NULL, 1001, NULL,
  'seat_broken', 'medium', 'REPORTED', '【测试】座椅损坏-1001', '用于健康查询', NULL,
  'USER', 1, NULL,
  NOW(), NOW(), NULL
);

-- F2：理科馆 区域 1，medium + IN_PROGRESS → 健康查询 SEAT_AREA/1 应 unavailable
INSERT INTO tb_fault_report (
  library_id, area_id, seat_id, equipment_id,
  fault_type, severity, status, title, description, admin_remark,
  report_source, report_user_id, assignee,
  created_time, updated_time, resolved_time
) VALUES (
  1, 1, NULL, NULL,
  'env_issue', 'medium', 'IN_PROGRESS', '【测试】区域施工-区域1', NULL, NULL,
  'ADMIN', 1, '运维A',
  NOW(), NOW(), NULL
);

-- F3：理科馆 馆级，high + REPORTED → 健康查询 LIBRARY/1 应 unavailable（整馆排除）
INSERT INTO tb_fault_report (
  library_id, area_id, seat_id, equipment_id,
  fault_type, severity, status, title, description, admin_remark,
  report_source, report_user_id, assignee,
  created_time, updated_time, resolved_time
) VALUES (
  1, NULL, NULL, NULL,
  'network_fault', 'high', 'REPORTED', '【测试】馆级网络故障-理科馆', NULL, NULL,
  'MONITOR', NULL, NULL,
  NOW(), NOW(), NULL
);

-- F4：理科馆 馆级，medium + REPORTED → 在 library-exclude-only-high=true 下，
--      健康查询 LIBRARY/1 若仅存在本条（需手工删除 F3 后测）应仍 available
--      与 F3 同时存在时，因 F3 已为 high，LIBRARY/1 仍为 unavailable
INSERT INTO tb_fault_report (
  library_id, area_id, seat_id, equipment_id,
  fault_type, severity, status, title, description, admin_remark,
  report_source, report_user_id, assignee,
  created_time, updated_time, resolved_time
) VALUES (
  1, NULL, NULL, NULL,
  'other', 'medium', 'REPORTED', '【测试】馆级中严重-应不单独整馆封', NULL, NULL,
  'SYSTEM', NULL, NULL,
  NOW(), NOW(), NULL
);

-- F5：设备 1，low + REPORTED → 健康查询 EQUIPMENT/1 应 available（low 不在 exclude 列表）
INSERT INTO tb_fault_report (
  library_id, area_id, seat_id, equipment_id,
  fault_type, severity, status, title, description, admin_remark,
  report_source, report_user_id, assignee,
  created_time, updated_time, resolved_time
) VALUES (
  1, NULL, NULL, 1,
  'other', 'low', 'REPORTED', '【测试】设备低严重-应不排除', NULL, NULL,
  'USER', 1, NULL,
  NOW(), NOW(), NULL
);

-- F6：已关闭工单 RESTORED → 不参与活跃状态，可用于验证「不拦截」
INSERT INTO tb_fault_report (
  library_id, area_id, seat_id, equipment_id,
  fault_type, severity, status, title, description, admin_remark,
  report_source, report_user_id, assignee,
  created_time, updated_time, resolved_time
) VALUES (
  2, NULL, 1201, NULL,
  'seat_broken', 'high', 'RESTORED', '【测试】已恢复-不应影响健康', NULL, NULL,
  'USER', 2, NULL,
  NOW(), NOW(), NOW()
);

-- 说明：
-- 健康查询 LIBRARY/1：同时存在 F3(high) 与 F4(medium) → 应 unavailable（high 命中馆级规则即可）。
-- 若仅测「馆 medium 不封馆」：先 DELETE FROM tb_fault_report WHERE title LIKE '%馆级网络%'; 再只保留 F4 与必要数据。
