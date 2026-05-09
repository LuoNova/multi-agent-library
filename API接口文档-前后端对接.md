# 图书馆多智能体系统 — 标准接口文档（前后端对接）

> 依据当前仓库 **Controller 层** 实现整理，与 Swagger（`springdoc`）可对照校验。  
> **Base URL**：`http://<host>:<port>`，默认后端端口 **`8080`**，上下文路径 **`/`**（无前缀）。  
> **Swagger UI**（推荐联调时对照）：`http://localhost:8080/swagger-ui/index.html`  
> **前端中文展示与解析约定**（不改后端字段名）：见 **`frontend/docs/前端字段与接口约定.md`**

---

## 1. 全局约定

### 1.1 统一响应 `Result<T>`（多数业务接口）

除 **§10 通知模板**、**§11 测试** 外，下列 Controller 的 JSON 响应体均为：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | **200** 成功；**400** 业务/参数错误；**404** 资源不存在（部分业务异常）；**500** 未预期异常 |
| message | String | 提示文案 |
| data | T | 成功时为业务数据；失败时多为 **null** |

**注意**：借书接口在「协商成功但业务失败」时仍可能 `code=200` 且 `data.status=FAILED`，前端需同时判断 **`code` 与 `data.status`**（见 §2.1）。

### 1.2 时间与格式

| 场景 | 约定 |
|------|------|
| Query：`LocalDate` | `yyyy-MM-dd`，如 `2026-03-10` |
| Query：`LocalTime` | `HH:mm`，如 `09:00` |
| Query / Body：`LocalDateTime`（故障列表筛选） | ISO-8601，如 `2026-04-01T00:00:00` |
| Body：`LocalDate` / `LocalTime`（座位预约 JSON） | 与字段上 `@JsonFormat` 一致：`yyyy-MM-dd`、`HH:mm` |

### 1.3 分页列表通用结构

以下接口 **`data`** 结构一致：`total`、`page`、`size`、`records`（数组）。

- `GET /api/seat/reservation/my`
- `GET /api/fault/list`
- `GET /api/transfer/my-transfers`（由服务层封装）
- `GET /api/transfer/list`

---

## 2. 图书借还 `/api/borrow`

### 2.1 发起借书请求

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/borrow/request` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |
| biblioId | Long | 是 | 书目 ID |
| preferredLibraryId | Long | 是 | 期望取书馆 ID |

**Response `data`：`BorrowResult`**

| 字段 | 类型 | 说明 |
|------|------|------|
| status | String | **SUCCESS** / **FAILED** |
| strategy | String | LOCAL_LOAN / TRANSFER_PROVIDE / RESERVATION 等 |
| message | String | 提示信息 |
| borrowId | Long | 本地借阅时借阅记录 ID |
| reservationId | Long | 预约排队时预约 ID |
| transferId | Long | 跨馆调拨时调拨记录 ID |
| copyId | Long | 图书副本 ID |
| sourceLibraryId | Long | 调拨源馆 |
| targetLibraryId | Long | 目标馆 |
| taskId | String | 任务 ID |
| estimatedArrivalMinutes | Integer | 调拨预计到达（分钟） |
| recommendSeatLibraryId | Long | 推荐预约座位所在馆 |
| recommendSeatDate | String | 推荐日期 `yyyy-MM-dd` |
| recommendSeatReason | String | 如 LOCAL_LOAN、TRANSFER_PICKUP |
| actionRecommendSeat | Boolean | 是否展示「预约座位」入口 |

**说明**：内部等待 Agent 处理最多约 **5 秒**；超时返回失败 `message` 含「处理超时」。

---

### 2.2 确认取书

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/borrow/confirm-pickup` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 |
|------|------|------|
| borrowId | Long | 是 |
| userId | Long | 是 |

**Response `data`：`PickupConfirmResponse`**

| 字段 | 类型 | 说明 |
|------|------|------|
| borrowId | Long | 借阅记录 ID |
| bookTitle | String | 书名 |
| borrowDate | String (datetime) | 借阅时间 |
| dueDate | String (datetime) | 应还时间 |
| libraryName | String | 取书馆名称 |
| recommendSeatLibraryId | Long | 取书后推荐占座馆 |
| recommendSeatDate | String | 推荐日期 |
| recommendSeatReason | String | 如 TRANSFER_PICKUP |
| actionRecommendSeat | Boolean | 是否建议展示预约座位 |

---

## 3. 图书归还 `/api/book`

