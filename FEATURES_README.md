# 图书馆管理系统 - 新功能实现说明

## 功能概述

本次实现了三个核心功能模块:

1. **通知机制** - 基于RabbitMQ的消息通知系统
2. **取书确认接口** - 用户到馆取书确认功能
3. **超期释放定时任务** - 自动处理超期未取的预留记录

---

## 一、通知机制

### 1.1 技术架构

- **消息队列**: RabbitMQ
- **通知渠道**: 站内信(已实现), 短信(TODO)
- **重试策略**: 重试3次,间隔1分钟
- **模板管理**: 支持动态配置通知模板

### 1.2 数据库表

#### tb_notification (通知记录表)
```sql
CREATE TABLE tb_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(20) NOT NULL COMMENT '通知类型',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    channel VARCHAR(20) NOT NULL COMMENT '通知渠道',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(500) COMMENT '错误信息',
    send_time DATETIME COMMENT '发送时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### tb_notification_template (通知模板表)
```sql
CREATE TABLE tb_notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL COMMENT '通知类型',
    channel VARCHAR(20) NOT NULL COMMENT '通知渠道',
    title_template VARCHAR(100) NOT NULL COMMENT '标题模板',
    content_template TEXT NOT NULL COMMENT '内容模板',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_channel (type, channel)
);
```

### 1.3 通知类型

| 类型 | 说明 | 模板示例 |
|------|------|----------|
| PICKUP_NOTICE | 取书通知 | 您预约的《{bookTitle}》已到达{libraryName},请在24小时内到馆取书。取书码:{pickupCode} |
| RESERVE_EXPIRE_WARNING | 预留超期提醒 | 您预约的《{bookTitle}》将在{remainingHours}小时后超期,请尽快到馆取书。 |
| RESERVE_EXPIRED | 预约超期通知 | 您预约的《{bookTitle}》因超期未取已被取消,信用分扣除5分。如需借阅请重新预约。 |
| RETURN_REMINDER | 还书提醒 | 您借阅的《{bookTitle}》将于{dueDate}到期,请及时归还。 |

### 1.4 API接口

#### 获取所有模板
```
GET /api/notification/template/list
```

#### 创建模板
```
POST /api/notification/template/create
Content-Type: application/json

{
  "type": "PICKUP_NOTICE",
  "channel": "IN_APP",
  "titleTemplate": "图书已到馆,请及时取书",
  "contentTemplate": "您预约的《{bookTitle}》已到达{libraryName},请在24小时内到馆取书。",
  "isEnabled": 1
}
```

#### 更新模板
```
PUT /api/notification/template/update
Content-Type: application/json

{
  "id": 1,
  "titleTemplate": "新的标题模板",
  "contentTemplate": "新的内容模板"
}
```

#### 启用/禁用模板
```
PUT /api/notification/template/toggle/{templateId}?enabled=true
```

---

## 二、取书确认接口

### 2.1 功能说明

用户到馆后确认取书,完成借阅流程。

**核心规则**:
- ✅ 仅限预约人本人取书
- ✅ 必须在24小时预留期内取书
- ✅ 超期不允许补取,必须重新预约
- ✅ 不记录取书方式

### 2.2 业务流程

```
用户到馆 → 扫码/输入取书码 → 验证身份和预留状态 → 确认取书 → 状态变更 → 开始借阅计时
```

### 2.3 API接口

#### 确认取书
```
POST /api/borrow/confirm-pickup
Content-Type: application/json

