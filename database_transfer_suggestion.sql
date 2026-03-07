-- ========================================
-- 主动调拨功能 - 数据库表结构
-- ========================================

-- 1. 新增调拨建议表
CREATE TABLE IF NOT EXISTS `tb_transfer_suggestion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `from_library_id` bigint NOT NULL COMMENT '源馆ID',
  `to_library_id` bigint NOT NULL COMMENT '目标馆ID',
  `biblio_id` bigint NOT NULL COMMENT '书目ID',
  `suggested_quantity` int NOT NULL COMMENT '建议调拨数量',
  `priority_score` decimal(5,2) DEFAULT 0.00 COMMENT '优先级评分',
  `reason` varchar(200) DEFAULT NULL COMMENT '调拨原因',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态:PENDING-待审批,APPROVED-已批准,REJECTED-已拒绝,EXECUTED-已执行',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `transfer_id` bigint DEFAULT NULL COMMENT '关联的调拨单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_library` (`from_library_id`),
  KEY `idx_to_library` (`to_library_id`),
  KEY `idx_biblio` (`biblio_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调拨建议表';

-- 2. 调拨表补充字段
ALTER TABLE `tb_book_transfer` 
ADD COLUMN `estimated_arrival_time` datetime DEFAULT NULL COMMENT '预计到达时间' AFTER `complete_time`,
ADD COLUMN `actual_arrival_time` datetime DEFAULT NULL COMMENT '实际到达时间' AFTER `estimated_arrival_time`,
ADD COLUMN `transfer_reason` varchar(50) DEFAULT 'USER_REQUEST' COMMENT '调拨原因:USER_REQUEST-用户请求,INVENTORY_BALANCE-库存平衡' AFTER `actual_arrival_time`,
ADD COLUMN `suggestion_id` bigint DEFAULT NULL COMMENT '关联的调拨建议ID' AFTER `transfer_reason`;

-- 3. 添加索引
ALTER TABLE `tb_book_transfer` 
ADD INDEX `idx_transfer_reason` (`transfer_reason`),
ADD INDEX `idx_suggestion` (`suggestion_id`);

-- 4. 插入测试数据（可选）
INSERT INTO `tb_transfer_suggestion` VALUES
(1, 2, 1, 1, 2, 85.50, '理科馆需求旺盛，文科馆库存充足', 'PENDING', NULL, NULL, NULL, NOW(), NOW());
