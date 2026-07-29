## Why

`org-invite-channels-web` 已交付四渠道 UI 与加入页，`org-invite-channels-api` 已落地 `app-api` capability / shared / mobile / consume；前端 `orgInvite` / `createHub` 仍用本地草稿。需联调去草稿，使凭证与加入写成员以服务端为真源并关闭本切片。

## What Changes

- 新增 `api/app/orgInvite.ts`（或等价）：capability、shared ensure/reset、mobile、consume，对齐 contract
- `stores/orgInvite.ts`：`loadShared` / `resetShared` / `inviteMobile` / `consume` 只走 API；删除随机 token/code 与本地校验真源
- `stores/createHub.ts`：`canInvite` 改读 capability API（或经 orgInvite store 暴露），不再仅用 inviteSettings + isAdmin 本地推导
- 更新 `workspace-ui-patterns`、对接看板 `web → done`；contract channels 章标注 integrate / 前端已接
- 浏览器验收：`+` → 添加企业成员（重置后旧凭证失效）；`/app/invite/join` 消费成功写入成员

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `org-invite`：去掉「`-web` 可用客户端草稿、可不调后端」例外；工作台四渠道与加入流 MUST 以 `/app-api/system/org-invite/*` 为唯一真源

## Impact

- **前端**：`web/src/api/app/`、`stores/orgInvite.ts`、`stores/createHub.ts`；Modal / join 页一般只改调用与文案，不重做布局
- **文档**：`api-integration-board.md`、`workspace-ui-patterns.md`、`openspec/lanes/org-invite/contract.md`
- **依赖**：须先完成 `org-invite-channels-api` + `org-invite-settings-integrate`（capability 依赖设置；本地 server + Flyway）
- **不改**：Java / Flyway（除非联调发现 contract 缺口，另开修补）；管理端邀请设置页；外部联系人 / member-picker
- **回滚**：回退前端 integrate 提交；DB 邀请行可保留
