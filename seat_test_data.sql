-- ========= 座位测试数据初始化脚本 =========
-- 注意：执行前请确认没有重要生产数据

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE tb_seat_reservation;
TRUNCATE TABLE tb_seat;
TRUNCATE TABLE tb_seat_area;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================
-- 1. 插入座位区域（假定 library_id=1: 理科馆, library_id=2: 文科馆）
-- =========================================

INSERT INTO tb_seat_area (id, library_id, name, floor, seat_count, open_time, has_power, status) VALUES
  (1, 1, '一楼自习区A', 1, 12, '08:00-22:00', 1, 'OPEN'),
  (2, 1, '三楼自习区A', 3, 12, '08:00-22:00', 1, 'OPEN'),
  (3, 2, '二楼自习区A', 2, 12, '08:00-22:00', 1, 'OPEN');

-- 若你的 tb_library 中 ID 不同，请修改上面的 library_id 再执行。

-- =========================================
-- 2. 插入座位（每个区域若干个座位）
-- =========================================

-- 区域1：理科馆 一楼自习区A（部分有电源，部分无电源）
INSERT INTO tb_seat (id, area_id, seat_no, has_power, status, create_time) VALUES
  (1001, 1, 'A-01', 1, 'AVAILABLE', NOW()),
  (1002, 1, 'A-02', 1, 'AVAILABLE', NOW()),
  (1003, 1, 'A-03', 1, 'AVAILABLE', NOW()),
  (1004, 1, 'A-04', 0, 'AVAILABLE', NOW()),
  (1005, 1, 'A-05', 0, 'AVAILABLE', NOW()),
  (1006, 1, 'A-06', 0, 'AVAILABLE', NOW());

-- 区域2：理科馆 三楼自习区A（假设全部有电源）
INSERT INTO tb_seat (id, area_id, seat_no, has_power, status, create_time) VALUES
  (1101, 2, 'B-01', 1, 'AVAILABLE', NOW()),
  (1102, 2, 'B-02', 1, 'AVAILABLE', NOW()),
  (1103, 2, 'B-03', 1, 'AVAILABLE', NOW()),
  (1104, 2, 'B-04', 1, 'AVAILABLE', NOW()),
  (1105, 2, 'B-05', 1, 'AVAILABLE', NOW()),
  (1106, 2, 'B-06', 1, 'AVAILABLE', NOW());

-- 区域3：文科馆 二楼自习区A（电源较少）
INSERT INTO tb_seat (id, area_id, seat_no, has_power, status, create_time) VALUES
  (1201, 3, 'C-01', 1, 'AVAILABLE', NOW()),
  (1202, 3, 'C-02', 0, 'AVAILABLE', NOW()),
  (1203, 3, 'C-03', 0, 'AVAILABLE', NOW()),
  (1204, 3, 'C-04', 0, 'AVAILABLE', NOW()),
  (1205, 3, 'C-05', 0, 'AVAILABLE', NOW()),
  (1206, 3, 'C-06', 0, 'AVAILABLE', NOW());

-- =========================================
-- 3. 可选：插入少量预约记录，方便验证“时间冲突过滤”逻辑
--    如果你希望从全空开始测试，可以不执行本小节
-- =========================================

-- 示例：用户1在理科馆，一楼 A-01 座位，明天 09:00-11:00 已有预约
-- 注意：这里假定 tb_user 中已有 id=1 用户，tb_book_borrow 中有 id=1 的借阅记录可供关联
-- 如没有，可将 borrow_id 设为 NULL 或改为你实际存在的 ID

INSERT INTO tb_seat_reservation (
  seat_id, user_id, reservation_date,
  start_time, end_time,
  status, check_in_time, create_time,
  library_id, borrow_id, source
) VALUES
  (1001, 1, DATE(NOW()) + INTERVAL 1 DAY,
   '09:00:00', '11:00:00',
   'ACTIVE', NULL, NOW(),
   1, NULL, 'WALK_IN');

-- 另一个例子：同一座位当天晚上的已完成预约，用于测试过滤逻辑仅看 ACTIVE
INSERT INTO tb_seat_reservation (
  seat_id, user_id, reservation_date,
  start_time, end_time,
  status, check_in_time, create_time,
  library_id, borrow_id, source
) VALUES
  (1001, 1, DATE(NOW()) + INTERVAL 1 DAY,
   '18:00:00', '20:00:00',
   'COMPLETED', NOW(), NOW(),
   1, NULL, 'WALK_IN');