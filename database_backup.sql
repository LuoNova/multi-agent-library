-- MySQL dump 10.13  Distrib 9.1.0, for Win64 (x86_64)
--
-- Host: localhost    Database: library_db
-- ------------------------------------------------------
-- Server version	9.1.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `tb_book_biblio`
--

DROP TABLE IF EXISTS `tb_book_biblio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_book_biblio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `isbn` varchar(20) DEFAULT NULL COMMENT 'ISBN',
  `title` varchar(200) NOT NULL COMMENT '书名',
  `author` varchar(100) DEFAULT NULL COMMENT '作者',
  `publisher` varchar(100) DEFAULT NULL COMMENT '出版社',
  `publish_date` date DEFAULT NULL COMMENT '出版日期',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `total_borrow_count` int DEFAULT '0' COMMENT '累计总借阅次数',
  `monthly_borrow_count` int DEFAULT '0' COMMENT '近30天借阅次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_isbn` (`isbn`),
  KEY `idx_category` (`category`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书目信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_book_biblio`
--

LOCK TABLES `tb_book_biblio` WRITE;
/*!40000 ALTER TABLE `tb_book_biblio` DISABLE KEYS */;
INSERT INTO `tb_book_biblio` VALUES (1,'978-7-111-1','Java编程思想','Bruce Eckel','机械工业出版社',NULL,'计算机',100,20,'2026-02-25 14:15:01');
/*!40000 ALTER TABLE `tb_book_biblio` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_book_borrow`
--

DROP TABLE IF EXISTS `tb_book_borrow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_book_borrow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `copy_id` bigint NOT NULL COMMENT '副本ID',
  `user_id` bigint NOT NULL COMMENT '借阅人ID',
  `borrow_time` datetime NOT NULL COMMENT '借出时间',
  `due_time` datetime NOT NULL COMMENT '应还时间',
  `return_time` datetime DEFAULT NULL COMMENT '实际归还时间',
  `status` varchar(20) DEFAULT 'BORROWING' COMMENT '记录状态:BORROWING/RETURNED/RESERVED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_copy` (`copy_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_borrow_copy` FOREIGN KEY (`copy_id`) REFERENCES `tb_book_copy` (`id`),
  CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='借阅记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_book_borrow`
--

LOCK TABLES `tb_book_borrow` WRITE;
/*!40000 ALTER TABLE `tb_book_borrow` DISABLE KEYS */;
INSERT INTO `tb_book_borrow` VALUES (23,1001,1,'2026-02-26 22:30:41','2026-02-27 22:31:03',NULL,'RESERVED','2026-02-26 22:30:40');
/*!40000 ALTER TABLE `tb_book_borrow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_book_copy`
--

DROP TABLE IF EXISTS `tb_book_copy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_book_copy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID(图书条码号)',
  `biblio_id` bigint NOT NULL COMMENT '书目ID',
  `library_id` bigint NOT NULL COMMENT '当前所在馆ID',
  `location` varchar(100) DEFAULT NULL COMMENT '精确位置描述',
  `status` varchar(20) DEFAULT 'AVAILABLE' COMMENT '''状态: AVAILABLE-可借, BORROWED-已借出, RESERVED-已预留(到馆待取), IN_TRANSIT-调拨运输中, LOST-丢失, DAMAGED-损坏''',
  `local_borrow_count` int DEFAULT '0' COMMENT '在本馆累计被借次数',
  `last_borrow_time` datetime DEFAULT NULL COMMENT '上次被借时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_biblio` (`biblio_id`),
  KEY `idx_library` (`library_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_copy_biblio` FOREIGN KEY (`biblio_id`) REFERENCES `tb_book_biblio` (`id`),
  CONSTRAINT `fk_copy_library` FOREIGN KEY (`library_id`) REFERENCES `tb_library` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图书副本表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_book_copy`
--

LOCK TABLES `tb_book_copy` WRITE;
/*!40000 ALTER TABLE `tb_book_copy` DISABLE KEYS */;
INSERT INTO `tb_book_copy` VALUES (1001,1,1,'馆1-调拨暂存区','RESERVED',0,NULL,'2026-02-25 14:15:01','2026-02-26 22:31:03'),(1002,1,2,'2楼B区06架','AVAILABLE',0,NULL,'2026-02-25 14:15:01','2026-02-26 22:23:39');
/*!40000 ALTER TABLE `tb_book_copy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_book_reservation`
--

DROP TABLE IF EXISTS `tb_book_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_book_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biblio_id` bigint NOT NULL COMMENT '书目ID',
  `user_id` bigint NOT NULL COMMENT '预约人ID',
  `pickup_library_id` bigint NOT NULL COMMENT '期望取书馆ID',
  `copy_id` bigint DEFAULT NULL COMMENT '实际分配的副本ID',
  `reserve_time` datetime NOT NULL COMMENT '预约时间',
  `expire_time` datetime NOT NULL COMMENT '预约过期时间',
  `fulfill_time` datetime DEFAULT NULL COMMENT '实际分配副本时间',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '预约状态:PENDING/FULFILLED/CANCELED/EXPIRED',
  `notification_sent` tinyint DEFAULT '0' COMMENT '是否已发送取书通知(0-否,1-是)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_biblio` (`biblio_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_copy` (`copy_id`),
  KEY `idx_status` (`status`),
  KEY `fk_resv_library` (`pickup_library_id`),
  CONSTRAINT `fk_resv_biblio` FOREIGN KEY (`biblio_id`) REFERENCES `tb_book_biblio` (`id`),
  CONSTRAINT `fk_resv_copy` FOREIGN KEY (`copy_id`) REFERENCES `tb_book_copy` (`id`),
  CONSTRAINT `fk_resv_library` FOREIGN KEY (`pickup_library_id`) REFERENCES `tb_library` (`id`),
  CONSTRAINT `fk_resv_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图书预约表，全馆无库存时用户进入预约排队状态';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_book_reservation`
--

LOCK TABLES `tb_book_reservation` WRITE;
/*!40000 ALTER TABLE `tb_book_reservation` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_book_reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_book_transfer`
--

DROP TABLE IF EXISTS `tb_book_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_book_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(50) DEFAULT NULL COMMENT '关联的协商任务ID',
  `copy_id` bigint NOT NULL COMMENT '调拨副本ID',
  `from_library_id` bigint NOT NULL COMMENT '源馆ID',
  `to_library_id` bigint NOT NULL COMMENT '目标馆ID',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '调拨状态:PENDING/IN_TRANSIT/COMPLETED/CANCELED',
  `request_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  PRIMARY KEY (`id`),
  KEY `idx_copy` (`copy_id`),
  KEY `idx_from_lib` (`from_library_id`),
  KEY `idx_to_lib` (`to_library_id`),
  KEY `idx_request_id` (`request_id`),
  CONSTRAINT `fk_trans_copy` FOREIGN KEY (`copy_id`) REFERENCES `tb_book_copy` (`id`),
  CONSTRAINT `fk_trans_from` FOREIGN KEY (`from_library_id`) REFERENCES `tb_library` (`id`),
  CONSTRAINT `fk_trans_to` FOREIGN KEY (`to_library_id`) REFERENCES `tb_library` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图书调拨记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_book_transfer`
--

LOCK TABLES `tb_book_transfer` WRITE;
/*!40000 ALTER TABLE `tb_book_transfer` DISABLE KEYS */;
INSERT INTO `tb_book_transfer` VALUES (19,NULL,1001,2,1,'COMPLETED','2026-02-26 22:30:41','2026-02-26 14:02:51',NULL);
/*!40000 ALTER TABLE `tb_book_transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_equipment`
--

DROP TABLE IF EXISTS `tb_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NOT NULL COMMENT '所在馆ID',
  `type` varchar(50) NOT NULL COMMENT '设备类型(自助借还机/书架/电脑/空调等)',
  `name` varchar(100) NOT NULL COMMENT '设备名称/编号',
  `location` varchar(200) DEFAULT NULL COMMENT '具体位置描述',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT '状态:NORMAL/FAULT/MAINTAIN/DISABLED',
  `install_time` datetime DEFAULT NULL COMMENT '安装时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_library` (`library_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_equip_library` FOREIGN KEY (`library_id`) REFERENCES `tb_library` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_equipment`
--

LOCK TABLES `tb_equipment` WRITE;
/*!40000 ALTER TABLE `tb_equipment` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_equipment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_library`
--

DROP TABLE IF EXISTS `tb_library`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_library` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '馆名',
  `location_desc` varchar(200) DEFAULT NULL COMMENT '地理位置描述',
  `open_time` varchar(50) DEFAULT NULL COMMENT '开放时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='馆信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_library`
--

LOCK TABLES `tb_library` WRITE;
/*!40000 ALTER TABLE `tb_library` DISABLE KEYS */;
INSERT INTO `tb_library` VALUES (1,'理科馆','校园西区理科楼','08:00-22:00','2026-02-25 14:15:01'),(2,'文科馆','校园东区人文楼','08:00-22:00','2026-02-25 14:15:01');
/*!40000 ALTER TABLE `tb_library` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_library_biblio_stats`
--

DROP TABLE IF EXISTS `tb_library_biblio_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_library_biblio_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NOT NULL COMMENT '馆ID',
  `biblio_id` bigint NOT NULL COMMENT '书目ID',
  `stock_count` int DEFAULT '0' COMMENT '当前库存副本数',
  `borrow_count_30d` int DEFAULT '0' COMMENT '近30天借出次数',
  `reservation_pending_count` int DEFAULT '0' COMMENT '当前排队预约人数',
  `avg_borrow_duration` decimal(5,2) DEFAULT '0.00' COMMENT '平均借阅天数',
  `last_calculated_time` datetime DEFAULT NULL COMMENT '统计更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lib_biblio` (`library_id`,`biblio_id`),
  KEY `idx_biblio` (`biblio_id`),
  CONSTRAINT `fk_stats_biblio` FOREIGN KEY (`biblio_id`) REFERENCES `tb_book_biblio` (`id`),
  CONSTRAINT `fk_stats_library` FOREIGN KEY (`library_id`) REFERENCES `tb_library` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='馆藏书目统计表(智能体决策用)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_library_biblio_stats`
--

LOCK TABLES `tb_library_biblio_stats` WRITE;
/*!40000 ALTER TABLE `tb_library_biblio_stats` DISABLE KEYS */;
INSERT INTO `tb_library_biblio_stats` VALUES (1,1,1,1,0,0,0.00,'2026-02-26 22:31:03'),(2,2,1,1,5,0,14.50,'2026-02-26 20:59:26');
/*!40000 ALTER TABLE `tb_library_biblio_stats` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_repair_ticket`
--

DROP TABLE IF EXISTS `tb_repair_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_repair_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `equipment_id` bigint NOT NULL COMMENT '设备ID',
  `reporter_id` bigint NOT NULL COMMENT '报修人ID',
  `description` text NOT NULL COMMENT '故障描述',
  `priority` varchar(20) DEFAULT 'MEDIUM' COMMENT '优先级(HIGH/MEDIUM/LOW)',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSING/COMPLETED/CLOSED)',
  `assignee_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `images` varchar(500) DEFAULT NULL COMMENT '故障图片URL',
  `report_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',
  `process_time` datetime DEFAULT NULL COMMENT '开始处理时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `feedback` varchar(500) DEFAULT NULL COMMENT '处理反馈',
  PRIMARY KEY (`id`),
  KEY `idx_equipment` (`equipment_id`),
  KEY `idx_reporter` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  CONSTRAINT `fk_ticket_equip` FOREIGN KEY (`equipment_id`) REFERENCES `tb_equipment` (`id`),
  CONSTRAINT `fk_ticket_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='故障报修表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_repair_ticket`
--

LOCK TABLES `tb_repair_ticket` WRITE;
/*!40000 ALTER TABLE `tb_repair_ticket` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_repair_ticket` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_seat`
--

DROP TABLE IF EXISTS `tb_seat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_seat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `area_id` bigint NOT NULL COMMENT '区域ID',
  `seat_no` varchar(20) NOT NULL COMMENT '座位编号(如:A-01)',
  `has_power` tinyint DEFAULT '0' COMMENT '是否有独立电源(0无；1有）',
  `status` varchar(20) DEFAULT 'AVAILABLE' COMMENT '状态:AVAILABLE/OCCUPIED/DISABLED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_area` (`area_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_seat_area` FOREIGN KEY (`area_id`) REFERENCES `tb_seat_area` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='座位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_seat`
--

LOCK TABLES `tb_seat` WRITE;
/*!40000 ALTER TABLE `tb_seat` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_seat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_seat_area`
--

DROP TABLE IF EXISTS `tb_seat_area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_seat_area` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `library_id` bigint NOT NULL COMMENT '所属馆ID',
  `name` varchar(50) NOT NULL COMMENT '区域名称(如:3楼自习区)',
  `floor` int DEFAULT NULL COMMENT '楼层',
  `seat_count` int DEFAULT '0' COMMENT '座位总数',
  `open_time` varchar(50) DEFAULT NULL COMMENT '开放时间',
  `has_power` tinyint DEFAULT '1' COMMENT '是否有电源(0-无,1-有)',
  `status` varchar(20) DEFAULT 'OPEN' COMMENT '状态:OPEN/CLOSED/MAINTAIN',
  PRIMARY KEY (`id`),
  KEY `idx_library` (`library_id`),
  CONSTRAINT `fk_area_library` FOREIGN KEY (`library_id`) REFERENCES `tb_library` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='座位区域表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_seat_area`
--

LOCK TABLES `tb_seat_area` WRITE;
/*!40000 ALTER TABLE `tb_seat_area` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_seat_area` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_seat_reservation`
--

DROP TABLE IF EXISTS `tb_seat_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_seat_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `seat_id` bigint NOT NULL COMMENT '座位ID',
  `user_id` bigint NOT NULL COMMENT '预约人ID',
  `reservation_date` date NOT NULL COMMENT '预约日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/COMPLETED/CANCELED/NO_SHOW',
  `check_in_time` datetime DEFAULT NULL COMMENT '实际签到时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_seat` (`seat_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_date` (`reservation_date`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_seatresv_seat` FOREIGN KEY (`seat_id`) REFERENCES `tb_seat` (`id`),
  CONSTRAINT `fk_seatresv_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='座位预约表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_seat_reservation`
--

LOCK TABLES `tb_seat_reservation` WRITE;
/*!40000 ALTER TABLE `tb_seat_reservation` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_seat_reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_user`
--

DROP TABLE IF EXISTS `tb_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_no` varchar(20) NOT NULL COMMENT '学号/工号',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `max_borrow_count` int DEFAULT '10' COMMENT '最大可借册数',
  `current_borrow_count` int DEFAULT '0' COMMENT '当前借阅中数量',
  `credit_score` int DEFAULT '100' COMMENT '信用分',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-正常, FROZEN-冻结',
  `preferred_library_id` bigint DEFAULT NULL COMMENT '常用馆ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_preferred_lib` (`preferred_library_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_user`
--

LOCK TABLES `tb_user` WRITE;
/*!40000 ALTER TABLE `tb_user` DISABLE KEYS */;
INSERT INTO `tb_user` VALUES (1,'2024001','测试学生','13800138000',NULL,10,1,100,'ACTIVE',1,'2026-02-25 14:15:01','2026-02-26 22:23:41'),(2,'2024002','还书测试用户',NULL,NULL,10,0,94,'ACTIVE',NULL,'2026-02-26 14:31:19','2026-02-26 21:37:47');
/*!40000 ALTER TABLE `tb_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-27 11:56:15