### 3.1 归还图书

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/book/return` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| copyId | Long | 是 | 图书副本（条码）ID |
| userId | Long | 是 | 还书人 |
| returnLibraryId | Long | 是 | 还书所在馆 ID |

**Response `data`：`ReturnResult`**（`BookReturnService.ReturnResult`，含 success、message 及策略相关字段；成功时 `code=200`）

---

## 4. 图书调拨 `/api/transfer`

### 4.1 调拨完成回调（单条）

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/transfer/complete` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 |
|------|------|------|
| transferId | Long | 是 |

**Response `data`：`TransferCompleteResult`**（含 success、message 等）

---

### 4.2 批量调拨完成回调

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/transfer/complete-batch` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 |
|------|------|------|
| orderId | Long | 是 | 调拨单 ID |

**Response `data`：`BatchCompleteResult`**

---

### 4.3 单个调拨进度

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/transfer/progress/{transferId}` |

**Path**

| 参数 | 类型 | 必填 |
|------|------|------|
| transferId | Long | 是 | 调拨记录 ID |

**Response `data`：`TransferProgressDTO`**（进度百分比、状态、时间、图书/馆信息等）

---

### 4.4 批量调拨进度

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/transfer/progress/batch/{orderId}` |

**Path**

| 参数 | 类型 | 必填 |
|------|------|------|
| orderId | Long | 是 | 调拨单 ID |

**Response `data`：`BatchTransferProgressDTO`**

---

### 4.5 用户调拨列表

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/transfer/my-transfers` |

**Query**

| 参数 | 类型 | 必填 | 默认 |
|------|------|------|------|
| userId | Long | 是 | — |
| status | String | 否 | — |
| page | int | 否 | 1 |
| size | int | 否 | 10 |

**Response `data`：** `Map`：`total`、`page`、`size`、`records`

---

### 4.6 全部调拨记录（管理）

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/transfer/list` |

**Query**

| 参数 | 类型 | 必填 | 默认 |
|------|------|------|------|
| status | String | 否 | — |
| page | int | 否 | 1 |
| size | int | 否 | 10 |

**Response `data`：** `total`、`page`、`size`、`records`（元素为 `BookTransfer` 实体结构）

---

## 5. 调拨建议 `/api/transfer/suggestion`

### 5.1 待审批建议列表

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/transfer/suggestion/list` |

**Response `data`：** `TransferSuggestion[]`

---

### 5.2 建议详情

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/transfer/suggestion/{id}` |

**Path**：`id` Long，建议 ID。

**说明（实现现状）**：代码中详情查询 **TODO**，当前可能返回 **`data` 为 null**；联调前请与后端确认是否已补齐。

---

### 5.3 审批通过

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/transfer/suggestion/approve/{id}` |

**Path**：`id` Long  

**Query**

| 参数 | 类型 | 必填 |
|------|------|------|
| approverId | Long | 是 |

**Response `data`：** `null`

---

### 5.4 审批拒绝

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/transfer/suggestion/reject/{id}` |

**Path**：`id` Long  

**Query**

| 参数 | 类型 | 必填 |
|------|------|------|
| approverId | Long | 是 |
| reason | String | 否 |

**Response `data`：** `null`

---

### 5.5 手动触发库存平衡分析

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/transfer/suggestion/trigger-balance` |

无 Body。  

**Response `data`：** `null`

---

## 6. 座位 `/api/seat`

### 6.1 查询可用座位 / 自动分配

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/seat/available` |

**Query**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| libraryId | Long | 是 | 馆 ID |
| reservationDate | String | 是 | `yyyy-MM-dd` |
| startTime | String | 是 | `HH:mm` |
| endTime | String | 是 | `HH:mm` |
| areaId | Long | 否 | 限定区域 |
| hasPower | Boolean | 否 | true 仅要有电源座位 |
| autoAssign | Boolean | 否 | 默认 **true**，false 返回列表 |

**Response `data`：`SeatAvailabilityResultDTO`**

| 字段 | 类型 | 说明 |
|------|------|------|
| total | Long | `autoAssign=false` 时可用座位数 |
| seats | Array\<SeatInfoDTO\> | 列表模式 |
| availableTotal | Long | 自动分配模式下满足条件总数 |
| assignedSeat | SeatInfoDTO | 自动分配时的推荐座位 |

**`SeatInfoDTO`**：`seatId`、`areaId`、`areaName`、`floor`、`seatNo`、`hasPower`

**业务说明**：无可用座位时仍 **`code=200`**，`message` 可能为「当前条件下无可用座位」，通过 **`total`/`availableTotal` 为 0** 判断。

---

### 6.2 创建座位预约

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/seat/reservation` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | |
| seatId | Long | 是 | |
| libraryId | Long | 是 | 校验座位所属馆 |
| reservationDate | String | 是 | `yyyy-MM-dd` |
| startTime | String | 是 | `HH:mm` |
| endTime | String | 是 | `HH:mm` |
| borrowId | Long | 否 | 借书/取书联动 |
| source | String | 否 | WALK_IN / BORROW_PICKUP；可省略由后端推断 |

