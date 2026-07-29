## Context

母 change `admin-product-settings-v1` 已交付产品设置壳层与 IM 组织策略；日历仅为占位页。

## Goals / Non-Goals

**Goals**

- 定义日历组织级设置 V1 子集（对标飞书日历管理类设置）
- Admin UI ↔ Admin API ↔ 工作台 calendar 强制
- 与 `user-preference` 日历段清晰分层

**Non-Goals**

- 一次会话全栈；个人默认视图/主题等仍属个人偏好

## Open Questions（深化前拍板）

1. V1 首批项：默认会议时长、允许成员建公开日历、工作时段组织默认等取哪些？
2. 与 `CalEventApi` / meeting 预约的交叉约束？
3. 权限码独立还是复用产品设置码？

## Decisions（骨架）

| ID | 决策 | 说明 |
|----|------|------|
| D1 | 路由 | `/admin/products/calendar` + 二级分组 |
| D2 | 分层 | 组织策略 ≠ `user-preference` 日历段 |
| D3 | 交付 | 可拆 `-web` / `-api` / `-integrate` |

## Risks

| 风险 | 缓解 |
|------|------|
| 与会议/预约重叠 | design 写清「日历产品设置」vs meeting 域 |
| 个人偏好误迁 | 规格显式禁止本页写 preference |
