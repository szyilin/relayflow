# API 契约：admin-product-settings

> **状态**：integrate 完成（Admin 三页 + 工作台强制执行骨架）  
> **变更**：`admin-product-settings-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端「产品设置」：云文档 / 即时消息 / 日历入口；IM 三块组织策略（群规模、群权限、消息权限）。个人偏好仍走 `user-preference`。

## 鉴权

| 面 | 路径 | 鉴权 |
|----|------|------|
| Admin | `/admin-api/im/product-settings*` | JWT + 管理身份 + `im:product-settings:query` / `update` |
| App | `/app-api/im/settings/effective` · `/app-api/im/group/*` · `/app-api/im/message/recall|edit` | JWT + 有效成员 |

前端：`api/admin/imProductSettings.ts` · `api/app/im.ts` → stores → `/admin/products/**` · `/app/messages`。

## Admin 产品设置

- `GET/PUT /admin-api/im/product-settings` — 主配置 + 用户/群组规则集（字段见设计 D4）
- `GET /admin-api/im/product-settings/groups?keyword=` — 本租户对话群选择器
- `allowPublicGroup=false` 保存时批量关闭已公开群

默认：群上限 50；公开允许；部门群退出允许；导出禁止；撤回/编辑允许且 24h。

## App 生效策略与配套能力

| Method | Path | 说明 |
|--------|------|------|
| GET | `/app-api/im/settings/effective` | 开关与时间窗摘要 |
| GET | `/app-api/im/group/info` | searchable / groupKind / myRole |
| PUT | `/app-api/im/group/public` | 设置可搜索公开 |
| GET | `/app-api/im/group/search-public` | 搜公开群 |
| POST | `/app-api/im/group/join` | 直接加入 |
| POST | `/app-api/im/group/leave` | 退群（部门群受策略） |
| GET | `/app-api/im/group/members/export` | 导出成员 CSV 数据 |
| POST | `/app-api/im/group/create` | 支持 `groupKind` / `deptId` |
| POST | `/app-api/im/message/recall` | 撤回（群主/管理员豁免组织禁止） |
| POST | `/app-api/im/message/edit` | 编辑（保 seq） |

WS：`message.recalled` / `message.edited`。

建群/加人上限：群组规则 > 用户规则 > 默认；**部门群不受默认/用户规则**。

## 前端路由

`/admin/products/**`（docs/calendar 占位）；`/app/messages`（公开群 / 撤回编辑 / 导出 / 部门群）。
