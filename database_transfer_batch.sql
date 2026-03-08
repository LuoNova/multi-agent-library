-- ========================================
-- 批量调拨功能 - 完整数据库配置
-- ========================================
-- 说明：本SQL文件基于现有表结构进行修改，支持批量调拨功能
-- 执行前请备份数据库！

-- ========================================
-- 第一部分：表结构修改
-- ========================================

-- 1. 创建调拨单表（新增）
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

-- 2. 创建调拨建议表（新增）
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
  `order_id` bigint DEFAULT NULL COMMENT '关联的调拨单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_library` (`from_library_id`),
  KEY `idx_to_library` (`to_library_id`),
  KEY `idx_biblio` (`biblio_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调拨建议表';

-- 3. 修改调拨记录表，添加新字段
-- 注意：如果字段已存在，会报错，可以忽略或先检查
ALTER TABLE `tb_book_transfer`
ADD COLUMN  `order_id` bigint DEFAULT NULL COMMENT '关联的调拨单ID' AFTER `suggestion_id`;

-- 4. 添加索引（如果不存在）
-- MySQL不支持IF NOT EXISTS语法，如果索引已存在会报错，可以忽略
ALTER TABLE `tb_book_transfer`
ADD INDEX `idx_transfer_reason` (`transfer_reason`),
ADD INDEX `idx_suggestion` (`suggestion_id`),
ADD INDEX `idx_order` (`order_id`);

-- ========================================
-- 第二部分：测试数据准备
-- ========================================

-- 场景：从文科馆(馆2)调拨5本《Java编程思想》到理科馆(馆1)
-- 前提条件：
-- 1. 理科馆(馆1)库存不足（< 2本），需求旺盛（近30天借出 > 20次）
-- 2. 文科馆(馆2)库存充足（> 5本），利用率低（近30天借出 < 10次）

-- 1. 插入调拨建议（建议调拨5本）
-- 注意：ID为自增主键，插入时不需要指定id字段
INSERT INTO `tb_transfer_suggestion`
(`from_library_id`, `to_library_id`, `biblio_id`, `suggested_quantity`, `priority_score`, `reason`, `status`, `approver_id`, `approve_time`, `order_id`, `create_time`, `update_time`)
VALUES
(2, 1, 1, 5, 85.50, '理科馆需求旺盛（库存1，近30天借出25次），文科馆库存充足（库存10，近30天借出5次）', 'PENDING', NULL, NULL, NULL, NOW(), NOW());

-- 2. 模拟理科馆(馆1)的库存统计（库存不足，需求旺盛）
INSERT INTO `tb_library_biblio_stats`
(`library_id`, `biblio_id`, `stock_count`, `borrow_count_30d`, `reservation_pending_count`, `last_calculated_time`)
VALUES
(1, 1, 1, 25, 5, NOW())  -- 理科馆：库存1本，近30天借出25次，预约排队5人
ON DUPLICATE KEY UPDATE
`stock_count` = 1,
`borrow_count_30d` = 25,
`reservation_pending_count` = 5,
`last_calculated_time` = NOW();

-- 3. 模拟文科馆(馆2)的库存统计（库存充足，利用率低）
INSERT INTO `tb_library_biblio_stats`
(`library_id`, `biblio_id`, `stock_count`, `borrow_count_30d`, `reservation_pending_count`, `last_calculated_time`)
VALUES
(2, 1, 10, 5, 0, NOW())  -- 文科馆：库存10本，近30天借出5次，无预约排队
ON DUPLICATE KEY UPDATE
`stock_count` = 10,
`borrow_count_30d` = 5,
`reservation_pending_count` = 0,
`last_calculated_time` = NOW();

-- 4. 确保文科馆有10本可用的副本（如果副本不足，需要插入）
-- 注意：ID为自增主键，插入时不需要指定id字段
-- 先查询当前文科馆(biblio_id=1, library_id=2)的可用副本数量
-- 如果不足10本，执行以下插入语句（根据实际需要插入的数量调整）

-- 示例：插入9本可用副本（假设已有1本，共10本）
INSERT INTO `tb_book_copy` (`biblio_id`, `library_id`, `status`, `location`, `local_borrow_count`, `create_time`, `update_time`)
VALUES
(1, 2, 'AVAILABLE', '文科馆-书架A-1', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-2', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-3', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-4', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-5', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-6', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-7', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-8', 0, NOW(), NOW()),
(1, 2, 'AVAILABLE', '文科馆-书架A-9', 0, NOW(), NOW());

-- 注意：如果数据库中已有部分副本，请根据实际情况调整插入数量
-- 可以先执行以下查询确认当前副本数量：
-- SELECT COUNT(*) FROM tb_book_copy WHERE biblio_id = 1 AND library_id = 2 AND status = 'AVAILABLE';

-- ========================================
-- 第三部分：测试步骤说明
-- ========================================
-- 1. 执行上述SQL创建表结构和测试数据
-- 2. 启动应用，调用 GET /api/transfer/suggestion/list 查看调拨建议列表
--    注意：记录返回的调拨建议ID（假设为{suggestion_id}）
-- 3. 调用 POST /api/transfer/suggestion/approve/{suggestion_id}?approverId=1 审批通过调拨建议
--    注意：将{suggestion_id}替换为实际的调拨建议ID
-- 4. 查看数据库验证：
--    - tb_transfer_order表应该有1条记录，planned_quantity=5, actual_quantity=5
--    - tb_book_transfer表应该有5条记录，每条记录的order_id都相同
--    - tb_book_copy表应该有5个副本状态变为IN_TRANSIT
--    - tb_library_biblio_stats表中文科馆(馆2)的库存应该减少5本
-- 5. 调用定时任务或手动完成调拨，验证目标馆库存增加

-- ========================================
-- 第四部分：辅助查询语句
-- ========================================
-- 查询最新的调拨建议ID
-- SELECT id FROM tb_transfer_suggestion ORDER BY create_time DESC LIMIT 1;

-- 查询文科馆当前可用副本数量
-- SELECT COUNT(*) FROM tb_book_copy WHERE biblio_id = 1 AND library_id = 2 AND status = 'AVAILABLE';

-- 查询调拨单执行情况
-- SELECT * FROM tb_transfer_order ORDER BY create_time DESC LIMIT 1;

-- 查询调拨记录
-- SELECT * FROM tb_book_transfer WHERE order_id = (SELECT id FROM tb_transfer_order ORDER BY create_time DESC LIMIT 1);

-- ========================================
-- 第五部分：清理测试数据（可选）
-- ========================================
-- 如果需要重新测试，可以执行以下清理语句：
-- DELETE FROM tb_book_transfer WHERE transfer_reason = 'INVENTORY_BALANCE';
-- DELETE FROM tb_transfer_order;
-- DELETE FROM tb_transfer_suggestion;
-- UPDATE tb_book_copy SET status = 'AVAILABLE' WHERE biblio_id = 1 AND library_id = 2;
-- UPDATE tb_library_biblio_stats SET stock_count = 10 WHERE library_id = 2 AND biblio_id = 1;
