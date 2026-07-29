## Why

`workspace-contact-add-external-web` 已用 `contactAdd` 草稿跑通精确搜索与发起申请、拒绝 UI；enrich 表与 list/accept/star 已就绪。需落地 `search-account`、`POST request`、`reject`，供后续 integrate 去草稿。

## What Changes

- App API：`GET /app-api/system/contact/search-account`；`POST /app-api/system/contact/request`；`POST /app-api/system/contact/request/{id}/reject`
- 扩展 `ContactEnrichService`（复用既有 `sys_contact_request` / `sys_contact_external`，一般无需新表）
- 定稿 `openspec/lanes/workspace-contacts-enrich/contract.md` 发起/拒绝章；看板 api → archived
- **不**改 `web/` store（留给 `-integrate`）

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-contacts-directory`：增加按手机/邮箱精确搜索账号、发起申请、拒绝 pending 的后端行为（相对 enrich list/accept）

## Impact

- **后端**：`relayflow-module-system`（Controller / Service / 错误码）；通常无新 Flyway（表已在 `V0.1.0.40`）
- **前端**：本 change 不改 `web/`
- **依赖**：`workspace-contact-add-external-web` + enrich-api 已有表与 accept
- **回滚**：删端点即可；申请行可保留
- **不改**：扫码加好友、邮箱通讯录、Bot 推送
