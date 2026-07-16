## Context

已有实现呈现两种味道：根部门 `getOrCreateRootDept`（入企必须有行）、系统 Bot 触达（无行即默认策略）。用户偏好将采用读合并 + 写 upsert。需要把分类写清，避免后人统一做成「入企抄满表」。

## Goals / Non-Goals

**Goals:**

- 文档化 A/B/C 三类默认数据策略与选型标准。
- 明确「集中规则、分散落库」：事件薄、各域 `ensure*` 自负。
- 禁止跨域上帝填充器。

**Non-Goals:**

- 本 change 不实现框架 SPI、不新增表、不改 Listener 代码。
- 不强制一次性把历史代码全部改造成同一接口（惯例约束新代码与重构）。

## Decisions

### D1. 三类策略

| 类 | 含义 | 何时落库 | 例子 |
|----|------|----------|------|
| **A 必须物化** | 无行则功能坏（FK、组织树、会话骨架） | 生命周期 **eager** 幂等 `ensure*` | 租户根部门 |
| **B 默认即无行** | 代码/策略可解释默认 | **不插行** | 系统 Bot 触达 |
| **C 偏好文档** | 读合并默认、写 upsert | **lazy**（首次改写才插） | `sys_user_preference` |

### D2. 集中什么、分散什么

- **集中**：分类标准与禁止项（本文档）；可选的 `MemberActivated` / `TenantCreated` 事件契约（已有跨域消息方向）。
- **分散**：各域自己的 `*EnsureService` / `*BootstrapService`；跨域同步走 `*-api`，异步走领域消息。

### D3. 禁止

- 单一 `DefaultDataService` 内 `switch(table)` 写入 `sys_` / `im_` / `infra_` 多前缀表。
- 对 C 类数据在入企时批量预插「完整默认 JSON」。

## Risks / Trade-offs

- [惯例不被遵守] → AGENTS / architecture 链到文档；code review 用 A/B/C 提问。
- [与历史 Bot enable 抄行不一致] → 以已拍板的 reach-policy 为准，文档引用为 B 类范例。

## Migration Plan

仅文档；无 DB 迁移。

## Open Questions

- 无。企业级默认层（`sys_tenant_preference`）留待产品需要时另开 change，merge 顺序预留在用户偏好 design。
