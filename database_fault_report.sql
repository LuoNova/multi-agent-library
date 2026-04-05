-- =============================================================================
-- 故障报修模块：设备表扩展 + 故障工单表
-- 执行前请确认已存在：tb_library、tb_seat_area、tb_seat、tb_equipment、tb_user
-- MySQL 8.0+ 推荐（CHECK 约束在 8.0.16+ 生效）
-- =============================================================================

-- 1) 扩展 tb_equipment（与《数据库结构.md》一致）
ALTER TABLE `tb_equipment`
  ADD COLUMN `area_id` bigint DEFAULT NULL COMMENT '所属区域(tb_seat_area.id)' AFTER `library_id`,
  ADD COLUMN `remark` varchar(200) DEFAULT NULL COMMENT '备注' AFTER `status`,
  ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
  ADD KEY `idx_equip_area` (`area_id`),
  ADD CONSTRAINT `fk_equip_area` FOREIGN KEY (`area_id`) REFERENCES `tb_seat_area` (`id`);

-- 若某环境已手工加过上述列，可逐条执行并忽略 “Duplicate column” 错误。

-- 2) 新建 tb_fault_report
CREATE TABLE IF NOT EXISTS `tb_fault_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint DEFAULT NULL COMMENT '所属馆(系统级或未知时为空)',
  `area_id` bigint DEFAULT NULL COMMENT '所属区域',
  `seat_id` bigint DEFAULT NULL COMMENT '所属座位',
  `equipment_id` bigint DEFAULT NULL COMMENT '关联馆内设备(tb_equipment.id)',
  `fault_type` varchar(30) NOT NULL COMMENT '故障类型:seat_broken/power_failure/env_issue/network_fault/other',
  `severity` varchar(20) NOT NULL COMMENT '严重程度:low/medium/high',
  `status` varchar(20) NOT NULL DEFAULT 'REPORTED' COMMENT 'REPORTED/ACCEPTED/IN_PROGRESS/RESTORED/CLOSED',
  `title` varchar(200) NOT NULL COMMENT '简要标题',
  `description` text COMMENT '报修人详细描述',
  `admin_remark` varchar(500) DEFAULT NULL COMMENT '运维/管理员处理备注',
  `report_source` varchar(20) NOT NULL COMMENT '报修来源:用户/监控/管理员/系统',
  `report_user_id` bigint DEFAULT NULL COMMENT '报修用户ID',
  `assignee` varchar(50) DEFAULT NULL COMMENT '当前负责人(姓名或工号)',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `resolved_time` datetime DEFAULT NULL COMMENT '恢复/关闭时间',
  PRIMARY KEY (`id`),
  KEY `idx_fault_library` (`library_id`),
  KEY `idx_fault_area` (`area_id`),
  KEY `idx_fault_seat` (`seat_id`),
  KEY `idx_fault_equipment` (`equipment_id`),
  KEY `idx_fault_status_severity` (`status`,`severity`),
  KEY `idx_fault_created` (`created_time`),
  CONSTRAINT `fk_fault_library` FOREIGN KEY (`library_id`) REFERENCES `tb_library` (`id`),
  CONSTRAINT `fk_fault_area` FOREIGN KEY (`area_id`) REFERENCES `tb_seat_area` (`id`),
  CONSTRAINT `fk_fault_seat` FOREIGN KEY (`seat_id`) REFERENCES `tb_seat` (`id`),
  CONSTRAINT `fk_fault_equipment` FOREIGN KEY (`equipment_id`) REFERENCES `tb_equipment` (`id`),
  CONSTRAINT `fk_fault_report_user` FOREIGN KEY (`report_user_id`) REFERENCES `tb_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_fault_at_least_one_target` CHECK (
    (`library_id` IS NOT NULL)
    OR (`area_id` IS NOT NULL)
    OR (`seat_id` IS NOT NULL)
    OR (`equipment_id` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='故障工单表(统一报修)';
