-- ========================================
-- 多智能体图书馆管理系统 - 测试数据脚本
-- ========================================
-- 说明：此脚本用于重置数据库，准备测试所需的数据
-- 包含：用户、图书馆、图书目录、图书副本等基础数据
-- 用途：支持Swagger测试和功能验证
-- ========================================

-- 临时禁用外键约束
SET FOREIGN_KEY_CHECKS = 0;

-- 清理现有数据（按依赖关系倒序删除）
DELETE FROM tb_book_borrow;
DELETE FROM tb_book_reservation;
DELETE FROM tb_book_transfer;
DELETE FROM tb_book_copy;
DELETE FROM tb_library_biblio_stats;
DELETE FROM tb_book_biblio;
DELETE FROM tb_user;
DELETE FROM tb_library;

-- 重新启用外键约束
SET FOREIGN_KEY_CHECKS = 1;

-- 重置自增ID
ALTER TABLE tb_book_borrow AUTO_INCREMENT = 1;
ALTER TABLE tb_book_reservation AUTO_INCREMENT = 1;
ALTER TABLE tb_book_transfer AUTO_INCREMENT = 1;
ALTER TABLE tb_book_copy AUTO_INCREMENT = 1;
ALTER TABLE tb_book_biblio AUTO_INCREMENT = 1;
ALTER TABLE tb_library_biblio_stats AUTO_INCREMENT = 1;
ALTER TABLE tb_user AUTO_INCREMENT = 1;
ALTER TABLE tb_library AUTO_INCREMENT = 1;

-- ========================================
-- 1. 插入图书馆数据
-- ========================================
INSERT INTO tb_library (id, name, location_desc, open_time, create_time) VALUES
(1, '理科馆', '校园北区', '08:00-22:00', NOW()),
(2, '文科馆', '校园南区', '08:00-22:00', NOW()),
(3, '工程馆', '校园东区', '09:00-21:00', NOW());

-- ========================================
-- 2. 插入用户数据
-- ========================================
INSERT INTO tb_user (id, student_no, name, phone, email, max_borrow_count, current_borrow_count, credit_score, status, preferred_library_id, create_time, update_time) VALUES
-- 正常用户（有借阅额度）
(1, '2021001', '张三', '13800138001', 'zhangsan@example.com', 10, 0, 100, 'ACTIVE', 1, NOW(), NOW()),
(2, '2021002', '李四', '13800138002', 'lisi@example.com', 10, 0, 100, 'ACTIVE', 2, NOW(), NOW()),
(3, '2021003', '王五', '13800138003', 'wangwu@example.com', 10, 0, 100, 'ACTIVE', 1, NOW(), NOW()),
-- 冻结用户（无借阅额度）
(4, '2021004', '赵六', '13800138004', 'zhaoliu@example.com', 10, 0, 50, 'FROZEN', 1, NOW(), NOW());

-- ========================================
-- 3. 插入图书目录数据
-- ========================================
INSERT INTO tb_book_biblio (id, isbn, title, author, publisher, publish_date, category, total_borrow_count, monthly_borrow_count, create_time) VALUES
-- 计算机类书籍
(1, '9787111213826', 'Java编程思想', 'Bruce Eckel', '机械工业出版社', '2020-01-01', '计算机', 0, 0, NOW()),
(2, '9787111544937', '深入理解Java虚拟机', '周志明', '机械工业出版社', '2019-01-01', '计算机', 0, 0, NOW()),
(3, '9787115428028', '算法导论', 'Thomas H. Cormen', '人民邮电出版社', '2018-01-01', '计算机', 0, 0, NOW()),
-- 数学类书籍
(4, '9787040205497', '高等数学', '同济大学数学系', '高等教育出版社', '2019-01-01', '数学', 0, 0, NOW()),
(5, '9787040413640', '线性代数', '同济大学数学系', '高等教育出版社', '2020-01-01', '数学', 0, 0, NOW()),
-- 物理类书籍
(6, '9787040264799', '大学物理', '程守洙', '高等教育出版社', '2018-01-01', '物理', 0, 0, NOW()),
-- 文学类书籍
(7, '9787020002207', '红楼梦', '曹雪芹', '人民文学出版社', '1996-01-01', '文学', 0, 0, NOW()),
(8, '9787020008735', '西游记', '吴承恩', '人民文学出版社', '2008-01-01', '文学', 0, 0, NOW()),
-- 历史类书籍
(9, '9787101003048', '史记', '司马迁', '中华书局', '2013-01-01', '历史', 0, 0, NOW()),
(10, '9787101005219', '资治通鉴', '司马光', '中华书局', '2015-01-01', '历史', 0, 0, NOW());

