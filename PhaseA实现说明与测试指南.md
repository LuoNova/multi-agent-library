# Phase A实现说明与测试指南(2026-03-09)

本文档用于说明 Phase A 已落地的代码改动，并指导你完成数据准备、接口调试与数据库期望变化验证。

---

## 一、实现内容总览

### 1.1 目标
- 统一调拨时间字段写入,保证进度/延迟检测可靠
- 预留超期释放形成闭环(释放+计数回滚+继续兑现下一预约者)
- 增加用户调拨列表查询能力(可分页/可按状态筛选)

### 1.2 主要改动文件
- `src/main/java/com/library/service/TransferService.java`
- `src/main/java/com/library/service/BookReturnService.java`
- `src/main/java/com/library/task/ReservationExpireTask.java`
- `src/main/java/com/library/service/ReservationFulfillmentService.java`(新增)
- `src/main/java/com/library/entity/BookTransfer.java`
- `src/main/java/com/library/service/TransferProgressService.java`
- `src/main/java/com/library/controller/TransferProgressController.java`
- `src/main/java/com/library/constant/LibraryConstants.java`

---

## 二、功能实现细节(按Phase A拆分)

## 2.1 A1-调拨时间字段统一

### 改动点
- `TransferService.createTransfer`:
  - 写入`requestTime`
  - 写入`estimatedArrivalTime=now+estimatedMinutes`
  - 不再将`completeTime`当作预计到达
- `TransferService.completeTransfer`:
  - 写入`actualArrivalTime`
  - 同步写入`completeTime`(作为完成处理时间)

### 影响与预期
- `TransferDelayCheckTask`按`estimated_arrival_time`检测延迟
- `GET /api/transfer/progress/{transferId}`在`IN_TRANSIT`状态下能返回正确的预计到达时间与进度百分比

---

## 2.2 A2-预留超期释放闭环

### 改动点
- 预留阶段`tb_book_borrow`统一写入:
  - `reserved_time`
  - `pickup_deadline`
  - `pickup_library_id`
  - `due_time`(承载预留到期时间,取书确认后会覆盖为30天应还期)
- `ReservationExpireTask`增强:
  - 超期释放时:Borrow `RESERVED->CANCELLED`,Copy `RESERVED->AVAILABLE`
  - 回滚用户`current_borrow_count-1`
  - 扣信用分-5并必要时冻结账号
  - 调用`ReservationFulfillmentService`自动兑现下一位预约者
- 新增`ReservationFulfillmentService`:
  - 输入:一个`AVAILABLE`副本
  - 输出:无预约/就地预留兑现/跨馆调拨兑现

---

## 2.3 A3-用户调拨列表

### 改动点
- `BookTransfer`新增字段`receiverUserId`(数据库字段`receiver_user_id`)
- 创建用户请求调拨时写入接收用户ID:
  - 借书跨馆调拨(`UserDemandAgent`发起):receiver_user_id=借书用户
  - 还书预约兑现跨馆调拨(`BookReturnService`发起):receiver_user_id=预约者
  - 预约超期释放后继续兑现跨馆调拨(`ReservationFulfillmentService`发起):receiver_user_id=预约者
- 实现接口:
  - `GET /api/transfer/my-transfers?userId=1&status=IN_TRANSIT&page=1&size=10`

---

## 三、数据库变更与初始化检查(必须先做)

>以下SQL请在 MySQL 中执行,执行前建议先检查字段是否存在。

### 3.1 检查字段是否存在

```sql
SHOW COLUMNS FROM tb_book_transfer LIKE 'estimated_arrival_time';
SHOW COLUMNS FROM tb_book_transfer LIKE 'actual_arrival_time';
SHOW COLUMNS FROM tb_book_transfer LIKE 'receiver_user_id';
```

### 3.2 如果缺少字段则执行DDL

```sql
ALTER TABLE tb_book_transfer
  ADD COLUMN estimated_arrival_time DATETIME NULL COMMENT '预计到达时间',
  ADD COLUMN actual_arrival_time DATETIME NULL COMMENT '实际到达时间',
  ADD COLUMN receiver_user_id BIGINT NULL COMMENT '接收用户ID(用户请求/预约兑现时填写,库存平衡为空)';

CREATE INDEX idx_transfer_receiver_status_time
  ON tb_book_transfer(receiver_user_id,status,request_time);
```

### 3.3 推荐索引(提升定时任务扫描效率)

```sql
CREATE INDEX idx_borrow_status_deadline
  ON tb_book_borrow(status,pickup_deadline);

CREATE INDEX idx_reservation_biblio_status_time
  ON tb_book_reservation(biblio_id,status,reserve_time);
```

---

## 四、测试指南(数据准备+接口调试+期望数据变化)

>建议先执行项目内的`test_data.sql`作为基础数据,再按场景补充/调整。

### 4.1 启动检查
1. 启动应用:

```bash
mvn spring-boot:run
```

2. 打开Swagger:
`http://localhost:8080/swagger-ui.html`

---

## 4.2 场景A(借书触发跨馆调拨,验证A1+A3)

### 数据准备
确保目标馆无库存,其他馆有库存且副本为`AVAILABLE`:

