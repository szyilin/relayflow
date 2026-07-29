## MODIFIED Requirements

### Requirement: 基于角色的访问控制

系统 MUST 根据用户角色对管理端 API 实施 RBAC；鉴权链路 MUST 为 `user → user_role → role → role_permission → permission.code`。具备所需 `permission.code` 的管理员 MUST 对该权限所保护的管理端数据按业务规则全量可见，MUST NOT 再按角色「数据范围」裁切组织数据。员工工作台 `/app-api` 主导航与能力 MUST NOT 依赖本 RBAC 权限码（仍按有效组织成员准入）。

#### Scenario: 授权访问

- **WHEN** 用户拥有某 `permission.code` 对应权限
- **THEN** 调用受该权限保护的管理端端点成功（在其他业务校验通过的前提下）

#### Scenario: 禁止访问

- **WHEN** 用户不具备所需 `permission.code`
- **THEN** 系统返回 HTTP 403

### Requirement: 角色树与系统预置角色

系统 MUST 支持角色父子关系及系统预置角色；`code` MUST 仅作为系统角色技术锚点（自定义角色 MUST 为 NULL）。

#### Scenario: 一级角色无需上级

- **WHEN** 创建或更新角色且未指定上级（`parent_id` 为空或 `0`）
- **THEN** 系统将该角色持久化为一级角色
- **AND** MUST NOT 因缺少上级而拒绝

#### Scenario: 子角色权限子集

- **WHEN** 角色 B 的 `parent_id` 指向角色 A
- **THEN** 角色 B 绑定的权限点必须为角色 A 权限点的子集

#### Scenario: 超级管理员预置

- **WHEN** Flyway 执行 system 域种子或租户引导创建系统角色
- **THEN** 存在 `role_type=SYSTEM` 且 `code=super_admin` 的角色
- **AND** 该角色绑定 system 域全部预置权限点

#### Scenario: 自定义角色无 code

- **WHEN** 创建 `role_type=CUSTOM` 的角色
- **THEN** 持久化的 `code` MUST 为 NULL
- **AND** 管理端 API MUST NOT 要求客户端提交角色编码

### Requirement: 组织架构

系统 MUST 支持部门（组织树）及用户与部门的隶属关系；部门用于组织归属与通讯录结构，MUST NOT 单独替代 RBAC 功能权限，亦 MUST NOT 再作为角色级「数据范围」计算输入。

#### Scenario: 部门树

- **WHEN** 已认证管理员请求部门树
- **THEN** 返回当前租户内层级化的 `sys_dept` 列表
- **AND** 部门记录包含 `parent_id` 以表达上下级

#### Scenario: 用户多部门

- **WHEN** 用户归属多个部门
- **THEN** 通过 `sys_user_dept` 关联
- **AND** 至少一个部门可标记为主部门（`primary_flag`）

#### Scenario: 列出部门

- **GIVEN** 已认证的管理员用户
- **WHEN** 请求部门树
- **THEN** 系统返回当前租户内层级化的部门列表

#### Scenario: 租户根部门

- **WHEN** 租户初始化组织功能
- **THEN** 租户须有且仅有一个 `parent_id = 0` 的根部门
- **AND** 根部门展示名应与 `sys_tenant.name` 对齐（飞书式企业根节点）

#### Scenario: 根部门不可删除

- **WHEN** 管理员尝试删除根部门（`parent_id = 0`）
- **THEN** 系统拒绝该操作

### Requirement: 角色管理 API

系统 MUST 提供管理端 API 管理管理员角色、绑定权限点、查询/增减角色成员；各端点 MUST 以 `system:role:*`（成员变更可用 `system:role:update`）权限码保护。API MUST NOT 接受或依赖 `dataScope` / `deptIds`（角色部门范围）。创建/更新请求中 `parentId` 可选；自定义角色 MUST NOT 要求 `code`。

#### Scenario: 创建自定义角色并绑权限

- **WHEN** 具备 `system:role:create` 的用户提交含名称与权限 ID、可选上级的新角色
- **THEN** 持久化 `role_type=CUSTOM` 且 `code` 为 NULL 的角色
- **AND** 创建 `sys_role_permission` 关联

#### Scenario: 创建一级角色

- **WHEN** 创建请求省略上级或显式传空/`0`
- **THEN** 系统创建成功且 `parent_id` 表示一级