{
  "borrowId": 1,
  "userId": 1
}
```

**响应示例**:
```json
{
  "borrowId": 1,
  "bookTitle": "Java编程思想",
  "borrowDate": "2026-02-27T10:30:00",
  "dueDate": "2026-03-29T10:30:00",
  "libraryName": "理科馆"
}
```

### 2.4 状态变更

- **借阅记录**: RESERVED → BORROWING
- **图书副本**: RESERVED → BORROWED
- **借阅期限**: 从取书确认时间开始计算(默认30天)

---

## 三、超期释放定时任务

### 3.1 功能说明

自动处理超期未取的预留记录,释放资源。

**核心规则**:
- ✅ 监控预留超期(24小时未取)
- ✅ 信用扣分: 扣5分,无宽限期
- ✅ 不自动重新排队,用户需重新预约
- ✅ 提前提醒: 预留20小时时发送提醒

### 3.2 定时任务

#### 任务1: 超期释放任务
- **执行频率**: 每小时执行一次 (0 0 * * * ?)
- **扫描条件**: status = RESERVED AND pickupDeadline < now
- **执行逻辑**:
  1. 更新借阅状态: RESERVED → CANCELLED
  2. 更新图书状态: RESERVED → AVAILABLE
  3. 扣除用户信用分: -5分
  4. 发送超期通知

#### 任务2: 提前提醒任务
- **执行频率**: 每小时执行一次 (0 0 * * * ?)
- **扫描条件**: status = RESERVED AND pickupDeadline 在 now+4小时 到 now+5小时 之间
- **执行逻辑**: 发送预留超期提醒通知

### 3.3 数据库字段更新

需要在 `tb_book_borrow` 表中添加以下字段:

```sql
ALTER TABLE tb_book_borrow 
ADD COLUMN reserved_time DATETIME COMMENT '预留开始时间(调拨完成时间)';

ALTER TABLE tb_book_borrow 
ADD COLUMN pickup_deadline DATETIME COMMENT '取书截止时间(预留开始时间+24小时)';

ALTER TABLE tb_book_borrow 
ADD COLUMN actual_pickup_time DATETIME COMMENT '实际取书时间';

ALTER TABLE tb_book_borrow 
ADD COLUMN pickup_library_id BIGINT COMMENT '取书馆ID';
```

---

## 四、部署说明

### 4.1 环境要求

- JDK 17+
- MySQL 8.0+
- RabbitMQ 3.8+ (需要安装并启动)

### 4.2 安装RabbitMQ

#### Windows:
1. 下载并安装Erlang: https://www.erlang.org/downloads
2. 下载并安装RabbitMQ: https://www.rabbitmq.com/download.html
3. 启动RabbitMQ服务:
   ```bash
   rabbitmq-server start
   ```
4. 访问管理界面: http://localhost:15672 (默认账号: guest/guest)

#### Linux/Mac:
```bash
# 安装RabbitMQ
brew install rabbitmq  # Mac
sudo apt-get install rabbitmq-server  # Ubuntu

# 启动服务
rabbitmq-server start
```

### 4.3 数据库初始化

执行SQL文件:
```bash
mysql -u root -p library_db < notification_tables.sql
```

### 4.4 配置文件

`application.yml` 中已添加RabbitMQ配置:
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 60000
```

### 4.5 启动应用

```bash
mvn spring-boot:run
```

---

## 五、测试说明

### 5.1 测试通知机制

1. 启动RabbitMQ服务
2. 启动应用
3. 调用通知发送接口:
   ```java
   notificationService.sendPickupNotice(1L, "Java编程思想", "理科馆", "ABC123");
   ```
4. 查询通知记录:
   ```sql
   SELECT * FROM tb_notification WHERE user_id = 1;
   ```

### 5.2 测试取书确认

1. 创建一条RESERVED状态的借阅记录:
   ```sql
   INSERT INTO tb_book_borrow (copy_id, user_id, status, reserved_time, pickup_deadline, pickup_library_id)
   VALUES (1, 1, 'RESERVED', NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR), 1);
   ```

2. 调用取书确认接口:
   ```bash
   curl -X POST http://localhost:8080/api/borrow/confirm-pickup \
   -H "Content-Type: application/json" \
   -d '{"borrowId":1,"userId":1}'
   ```

3. 验证状态变更:
   ```sql
   SELECT * FROM tb_book_borrow WHERE id = 1;
   SELECT * FROM tb_book_copy WHERE id = 1;
   ```