**Response `data`：`SeatReservationResultDTO`**（含 `reservationId`、座位与区域展示字段、`source` 等）

---

### 6.3 取消座位预约

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/seat/reservation/cancel` |

**Query**

| 参数 | 类型 | 必填 |
|------|------|------|
| reservationId | Long | 是 |
| userId | Long | 是 |

**Response `data`：** `null`

---

### 6.4 座位签到

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/seat/reservation/check-in` |

**Query**：`reservationId`（Long，必填）、`userId`（Long，必填）

**Response `data`：** `null`

---

### 6.5 座位暂离

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/seat/reservation/temp-leave` |

**Query**：`reservationId`、`userId`

**Response `data`：** `null`

---

### 6.6 结束暂离

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/seat/reservation/temp-leave/end` |

**Query**：`reservationId`、`userId`

**Response `data`：** `null`

---

### 6.7 结束使用

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/seat/reservation/finish` |

**Query**：`reservationId`、`userId`

**Response `data`：** `null`

---

### 6.8 我的座位预约列表

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/seat/reservation/my` |

**Query**

| 参数 | 类型 | 必填 | 默认 |
|------|------|------|------|
| userId | Long | 是 | — |
| status | String | 否 | ACTIVE/CANCELED/COMPLETED/NO_SHOW |
| fromDate | String | 否 | `yyyy-MM-dd` |
| toDate | String | 否 | `yyyy-MM-dd` |
| page | int | 否 | 1 |
| size | int | 否 | 10 |

**Response `data`：** 分页 Map，`records` 为预约记录列表（结构以实际返回为准）

---

## 7. 故障报修 `/api/fault`

### 7.1 提交工单

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/fault/report` |
| **Content-Type** | `application/json` |

**Request Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| libraryId | Long | 条件 | 与 areaId、seatId、equipmentId **至少其一非空** |
| areaId | Long | 条件 | 同上 |
| seatId | Long | 条件 | 同上 |
| equipmentId | Long | 条件 | 同上 |
| faultType | String | 是 | seat_broken / power_failure / env_issue / network_fault / other |
| severity | String | 是 | low / medium / high |
| title | String | 是 | |
| description | String | 否 | |
| reportSource | String | 是 | USER / MONITOR / ADMIN / SYSTEM |
| reportUserId | Long | 否 | |

**校验**：非空 ID 须存在，且 seat/area/library、equipment 与馆/区层级一致（详见《功能文档》Part3）。

**Response `data`：`FaultReportVO`**（含 `id`、`status=REPORTED`、`createdTime` 等）

---

### 7.2 工单分页列表

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/fault/list` |

**Query**

| 参数 | 类型 | 必填 | 默认 |
|------|------|------|------|
| libraryId | Long | 否 | |
| areaId | Long | 否 | |
| seatId | Long | 否 | |
| equipmentId | Long | 否 | |
| status | String | 否 | |
| faultType | String | 否 | |
| severity | String | 否 | |
| startTime | datetime | 否 | ISO |
| endTime | datetime | 否 | ISO |
| page | int | 否 | 1 |
| size | int | 否 | 10 |

**Response `data`：** `total`、`page`、`size`、`records`（`FaultReportVO[]`）

---

### 7.3 工单详情

| 项 | 说明 |
|----|------|
| **Method / Path** | `GET /api/fault/{id}` |

**Path**：`id` Long

**Response `data`：`FaultReportVO`**

---

### 7.4 更新工单状态

| 项 | 说明 |
|----|------|
| **Method / Path** | `PATCH /api/fault/{id}/status` |
| **Content-Type** | `application/json` |

**Path**：`id` Long  

