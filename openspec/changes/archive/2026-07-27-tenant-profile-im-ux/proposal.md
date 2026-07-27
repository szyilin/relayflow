## Why

工作台头像/昵称等资料当前落在全局 `sys_user`，一账号多企业共享同一头像，违背「除账号密码外资料跟随企业+账号」的产品约定；无有效头像时「登录更多账号」原生 `<img>` 会破图。IM 切换会话每次强制 skeleton 且滚底不可靠，缺少主流「查看未读 / 回到最新」体验。

## What Changes

- **BREAKING（行为）**：`GET/PUT /app-api/system/user/profile` 与 permission-info 中的 `nickname` / `avatar` / `signature` / `coverFileId` 改为**当前企业成员级**读写（作用域 `tenant_id + user_id`），不再写全局 `sys_user` 对应字段
- Flyway：在 `sys_tenant_user` 增加成员资料列；迁移时用现有 `sys_user` 值回填各 ACTIVE 成员行
- 前端 Account Dock：头像/昵称只更新当前 `userId:tenantId` 条目，禁止跨企业同步
- 所有头像展示：无效/空 fileId 时稳定回退文字占位，禁止破图
- IM：按会话内存缓存 + 有缓存时静默刷新（无 skeleton 闪烁）；打开会话可靠滚底；未读锚点 FAB +「回到最新」；可视区/离开会话再上报已读

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `system`：成员资料从全局账号字段改为企业成员级
- `web-auth`：Account Dock / 资料卡按企业+账号隔离头像昵称；无头像破图修复
- `workspace-business-card`：名片资料跟随当前企业成员
- `im`：消息列表缓存、滚动与未读导航、已读水位策略（前端为主；API 契约不变）

## Impact

- 模块：`relayflow-module-system`（Flyway + UserService + permission-info）、`web/`（dock、avatar、messages、im store）
- API 路径不变，响应语义变为租户作用域
- 回滚：可保留列；读路径需再改回 `sys_user`（0.x 可破坏性迁移已征得本 change 范围同意）
