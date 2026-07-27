## MODIFIED Requirements

### Requirement: 工作台用户资料 API

系统 MUST 向已认证工作台成员暴露 `/app-api/system/user/profile`，用于读写**当前企业成员级**展示资料（昵称、头像、签名、封面）；MUST NOT 要求 `sys_permission`。账号密码等凭证字段仍属全局 `sys_user`。

#### Scenario: 获取当前资料

- **WHEN** 已认证工作台成员请求 `GET /app-api/system/user/profile`
- **THEN** 返回 `userId`、`username`、`nickname`、`avatar`（fileId 或空）、`tenantId`、`tenantName`、`tenantVerified`（V1 恒为 `false`）、`isAdmin`
- **AND** `nickname` / `avatar` / `signature` / `coverFileId` 来自当前 `sys_tenant_user` 成员资料（无成员值时昵称可回退 `sys_user` / username）

#### Scenario: 更新昵称

- **WHEN** 成员调用 `PUT /app-api/system/user/profile` 并提交非空 `nickname`（≤ 64 字符）
- **THEN** 系统更新当前租户 `sys_tenant_user.nickname` 并返回与 GET 相同结构
- **AND** MUST NOT 用该昵称覆盖同账号其他企业的成员昵称

#### Scenario: 更新头像

- **WHEN** 成员调用 `PUT /app-api/system/user/profile` 并提交有效租户内 `fileId` 作为 `avatar`
- **THEN** 系统写入当前租户 `sys_tenant_user.avatar` 并返回更新后资料
- **AND** MUST NOT 写入全局 `sys_user.avatar` 作为成员展示真源

#### Scenario: 拒绝空昵称

- **WHEN** `PUT` 将 `nickname` 置空
- **THEN** 系统以业务错误 `USER_NICKNAME_EMPTY` 拒绝

#### Scenario: 跨企业资料隔离

- **WHEN** 同一账号在企业 A 设置头像后切换到企业 B
- **THEN** 企业 B 的 profile / permission-info 头像 MUST NOT 自动变成企业 A 的头像（除非 B 成员行本身有相同值）

### Requirement: 权限信息含头像

系统 MUST 在 `GET /admin-api/system/auth/get-permission-info` 响应中包含当前用户在**当前租户**的 `avatar` fileId，供会话恢复与工作台 UI 展示。

#### Scenario: permission info 返回 avatar

- **WHEN** 已认证用户请求 `GET /admin-api/system/auth/get-permission-info`
- **THEN** 响应 `avatar` / `nickname` 与当前租户 `sys_tenant_user` 成员资料一致（昵称可回退全局用户名）
