## Why

`workspace-contact-add-external-web` 已交付添加外部联系人 UI，`workspace-contact-add-external-api` 已落地 `search-account` / `POST request` / `reject`；前端 `contactAdd` 与 `contacts.rejectRequest` 仍用本地草稿。需联调去草稿，使搜索与申请以服务端为真源并关闭本切片。

## What Changes

- 实现 `api/app/contacts.ts`：`searchContactAccounts` / `createContactRequest`（`rejectContactRequest` 已接）
- `stores/contactAdd.ts`：`search` / `sendRequest` 只走 API；删除 `buildDraftHits` 与本地假账号
- `stores/contacts.ts`：`rejectRequest` 改调 `rejectContactRequest` 并刷新列表
- 更新 `workspace-ui-patterns`、对接看板 `web → done`；contract 标注前端已接
- 浏览器验收：Hub / 通讯录搜索真账号 → 发起申请 → 对方可同意/拒绝

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-contacts-directory`：去掉「`-web` 可用客户端草稿搜索/发起」例外；添加外部与拒绝 MUST 以 `/app-api/system/contact/*` 为真源

## Impact

- **前端**：`web/src/api/app/contacts.ts`、`stores/contactAdd.ts`、`stores/contacts.ts`；Modal 一般只改错误文案
- **文档**：`api-integration-board.md`、`workspace-ui-patterns.md`、`openspec/lanes/workspace-contacts-enrich/contract.md`
- **依赖**：须先完成 `workspace-contact-add-external-api`
- **不改**：Java / Flyway；组织邀请；member-picker