-- ========================================
-- 4. 插入馆藏统计数据
-- ========================================
INSERT INTO tb_library_biblio_stats (library_id, biblio_id, stock_count, borrow_count_30d, reservation_pending_count, avg_borrow_duration, last_calculated_time) VALUES
-- 理科馆（library_id=1）
(1, 1, 3, 0, 0, 0.00, NOW()),  -- Java编程思想
(1, 2, 2, 0, 0, 0.00, NOW()),  -- 深入理解Java虚拟机
(1, 3, 2, 0, 0, 0.00, NOW()),  -- 算法导论
(1, 4, 5, 0, 0, 0.00, NOW()),  -- 高等数学
(1, 5, 4, 0, 0, 0.00, NOW()),  -- 线性代数
(1, 6, 3, 0, 0, 0.00, NOW()),  -- 大学物理
-- 文科馆（library_id=2）
(2, 7, 4, 0, 0, 0.00, NOW()),  -- 红楼梦
(2, 8, 3, 0, 0, 0.00, NOW()),  -- 西游记
(2, 9, 2, 0, 0, 0.00, NOW()),  -- 史记
(2, 10, 2, 0, 0, 0.00, NOW()), -- 资治通鉴
-- 工程馆（library_id=3）
(3, 1, 2, 0, 0, 0.00, NOW()),  -- Java编程思想
(3, 2, 2, 0, 0, 0.00, NOW()),  -- 深入理解Java虚拟机
(3, 7, 2, 0, 0, 0.00, NOW());  -- 红楼梦

-- ========================================
-- 5. 插入图书副本数据
-- ========================================
INSERT INTO tb_book_copy (id, biblio_id, library_id, location, status, local_borrow_count, last_borrow_time, create_time, update_time) VALUES
-- 理科馆副本（library_id=1）
(1, 1, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(2, 1, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(3, 1, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(4, 2, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(5, 2, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(6, 3, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(7, 3, 1, 'A区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(8, 4, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(9, 4, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(10, 4, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(11, 4, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(12, 4, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(13, 5, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(14, 5, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(15, 5, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(16, 5, 1, 'B区-2楼-数学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(17, 6, 1, 'C区-2楼-物理', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(18, 6, 1, 'C区-2楼-物理', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(19, 6, 1, 'C区-2楼-物理', 'AVAILABLE', 0, NULL, NOW(), NOW()),
-- 文科馆副本（library_id=2）
(20, 7, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(21, 7, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(22, 7, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(23, 7, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(24, 8, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(25, 8, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(26, 8, 2, 'D区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(27, 9, 2, 'E区-2楼-历史', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(28, 9, 2, 'E区-2楼-历史', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(29, 10, 2, 'E区-2楼-历史', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(30, 10, 2, 'E区-2楼-历史', 'AVAILABLE', 0, NULL, NOW(), NOW()),
-- 工程馆副本（library_id=3）
(31, 1, 3, 'F区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(32, 1, 3, 'F区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(33, 2, 3, 'F区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(34, 2, 3, 'F区-3楼-计算机', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(35, 7, 3, 'G区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW()),
(36, 7, 3, 'G区-3楼-文学', 'AVAILABLE', 0, NULL, NOW(), NOW());

-- ========================================
-- 测试场景说明
-- ========================================
-- 场景1：本地借阅测试
--   用户1（张三）在理科馆借阅《Java编程思想》（biblio_id=1）
--   预期结果：直接借阅成功，无需调拨

-- 场景2：跨馆调拨测试
--   用户1（张三）在理科馆借阅《红楼梦》（biblio_id=7）
--   预期结果：触发跨馆调拨，从文科馆调拨到理科馆

-- 场景3：预约排队测试
--   用户1、2、3同时借阅同一本稀缺书籍（如《资治通鉴》biblio_id=10）
--   预期结果：第一个借阅成功，其他人进入预约队列

-- 场景4：还书测试
--   用户1归还已借阅的书籍
--   预期结果：检查预约队列，触发调拨或预留

-- 场景5：并发测试
--   多个用户同时借阅同一本书
--   预期结果：只有一个用户借阅成功，其他用户进入预约队列

-- 场景6：冻结用户测试
--   用户4（赵六）尝试借阅书籍
--   预期结果：借阅失败，提示用户已被冻结

-- ========================================
-- 数据统计
-- ========================================
-- 图书馆数量：3个（理科馆、文科馆、工程馆）
-- 用户数量：4个（3个正常用户，1个冻结用户）
-- 图书目录数量：10本（涵盖计算机、数学、物理、文学、历史）
-- 图书副本数量：36本
-- 总馆藏量：36本
-- 可用数量：36本

-- ========================================
-- 使用说明
-- ========================================
-- 1. 确保数据库已创建：CREATE DATABASE library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 2. 执行此脚本：mysql -u root -p1234 library_db < test_data.sql
-- 3. 启动应用，访问Swagger UI：http://localhost:8080/swagger-ui.html
-- 4. 按照测试场景进行功能测试

-- ========================================
-- 重置数据库命令
-- ========================================
-- mysql -u root -p1234 library_db < test_data.sql
