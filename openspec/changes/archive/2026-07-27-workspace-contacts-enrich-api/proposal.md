## Why

`workspace-contacts-enrich-web` 已交付 `/app/contacts` 飞书式目录 UI，但「外部联系人 / 新的联系人 / 星标联系人」仍用 store 临时数据。需要按冻结契约补齐后端持久化与 REST，才能进入 `-integrate` 去 Mock。

## What Changes

- 新增联系人关系表：外部联系人、加联系人申请、星标（`sys_` 前缀，Flyway）
- 实现契约端点（`/app-api/system/contact/*`）：
  - 外部联系人列表
  - 申请列表 + 同意申请（同意后写入外部联系人）
  - 星标列表 + 设/取消星标
- 列表 VO 组装显示名、`orgLabel`、`avatarText`、`sortLetter`（与 contract 对齐）
- 冻结并更新 `openspec/lanes/workspace-contacts-enrich/contract.md` 状态为 ready 草案→实现对齐
- 更新对接看板：`API → ready`（Web 仍 `ui_ready`，留给 integrate）
- **本 change 仅后端**；不改 `web/` UI（联调属 `workspace-contacts-enrich-integrate`）

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-contacts-directory`：外部联系人 / 好友申请 / 星标从「允许 store 临时数据」升级为「MUST 由 `/app-api/system/contact/*` 持久化提供」；同意申请 MUST 落库并进入外部列表

## Impact

- **模块**：`relayflow-module-system`（api 若需 DTO 暴露；biz Controller/Service/DO/Mapper）、`relayflow-server` Flyway
- **表**：新建 `sys_contact_external`、`sys_contact_request`、`sys_contact_star`（名称以 design 为准）
- **不改**：组织内 `dept/tree` · `list-by-dept`；IM 群会话列表；邮箱联系人；完整「发起加好友」产品流（仅 list + accept）
- **回滚**：回滚 Flyway / 删除 Controller；前端仍可用临时数据，不影响已交付 `-web`