### 5.3 测试超期释放

1. 创建一条已超期的预留记录:
   ```sql
   INSERT INTO tb_book_borrow (copy_id, user_id, status, reserved_time, pickup_deadline, pickup_library_id)
   VALUES (1, 1, 'RESERVED', DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 1);
   ```

2. 等待定时任务执行(每小时执行一次),或手动触发:
   ```java
   reservationExpireTask.releaseExpiredReservations();
   ```

3. 验证结果:
   ```sql
   SELECT * FROM tb_book_borrow WHERE id = 1;  -- status应为CANCELLED
   SELECT * FROM tb_book_copy WHERE id = 1;    -- status应为AVAILABLE
   SELECT * FROM tb_user WHERE id = 1;         -- creditScore应减少5分
   SELECT * FROM tb_notification WHERE user_id = 1;  -- 应有超期通知
   ```

---

## 六、后续扩展

### 6.1 短信通知(TODO)

1. 集成短信服务商API(如阿里云短信、腾讯云短信)
2. 在 `NotificationServiceImpl.processNotification()` 中添加短信发送逻辑
3. 创建短信通知模板

### 6.2 预约队列自动分配

在超期释放后,自动检查预约队列,将图书分配给下一个预约者:
```java
// TODO: 在ReservationExpireTask中实现
reservationQueueService.checkAndAssignNext(copy.getId());
```

### 6.3 通知偏好设置

允许用户自定义接收哪些类型的通知:
1. 创建用户通知偏好表
2. 在发送通知前检查用户偏好
3. 提供用户偏好设置接口

---

## 七、注意事项

1. **RabbitMQ必须启动**: 应用启动前请确保RabbitMQ服务已启动
2. **数据库字段**: 请确保已执行SQL脚本更新数据库表结构
3. **定时任务**: 定时任务每小时执行一次,测试时可以手动触发
4. **信用分**: 用户信用分最低为0,不会出现负数
5. **借阅期限**: 默认为30天,可在代码中调整

---

## 八、文件清单

### 新增文件

**实体类**:
- `src/main/java/com/library/entity/Notification.java`
- `src/main/java/com/library/entity/NotificationTemplate.java`

**Mapper**:
- `src/main/java/com/library/mapper/NotificationMapper.java`
- `src/main/java/com/library/mapper/NotificationTemplateMapper.java`

**DTO**:
- `src/main/java/com/library/dto/NotificationMessage.java`
- `src/main/java/com/library/dto/PickupConfirmRequest.java`
- `src/main/java/com/library/dto/PickupConfirmResponse.java`

**Service**:
- `src/main/java/com/library/service/NotificationService.java`
- `src/main/java/com/library/service/NotificationTemplateService.java`
- `src/main/java/com/library/service/PickupService.java`
- `src/main/java/com/library/service/impl/NotificationServiceImpl.java`
- `src/main/java/com/library/service/impl/NotificationTemplateServiceImpl.java`
- `src/main/java/com/library/service/impl/PickupServiceImpl.java`

**Controller**:
- `src/main/java/com/library/controller/NotificationTemplateController.java`
- `src/main/java/com/library/controller/PickupController.java`

**配置类**:
- `src/main/java/com/library/config/RabbitMQConfig.java`

**消费者**:
- `src/main/java/com/library/consumer/NotificationConsumer.java`

**定时任务**:
- `src/main/java/com/library/task/ReservationExpireTask.java`
- `src/main/java/com/library/task/ReservationWarningTask.java`

**SQL脚本**:
- `notification_tables.sql`

### 修改文件

- `pom.xml` - 添加RabbitMQ依赖
- `src/main/resources/application.yml` - 添加RabbitMQ配置
- `src/main/java/com/library/entity/BookBorrow.java` - 添加预留相关字段

---

## 九、联系方式

如有问题,请联系开发团队。

---

**文档版本**: v1.0
**最后更新**: 2026-02-27
