# 智能体配置修正说明

## 问题描述

在测试数据准备过程中，添加了工程馆（library_id=3）的数据，但是在JADE配置中只注册了两个InventoryAgent：
- `lib1`: 理科馆（library_id=1）
- `lib2`: 文科馆（library_id=2）

**缺少了工程馆的InventoryAgent**，这会导致：

1. **工程馆的资源无法参与调度**：如果用户需要从工程馆调拨书籍，系统无法处理
2. **智能体协商不完整**：UserDemandAgent只会与lib1和lib2协商，不会考虑工程馆
3. **资源利用率降低**：工程馆的6本书（4本计算机类、2本文学类）无法被系统利用

## 解决方案

在 `application.yml` 中添加工程馆的InventoryAgent配置：

```yaml
# JADE配置
jade:
  host: localhost
  port: 1099
  gui: true  # 是否启动JADE GUI（调试时用）
  agents: >
    demand:com.library.agent.UserDemandAgent;
    lib1:com.library.agent.InventoryAgent(1,理科馆);
    lib2:com.library.agent.InventoryAgent(2,文科馆);
    lib3:com.library.agent.InventoryAgent(3,工程馆)  # 新增
```

## 修正后的智能体配置

| Agent名称 | Agent类型 | 图书馆ID | 图书馆名称 |
|-----------|-----------|----------|------------|
| demand    | UserDemandAgent | - | - |
| lib1      | InventoryAgent | 1 | 理科馆 |
| lib2      | InventoryAgent | 2 | 文科馆 |
| lib3      | InventoryAgent | 3 | 工程馆 |

## 新增测试场景

### 场景2.5：工程馆跨馆调拨测试（新增）

**目的**：测试从工程馆跨馆调拨的流程，验证工程馆的智能体是否正常工作

**步骤**：
1. 在Swagger UI中找到 `BorrowController`
2. 调用 `/api/borrow` 接口
3. 输入以下参数：
   ```json
   {
     "userId": 1,
     "biblioId": 1,
     "preferredLibraryId": 2
   }
   ```
   - userId: 1（张三）
   - biblioId: 1（Java编程思想）
   - preferredLibraryId: 2（文科馆）

**说明**：
- 理科馆有3本《Java编程思想》
- 工程馆有2本《Java编程思想》
- 文科馆没有《Java编程思想》

**预期结果**：
- UserDemandAgent会向所有InventoryAgent（lib1、lib2、lib3）发送CFP消息
- lib1（理科馆）和lib3（工程馆）会返回PROPOSAL
- lib2（文科馆）不会返回PROPOSAL（因为没有这本书）
- 系统会根据评分算法选择最优方案
- 如果选择lib3，会触发从工程馆到文科馆的调拨

**验证点**：
- ✅ lib3（工程馆）参与智能体协商
- ✅ 工程馆的库存被正确评估
- ✅ 跨馆调拨成功执行
- ✅ 工程馆库存-1
- ✅ 文科馆库存+1（调拨完成后）

## 验证方法

### 1. 查看JADE GUI
启动应用后，JADE GUI会自动打开，可以看到：
- 4个Agent：demand、lib1、lib2、lib3
- Agent之间的消息传递
- 协商过程

### 2. 查看应用日志
查看日志确认Agent启动情况：
```
启动Agent: demand
启动Agent: lib1
启动Agent: lib2
启动Agent: lib3
```

### 3. 执行测试场景
执行场景2.5，验证工程馆的智能体是否正常工作。

## 注意事项

1. **重启应用**：修改配置后需要重启应用才能生效
2. **Agent命名**：Agent名称（lib1、lib2、lib3）必须与配置一致
3. **图书馆ID**：InventoryAgent的参数必须是数据库中存在的library_id
4. **智能体注册**：所有InventoryAgent都会在DF Service中注册，UserDemandAgent通过DF Service发现它们

## 影响范围

此次修正影响以下功能：
- ✅ 跨馆调拨：现在可以从工程馆调拨书籍
- ✅ 资源调度：工程馆的6本书可以参与资源调度
- ✅ 智能体协商：协商过程会考虑所有三个馆的资源
- ✅ 负载均衡：系统可以在三个馆之间进行负载均衡

## 总结

此次修正确保了：
1. 所有图书馆的InventoryAgent都已正确注册
2. 所有图书馆的资源都可以参与智能体协商
3. 系统可以充分利用所有图书馆的资源
4. 跨馆调拨功能覆盖所有图书馆

**请重启应用以使配置生效！**