#### Scenario: 系统角色不可删除

- **WHEN** 用户尝试删除 `role_type=SYSTEM` 的角色
- **THEN** 系统拒绝操作

#### Scenario: 列出角色成员

- **WHEN** 具备 `system:role:query`（或 list）的用户请求某角色的成员列表
- **THEN** 系统返回绑定该角色的用户摘要（至少含 userId、展示名）

#### Scenario: 添加角色成员

- **WHEN** 具备更新权限的用户为某角色添加组织内用户
- **THEN** 系统写入 `sys_user_role`（幂等）

#### Scenario: 移除角色成员

- **WHEN** 具备更新权限的用户从某角色移除成员
- **THEN** 系统删除对应 `sys_user_role`

### Requirement: 用户写操作管理端 API

系统 MUST 提供管理端 API，支持按手机号邀请成员（管理员不可设置账号密码）、创建用户（含部门与角色分配，保留种子管理员场景）、查询详情、更新基本信息、更新成员状态、更新主部门与角色绑定；用户分页在调用者具备 `system:user:list` 时 MUST 返回当前租户成员（可按显式 `deptId` 等筛选），MUST NOT 再按角色 data_scope 过滤；分页响应 MUST 含 `memberStatus` 与 `mobile`。

#### Scenario: 按手机号邀请成员

- **WHEN** 管理员调用 `POST /admin-api/system/user/invite` 并提交有效手机号及可选组织字段
- **THEN** 系统创建或复用该手机号对应的全局 `sys_user`，且不接受管理员传入密码
- **AND** 创建 `sys_tenant_user`，状态为 `NOT_JOINED`，租户为**管理端 JWT 当前租户**（多租户模式）或默认租户（单租户模式）
- **AND** 在当前租户内分配主部门与角色
- **AND** 该成员在状态变为 `ACTIVE` 之前不得登录本租户

#### Scenario: 重复邀请已加入成员

- **WHEN** 手机号已属于当前租户现有成员
- **THEN** 系统以业务错误拒绝邀请

#### Scenario: 分页响应含成员状态

- **WHEN** 管理员查询 `GET /admin-api/system/user/page`
- **THEN** 每行包含 `memberStatus`（枚举名）与 `mobile`
- **AND** `memberStatus` 反映 `sys_tenant_user.status`，含 `NOT_JOINED` 与 `ACTIVE`

#### Scenario: 创建用户并分配部门与角色

- **WHEN** 具备 `system:user:create` 的管理员提交含部门与角色分配的有效创建请求
- **THEN** 系统创建 `sys_user`、`sys_tenant_user` 及相关关联行

#### Scenario: 创建用户未指定部门

- **WHEN** 管理员创建用户时未提供 `deptId`
- **THEN** 系统将租户根部门设为用户主部门（`sys_user_dept.primary_flag = 1`）

#### Scenario: 有效成员必有主部门

- **WHEN** 用户为当前租户的有效成员（`sys_tenant_user` 为允许登录状态）
- **THEN** 用户须至少有一条 `primary_flag = 1` 的 `sys_user_dept` 记录

#### Scenario: 禁止清空主部门

- **WHEN** 管理员更新用户主部门时传入 null 或缺失 `deptId`
- **THEN** 系统拒绝该请求

#### Scenario: 用户分页全量（有 list 权限）

- **WHEN** 具备 `system:user:list` 的用户请求用户分页 API且未携带缩小范围的筛选
- **THEN** 返回当前租户成员分页结果，MUST NOT 因调用者角色缺少历史 data_scope=ALL 而缩小结果集

#### Scenario: 用户分页按部门过滤

- **WHEN** 管理员请求 `GET /admin-api/system/user/page` 并携带 query `deptId`
- **THEN** 响应列表仅含主部门（`sys_user_dept.primary_flag = 1`）等于 `deptId` 的用户

## REMOVED Requirements

### Requirement: RBAC 数据范围

**Reason**: 产品改为单轴 RBAC（角色 + 功能权限）；管理员有权限即看全量后台组织数据；数据范围视为设计偏差。

**Migration**: 删除 `sys_role.data_scope`、`sys_role_dept` 及 `DataScopeHelper`；用户分页等调用方去掉范围过滤。开发期 `0.x` 可重置库，不做旧数据回填。
