## ADDED Requirements

### Requirement: 租户主数据

系统 SHALL 维护租户（tenant）主数据，作为全局数据隔离边界。

#### Scenario: 默认租户种子

- **WHEN** 应用首次执行 Flyway 迁移
- **THEN** 创建 `sys_tenant` 表
- **AND** 插入 `id=1`、`code=default` 的默认租户记录
- **AND** 默认租户不可被删除

#### Scenario: 租户基本字段

- **WHEN** 查询租户记录
- **THEN** 至少包含 `id`、`code`、`name`、`status`、创建时间
- **AND** `code` 在全局唯一

### Requirement: 用户租户关系

系统 SHALL 通过 `sys_tenant_user` 维护用户与租户的多对多关系，以支持将来一账号多组织。

#### Scenario: V1 单租户绑定

- **WHEN** V1 单租户模式下创建用户
- **THEN** 自动在 `sys_tenant_user` 中建立用户与默认租户（`id=1`）的关联
- **AND** 每个用户至少属于一个租户

### Requirement: 业务表租户字段

除租户元数据表外，所有业务表 MUST 包含 `tenant_id` 字段，且不得为空。

#### Scenario: 系统模块表

- **WHEN** 创建任意 `sys_` 前缀业务表（不含 `sys_tenant`、`sys_tenant_user`）
- **THEN** 表结构包含 `tenant_id BIGINT NOT NULL`
- **AND** 租户内唯一约束包含 `tenant_id`（如 `UNIQUE(tenant_id, username)`）

#### Scenario: 基础设施与 IM 模块表

- **WHEN** 创建 `infra_` 或 `im_` 前缀业务表
- **THEN** 表结构包含 `tenant_id BIGINT NOT NULL`

### Requirement: 租户上下文自动注入

系统 SHALL 通过框架层 TenantContext 与持久层插件，自动注入当前请求的租户标识，避免业务代码手动拼接。

#### Scenario: 单租户模式查询

- **WHEN** `relayflow.tenant.enabled=false` 且已认证用户访问业务 API
- **THEN** TenantContext 固定为默认租户 ID
- **AND** MyBatis 查询与写入自动附加 `tenant_id` 条件或填充

#### Scenario: JWT 携带租户

- **WHEN** 系统签发 JWT 访问令牌
- **THEN** payload 包含 `tenant_id` claim
- **AND** V1 单租户模式下其值固定为默认租户 ID

### Requirement: 跨存储租户隔离

Redis 缓存与 MinIO 对象存储的 key 路径 MUST 包含租户维度，防止跨租户数据混读。

#### Scenario: Redis 缓存 key

- **WHEN** 写入 Redis 缓存
- **THEN** key 包含当前 `tenant_id` 前缀（如 `t:{tenantId}:...`）

#### Scenario: MinIO 对象路径

- **WHEN** 上传文件至对象存储
- **THEN** 对象 key 包含租户路径前缀（如 `tenant/{tenantId}/...`）

### Requirement: WebSocket 租户绑定

WebSocket 连接 MUST 绑定租户上下文，消息投递不得跨越租户边界。

#### Scenario: 握手绑定租户

- **WHEN** 客户端携带有效 JWT 连接 `/infra/ws`
- **THEN** 会话绑定 JWT 中的 `tenant_id`
- **AND** 后续消息路由与在线状态均限定在该租户内

## MODIFIED Requirements

### Requirement: JWT 用户认证

系统 SHALL 通过用户名与密码认证用户，并签发 JWT 供后续 API 请求使用；JWT MUST 包含当前租户标识 `tenant_id`。

#### Scenario: 登录成功

- **WHEN** 一名已注册且状态正常的用户向 `/admin-api/system/auth/login` 提交有效凭据
- **THEN** 系统返回 JWT 访问令牌
- **AND** 令牌 payload 包含 `tenant_id`（V1 单租户模式下为默认租户 ID）
- **AND** 后续携带 `Authorization: Bearer <token>` 的请求被接受

#### Scenario: 凭据无效

- **WHEN** 用户提交错误密码的登录凭据
- **THEN** 系统以未授权错误拒绝请求
- **AND** 不签发令牌
