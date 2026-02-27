-- ==========================================
-- 通知机制相关数据库表
-- ==========================================

-- 1. 通知记录表
CREATE TABLE IF NOT EXISTS tb_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(20) NOT NULL COMMENT '通知类型: PICKUP_NOTICE-取书通知, RESERVE_EXPIRE_WARNING-预留超期提醒, RESERVE_EXPIRED-预约超期通知, RETURN_REMINDER-还书提醒',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    channel VARCHAR(20) NOT NULL COMMENT '通知渠道: IN_APP-站内信, SMS-短信, EMAIL-邮件',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态: PENDING-待发送, SENDING-发送中, SUCCESS-发送成功, FAILED-发送失败',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(500) COMMENT '错误信息',
    send_time DATETIME COMMENT '发送时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';

-- 2. 通知模板表
CREATE TABLE IF NOT EXISTS tb_notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type VARCHAR(20) NOT NULL COMMENT '通知类型',
    channel VARCHAR(20) NOT NULL COMMENT '通知渠道',
    title_template VARCHAR(100) NOT NULL COMMENT '标题模板',
    content_template TEXT NOT NULL COMMENT '内容模板(支持占位符,如{bookTitle}, {libraryName}等)',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用: 1-启用, 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_channel (type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 3. 插入默认通知模板数据
-- 取书通知模板(站内信)
INSERT INTO tb_notification_template (type, channel, title_template, content_template, is_enabled) VALUES
('PICKUP_NOTICE', 'IN_APP', '图书已到馆,请及时取书', '您预约的《{bookTitle}》已到达{libraryName},请在24小时内到馆取书。取书码:{pickupCode}', 1);

-- 预留超期提醒模板(站内信)
INSERT INTO tb_notification_template (type, channel, title_template, content_template, is_enabled) VALUES
('RESERVE_EXPIRE_WARNING', 'IN_APP', '取书提醒', '您预约的《{bookTitle}》将在{remainingHours}小时后超期,请尽快到馆取书。', 1);

-- 预约超期通知模板(站内信)
INSERT INTO tb_notification_template (type, channel, title_template, content_template, is_enabled) VALUES
('RESERVE_EXPIRED', 'IN_APP', '预约已取消', '您预约的《{bookTitle}》因超期未取已被取消,信用分扣除5分。如需借阅请重新预约。', 1);

-- 还书提醒模板(站内信)
INSERT INTO tb_notification_template (type, channel, title_template, content_template, is_enabled) VALUES
('RETURN_REMINDER', 'IN_APP', '还书提醒', '您借阅的《{bookTitle}》将于{dueDate}到期,请及时归还。', 1);

-- ==========================================
-- 修改借阅记录表,增加预留相关字段
-- ==========================================

-- 检查并添加预留时间字段
ALTER TABLE tb_book_borrow 
ADD COLUMN IF NOT EXISTS reserved_time DATETIME COMMENT '预留开始时间(调拨完成时间)' AFTER borrow_time;

-- 检查并添加取书截止时间字段
ALTER TABLE tb_book_borrow 
ADD COLUMN IF NOT EXISTS pickup_deadline DATETIME COMMENT '取书截止时间(预留开始时间+24小时)' AFTER reserved_time;

-- 检查并添加实际取书时间字段
ALTER TABLE tb_book_borrow 
ADD COLUMN IF NOT EXISTS actual_pickup_time DATETIME COMMENT '实际取书时间' AFTER pickup_deadline;

-- 检查并添加取书馆ID字段
ALTER TABLE tb_book_borrow 
ADD COLUMN IF NOT EXISTS pickup_library_id BIGINT COMMENT '取书馆ID' AFTER actual_pickup_time;

-- 更新借阅状态枚举,增加RESERVED状态
-- 注意: status字段需要支持: RESERVED-已预留, BORROWING-借阅中, RETURNED-已归还, CANCELLED-已取消
