-- ========================================
-- 为tb_book_transfer表添加PhaseA所需字段
-- ========================================
-- 说明：此脚本用于添加调拨时间字段和接收用户ID字段
-- 用途：支持PhaseA功能测试
-- ========================================

-- 添加预计到达时间字段
ALTER TABLE tb_book_transfer
ADD COLUMN estimated_arrival_time DATETIME NULL COMMENT '预计到达时间';

-- 添加实际到达时间字段
ALTER TABLE tb_book_transfer
ADD COLUMN actual_arrival_time DATETIME NULL COMMENT '实际到达时间';

-- 添加接收用户ID字段
ALTER TABLE tb_book_transfer
ADD COLUMN receiver_user_id BIGINT NULL COMMENT '接收用户ID(用户请求/预约兑现时填写,库存平衡为空)';

-- 添加调拨原因字段
ALTER TABLE tb_book_transfer
ADD COLUMN transfer_reason VARCHAR(50) NULL COMMENT '调拨原因:USER_REQUEST-用户请求,INVENTORY_BALANCE-库存平衡';

-- 添加关联的调拨建议ID字段
ALTER TABLE tb_book_transfer
ADD COLUMN suggestion_id BIGINT NULL COMMENT '关联的调拨建议ID';

-- 添加关联的调拨单ID字段
ALTER TABLE tb_book_transfer
ADD COLUMN order_id BIGINT NULL COMMENT '关联的调拨单ID';

-- 创建索引以提升查询性能
CREATE INDEX idx_transfer_receiver_status_time
ON tb_book_transfer(receiver_user_id, status, request_time);

-- 验证字段是否添加成功
SHOW COLUMNS FROM tb_book_transfer LIKE 'estimated_arrival_time';
SHOW COLUMNS FROM tb_book_transfer LIKE 'actual_arrival_time';
SHOW COLUMNS FROM tb_book_transfer LIKE 'receiver_user_id';
SHOW COLUMNS FROM tb_book_transfer LIKE 'transfer_reason';
SHOW COLUMNS FROM tb_book_transfer LIKE 'suggestion_id';
SHOW COLUMNS FROM tb_book_transfer LIKE 'order_id';
