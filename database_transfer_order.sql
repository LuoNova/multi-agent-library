-- ========================================
-- 调拨单表 - 支持批量调拨
-- ========================================

-- 1. 创建调拨单表
CREATE TABLE IF NOT EXISTS `tb_transfer_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `suggestion_id` bigint NOT NULL COMMENT '关联的调拨建议ID',
  `from_library_id` bigint NOT NULL COMMENT '源馆ID',
  `to_library_id` bigint NOT NULL COMMENT '目标馆ID',
  `biblio_id` bigint NOT NULL COMMENT '书目ID',
  `planned_quantity` int NOT NULL COMMENT '计划调拨数量',
  `actual_quantity` int DEFAULT 0 COMMENT '实际调拨数量',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态:PENDING-待执行,IN_PROGRESS-执行中,COMPLETED-已完成,CANCELED-已取消',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_suggestion` (`suggestion_id`),
  KEY `idx_from_library` (`from_library_id`),
  KEY `idx_to_library` (`to_library_id`),
  KEY `idx_biblio` (`biblio_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调拨单表';

-- 2. 修改调拨建议表，将transfer_id改为order_id
ALTER TABLE `tb_transfer_suggestion`
DROP COLUMN `transfer_id`,
ADD COLUMN `order_id` bigint DEFAULT NULL COMMENT '关联的调拨单ID' AFTER `approve_time`;

-- 3. 修改调拨记录表，添加order_id字段
ALTER TABLE `tb_book_transfer`
ADD COLUMN `order_id` bigint DEFAULT NULL COMMENT '关联的调拨单ID' AFTER `suggestion_id`;

-- 4. 添加索引
ALTER TABLE `tb_book_transfer`
ADD INDEX `idx_order` (`order_id`);
