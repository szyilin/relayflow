## ADDED Requirements

### Requirement: 全局用户账号

系统 SHALL 维护全局用户账号（`sys_user`），用于跨租户登录标识与凭据存储；成员生命周期状态 MUST NOT 存储于该表。

#### Scenario: 全局账号字段

- **WHEN** 创建 `sys_user` 表
- **THEN** 至少包含登录标识（如用户名/手机/邮箱）、密码 hash、昵称等
- **AND** 不包含 `tenant_id`
- **AND** 不包含成员生命周期 status（如 ACTIVE、LEFT）

#### Scenario: 一账号多企业

- **WHEN** 同一 `sys_user` 关联多个租户
- **THEN** 通过多条 `sys_tenant_user` 记录表达
- **AND** 各租户成员状态可独立（例如 A 企业 ACTIVE、B 企业 LEFT）

### Requirement: 租户成员生命周期状态

系统 SHALL 在 `sys_tenant_user` 上维护成员在该租户内的生命周期状态，用于登录与成员管理。

#### Scenario: 状态枚举

- **WHEN** 查询租户成员关系
- **THEN** `status` 至少支持：`NOT_JOINED`、`PENDING_ACTIVATION`、`ACTIVE`、`SUSPENDED`、`PENDING_LEAVE`、`LEFT`

#### Scenario: 登录校验成员状态

- **WHEN** 用户尝试登录某一租户
- **THEN** 除校验全局凭据外，须校验对应 `sys_tenant_user.status` 为允许登录的状态（V1 默认仅 `ACTIVE`）
- **AND** `SUSPENDED` 或 `LEFT` 状态 MUST 拒绝登录该租户

### Requirement: 部门组织树

系统 SHALL 支持租户内的部门层级结构及用户与部门的多对多隶属关系。

#### Scenario: 部门树

- **WHEN** 已认证管理员请求部门树
- **THEN** 返回当前租户内层级化的 `sys_dept` 列表
- **AND** 部门记录包含 `parent_id` 以表达上下级

#### Scenario: 用户多部门

- **WHEN** 用户归属多个部门
- **THEN** 通过 `sys_user_dept` 关联
- **AND** 至少一个部门可标记为主部门（`primary_flag`）

### Requirement: RBAC 功能权限

系统 SHALL 通过权限点（`sys_permission`）与角色-权限关联（`sys_role_permission`）实施 API 级功能鉴权；菜单表 MUST NOT 作为鉴权真源。

#### Scenario: 权限点树

- **WHEN** 定义管理端能力
- **THEN** 以树形 `sys_permission` 存储，`code` 全局唯一（租户内或全局按设计）
- **AND** 角色通过 `sys_role_permission` 绑定权限点

#### Scenario: 菜单与权限解耦

- **WHEN** 渲染管理端菜单
- **THEN** `sys_menu` 负责 UI 导航结构
- **AND** 菜单可见性基于用户是否拥有关联的 `permission`（可选 `permission_id` 链接）
- **AND** API 鉴权仅依据 `permission.code`，不依据菜单 ID

### Requirement: RBAC 数据范围

系统 SHALL 在角色上配置数据范围，限制管理员可操作的组织数据边界。

#### Scenario: 数据范围类型

- **WHEN** 配置 `sys_role.data_scope`
- **THEN** 至少支持：`ALL`、`DEPT`、`DEPT_AND_CHILD`、`SELF`、`CUSTOM`

#### Scenario: 自定义部门范围

- **WHEN** `data_scope` 为 `CUSTOM`
- **THEN** 通过 `sys_role_dept` 指定可管理的部门 ID 列表

#### Scenario: 同角色共享范围

- **WHEN** 多名用户绑定同一 `sys_role`
- **THEN** 这些用户共享该角色的 `data_scope` 与 `sys_role_dept` 配置

### Requirement: 角色树与系统预置角色

系统 SHALL 支持角色父子关系及系统预置角色。

#### Scenario: 子角色权限子集

- **WHEN** 角色 B 的 `parent_id` 指向角色 A
- **THEN** 角色 B 绑定的权限点 MUST 为角色 A 权限点的子集

#### Scenario: 超级管理员预置

- **WHEN** Flyway 执行 system 域种子
- **THEN** 存在 `role_type=SYSTEM` 且 `code=super_admin` 的角色
- **AND** 其 `data_scope=ALL` 并绑定 system 域全部预置权限点

## MODIFIED Requirements

### Requirement: 基于角色的访问控制

系统 SHALL 根据用户角色对管理端与用户端 API 实施 RBAC；鉴权链路 MUST 为 `user → user_role → role → role_permission → permission.code`，并结合角色的 `data_scope` 过滤组织数据。

#### Scenario: 授权访问

- **WHEN** 用户拥有某 `permission.code` 对应权限且角色数据范围覆盖目标数据
- **THEN** 调用受保护的管理端端点成功

#### Scenario: 禁止访问

- **WHEN** 用户不具备所需 `permission.code` 或数据范围不覆盖目标数据
- **THEN** 系统返回 HTTP 403

### Requirement: 组织架构

系统 SHALL 支持部门（组织树）及用户与部门的隶属关系；部门用于组织归属与数据范围计算，不单独替代 RBAC 功能权限。

#### Scenario: 列出部门

- **WHEN** 已认证的管理员用户请求部门树
- **THEN** 系统返回当前租户内层级化的部门列表
