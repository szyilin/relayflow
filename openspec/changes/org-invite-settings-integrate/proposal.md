## Why

`org-invite-settings-web` 交付了管理端邀请设置 UI，`org-invite-settings-api` 已落地 `GET/PUT /admin-api/system/invite-settings`；前端 store 仍用会话内存。需联调去草稿，使刷新后仍保留设置并关闭本切片。

## What Changes

- `api/admin/inviteSettings.ts`：实现 `getInviteSettings` / `updateInviteSettings`（对齐 contract）
- `stores/inviteSettings.ts`：`load`/`save` 只走真实 API；删除「仅本地默认」真源
- 更新 `admin-ui-patterns`、对接看板 `web → done`；contract settings 章标注 integrate 完成
- 浏览器验收 `/admin/system/invite-settings`：保存 → 刷新仍可见

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `org-invite`：去掉「`-web` 可用会话内存、可不调后端」例外；管理端邀请设置 MUST 以 admin-api 为唯一真源
- `web-admin`：邀请设置页数据真源改为 API（文档/行为对齐）

## Impact

- **前端**：`web/src/api/admin/inviteSettings.ts`、`stores/inviteSettings.ts`；页面一般不改 template
- **文档**：`api-integration-board.md`、`admin-ui-patterns.md`、`openspec/lanes/org-invite/contract.md`
- **依赖**：须先完成 `org-invite-settings-api`（本地 server + Flyway）
- **不改**：Java / Flyway（除非联调发现 contract 缺口，另开修补）；工作台四渠道（`org-invite-channels-integrate`）
- **回滚**：回退前端 integrate 提交；DB 设置行可保留