**Request Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | REPORTED / ACCEPTED / IN_PROGRESS / RESTORED / CLOSED（允许任意切换） |
| assignee | String | 否 | JSON 含键且非 null 时更新 |
| adminRemark | String | 否 | 含键且非 null 时覆盖 |

**Response `data`：`FaultReportVO`**

---

### 7.5 批量资源健康查询

| 项 | 说明 |
|----|------|
| **Method / Path** | `POST /api/fault/health/query` |
| **Content-Type** | `application/json` |

**Request Body**

```json
{
  "resources": [
    { "resourceType": "LIBRARY", "resourceId": 1 },
    { "resourceType": "SEAT_AREA", "resourceId": 1 },
    { "resourceType": "SEAT", "resourceId": 1001 },
    { "resourceType": "EQUIPMENT", "resourceId": 1 }
  ]
}
```

**resourceType**：`LIBRARY` | `SEAT_AREA` | `SEAT` | `EQUIPMENT`（大写）

**Response `data`：`FaultHealthQueryResponse`**

| 字段 | 类型 | 说明 |
|------|------|------|
| results | Array | 每项含 `resourceType`、`resourceId`、`available`、`hasCriticalFault`、`faultSummary` |

---

## 8. 设备 `/api/equipment`

### 8.1 更新设备状态

| 项 | 说明 |
|----|------|
| **Method / Path** | `PATCH /api/equipment/{id}/status` |
| **Content-Type** | `application/json` |

**Path**：`id` Long，设备 ID  

**Request Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | NORMAL / FAULT / MAINTAIN / DISABLED |

**Response `data`：** `Equipment` 实体（含 `id`、`libraryId`、`areaId`、`type`、`name`、`status`、`updateTime` 等）

**说明**：当前实现下 FAULT↔NORMAL 会与故障工单联动（自动建 SYSTEM 单或关闭活跃单）；详见 `故障报修与选座设备联动-实现说明.md`。

---

## 9. 定时任务测试 `/api/task`（开发/联调用）

均无 Body；返回 **`Result<String>`**。

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/task/trigger/reservation-expire` | 预约超期释放 |
| POST | `/api/task/trigger/transfer-delay-check` | 调拨延迟检查 |
| POST | `/api/task/trigger/inventory-balance` | 库存平衡任务 |
| POST | `/api/task/trigger/reservation-warning` | 预约提醒 |
| POST | `/api/task/trigger/seat-temp-leave` | 座位暂离处理 |

---

## 10. 通知模板 `/api/notification/template`（非 Result 包装）

以下接口返回体 **不是** `Result<T>`，前端需按「裸数据 / 纯文本」解析。

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/notification/template/list` | 返回 **`NotificationTemplate[]`** |
| GET | `/api/notification/template/get` | Query：`type`（必填）、`channel`（必填）→ 单个模板 |
| POST | `/api/notification/template/create` | Body：`NotificationTemplate` → 创建后实体 |
| PUT | `/api/notification/template/update` | Body：`NotificationTemplate` |
| DELETE | `/api/notification/template/delete/{templateId}` | 返回字符串 **「删除成功」** |
| PUT | `/api/notification/template/toggle/{templateId}` | Query：`enabled`（boolean，必填）→ 字符串 **「启用成功」/「禁用成功」** |

**`NotificationTemplate` 主要字段**：`id`、`type`、`channel`、`titleTemplate`、`contentTemplate`、`isEnabled`（0/1）、`createTime`、`updateTime`

---

## 11. 测试 `/test`（运维/开发）

| Method | Path | 响应 |
|--------|------|------|
| GET | `/test/ping` | 纯文本 `pong - 系统运行正常` |
| GET | `/test/jade-status` | 纯文本，JADE 容器状态描述 |

---

## 12. 接口索引表（按路径）

| 路径前缀 | 模块 |
|-----------|------|
| `/api/borrow` | 借书请求、取书确认 |
| `/api/book` | 还书 |
| `/api/transfer` | 调拨完成、进度、列表 |
| `/api/transfer/suggestion` | 调拨建议 |
| `/api/seat` | 座位可用与预约全流程 |
| `/api/fault` | 故障工单与健康查询 |
| `/api/equipment` | 设备状态 |
| `/api/task` | 定时任务手动触发 |
| `/api/notification/template` | 通知模板（无统一 Result） |
| `/test` | 存活与 JADE 探测 |

---

## 13. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-04-05 | 初版：按 Controller 扫描整理，供前端对接 |

# 前端启动方式：

cd frontend
npm install
npm run dev