```sql
-- 示例:目标馆=1无库存,来源馆=2有库存
UPDATE tb_library_biblio_stats SET stock_count = 0 WHERE library_id = 1 AND biblio_id = 1;
UPDATE tb_library_biblio_stats SET stock_count = 5 WHERE library_id = 2 AND biblio_id = 1;

UPDATE tb_book_copy
SET library_id = 2, status = 'AVAILABLE'
WHERE biblio_id = 1
LIMIT 3;
```

### 接口调试
1. 借书请求:
`POST /api/borrow/request`

```json
{
  "userId": 1,
  "biblioId": 1,
  "preferredLibraryId": 1
}
```

### 期望结果与数据库变化
- 返回`strategy=TRANSFER_PROVIDE`且包含`transferId`
- 数据库期望:

```sql
-- 调拨记录应写入estimated_arrival_time与receiver_user_id
SELECT id,status,transfer_reason,receiver_user_id,request_time,estimated_arrival_time,actual_arrival_time,complete_time
FROM tb_book_transfer
WHERE id = <transferId>;

-- 副本应为IN_TRANSIT
SELECT id,library_id,status
FROM tb_book_copy
WHERE id = <copyId>;
```

2. 查询调拨进度:
`GET /api/transfer/progress/<transferId>`
- 期望返回`timeInfo.estimatedArrivalTime`不为null

3. 查询用户调拨列表:
`GET /api/transfer/my-transfers?userId=1&page=1&size=10`
- 期望`records`包含上述`transferId`

---

## 4.3 场景B(调拨完成回调,验证A1字段落库与预留字段完整)

### 接口调试
`POST /api/transfer/complete`

```json
{
  "transferId": <transferId>
}
```

### 数据库期望
```sql
-- 调拨应为COMPLETED且actual_arrival_time不为null
SELECT status,estimated_arrival_time,actual_arrival_time,complete_time
FROM tb_book_transfer
WHERE id = <transferId>;

-- 副本应移动到目标馆且为RESERVED
SELECT id,library_id,status,location
FROM tb_book_copy
WHERE id = <copyId>;

-- Borrow应为RESERVED且pickup_deadline/due_time已写入
SELECT id,user_id,status,reserved_time,pickup_deadline,pickup_library_id,due_time
FROM tb_book_borrow
WHERE copy_id = <copyId>
ORDER BY id DESC LIMIT 1;
```

---

## 4.4 场景C(预留超期释放闭环,验证A2)

### 数据准备(制造一条已超期的RESERVED借阅记录)
1. 找到一条`RESERVED`的Borrow记录(可用场景B产生):

```sql
SELECT id,copy_id,user_id,status,pickup_deadline
FROM tb_book_borrow
WHERE status='RESERVED'
ORDER BY id DESC LIMIT 1;
```

2. 将其`pickup_deadline`调成过去:

```sql
UPDATE tb_book_borrow
SET pickup_deadline = NOW() - INTERVAL 1 HOUR,
    due_time = NOW() - INTERVAL 1 HOUR
WHERE id = <borrowId>;
```

3. 准备下一位预约者(确保存在PENDING预约,用于验证“释放后继续兑现”):

```sql
-- 假设同书目biblio_id=1,让用户2排队预约(更早reserve_time优先)
INSERT INTO tb_book_reservation(biblio_id,user_id,pickup_library_id,reserve_time,expire_time,status,notification_sent,create_time)
VALUES(1,2,1,NOW()-INTERVAL 2 HOUR,NOW()+INTERVAL 3 DAY,'PENDING',0,NOW());
```

### 触发方式
- 等待定时任务每小时执行一次
- 或在IDE中直接运行`ReservationExpireTask.releaseExpiredReservations()`进行验证

### 期望数据库变化
```sql
-- 1)原Borrow应变为CANCELLED
SELECT status FROM tb_book_borrow WHERE id = <borrowId>;

-- 2)副本应先变为AVAILABLE,若存在下一预约者且就地预留则最终为RESERVED
SELECT status FROM tb_book_copy WHERE id = <copyId>;

-- 3)原用户借阅计数-1,信用分-5(必要时冻结)
SELECT current_borrow_count,credit_score,status
FROM tb_user
WHERE id = <originalUserId>;

-- 4)如果兑现下一预约者(用户2),则其预约应FULFILLED或产生调拨
SELECT id,status,copy_id,fulfill_time
FROM tb_book_reservation
WHERE user_id=2 AND biblio_id=1
ORDER BY id DESC LIMIT 1;

-- 5)如果是就地预留兑现,应新增Borrow(RESERVED,user_id=2)
SELECT id,user_id,status,pickup_deadline
FROM tb_book_borrow
WHERE copy_id=<copyId>
ORDER BY id DESC LIMIT 3;
```

---

## 五、常见问题

### 5.1 `/api/transfer/my-transfers`查询为空
- 检查`tb_book_transfer.receiver_user_id`是否已加字段并已写入
- 只有用户相关调拨会写入该字段,库存平衡调拨为空

### 5.2 进度查询estimatedArrivalTime为null
- 检查数据库字段`estimated_arrival_time`是否存在
- 确认调拨记录是否通过`TransferService.createTransfer`创建

