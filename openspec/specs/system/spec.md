# 系统模块规格（system）

## 目的

定义组织权限、认证与系统管理相关行为。

## 需求

### 需求：JWT 用户认证

系统应通过用户名与密码认证用户，并签发 JWT 供后续 API 请求使用。

#### 场景：登录成功

- 给定 一名已注册且状态正常的用户
- 当 向 `/admin-api/system/auth/login` 提交有效凭据
- 那么 系统返回 JWT 访问令牌
- 并且 后续携带 `Authorization: Bearer <token>` 的请求被接受

#### 场景：凭据无效

- 给定 一次密码错误的登录尝试
- 当 用户提交凭据
- 那么 系统以未授权错误拒绝请求
- 并且 不签发令牌

### 需求：全局用户账号

系统应维护全局用户账号（`sys_user`），用于跨租户登录标识与凭据存储；成员生命周期状态不得存储于该表。

#### 场景：全局账号字段

- 当 创建 `sys_user` 表
- 那么 至少包含登录标识（如用户名/手机/邮箱）、密码 hash、昵称等
- 并且 不包含 `tenant_id`
- 并且 不包含成员生命周期 status（如 ACTIVE、LEFT）

#### 场景：一账号多企业

- 当 同一 `sys_user` 关联多个租户
- 那么 通过多条 `sys_tenant_user` 记录表达
- 并且 各租户成员状态可独立（例如 A 企业 ACTIVE、B 企业 LEFT）

### 需求：租户成员生命周期状态

系统应在 `sys_tenant_user` 上维护成员在该租户内的生命周期状态，用于登录与成员管理。

#### 场景：状态枚举

- 当 查询租户成员关系
- 那么 `status` 至少支持：`NOT_JOINED`、`PENDING_ACTIVATION`、`ACTIVE`、`SUSPENDED`、`PENDING_LEAVE`、`LEFT`

#### 场景：登录校验成员状态

- 当 用户尝试登录某一租户
- 那么 除校验全局凭据外，须校验对应 `sys_tenant_user.status` 为允许登录的状态（V1 默认仅 `ACTIVE`）
- 并且 `SUSPENDED` 或 `LEFT` 状态必须拒绝登录该租户

### 需求：基于角色的访问控制

系统应根据用户角色对管理端与用户端 API 实施 RBAC；鉴权链路必须为 `user → user_role → role → role_permission → permission.code`，并结合角色的 `data_scope` 过滤组织数据。

#### 场景：授权访问

- 当 用户拥有某 `permission.code` 对应权限且角色数据范围覆盖目标数据
- 那么 调用受保护的管理端端点成功

#### 场景：禁止访问

- 当 用户不具备所需 `permission.code` 或数据范围不覆盖目标数据
- 那么 系统返回 HTTP 403

### 需求：RBAC 功能权限

系统应通过权限点（`sys_permission`）与角色-权限关联（`sys_role_permission`）实施 API 级功能鉴权；菜单表不得作为鉴权真源。

#### 场景：权限点树

- 当 定义管理端能力
- 那么 以树形 `sys_permission` 存储，`code` 租户内唯一
- 并且 角色通过 `sys_role_permission` 绑定权限点

#### 场景：菜单与权限解耦

- 当 渲染管理端菜单
- 那么 `sys_menu` 负责 UI 导航结构
- 并且 菜单可见性基于用户是否拥有关联的 `permission`（可选 `permission_id` 链接）
- 并且 API 鉴权仅依据 `permission.code`，不依据菜单 ID

### 需求：RBAC 数据范围

系统应在角色上配置数据范围，限制管理员可操作的组织数据边界。

#### 场景：数据范围类型

- 当 配置 `sys_role.data_scope`
- 那么 至少支持：`ALL`、`DEPT`、`DEPT_AND_CHILD`、`SELF`、`CUSTOM`

#### 场景：自定义部门范围

- 当 `data_scope` 为 `CUSTOM`
- 那么 通过 `sys_role_dept` 指定可管理的部门 ID 列表

#### 场景：同角色共享范围

- 当 多名用户绑定同一 `sys_role`
- 那么 这些用户共享该角色的 `data_scope` 与 `sys_role_dept` 配置

### 需求：角色树与系统预置角色

系统应支持角色父子关系及系统预置角色。

#### 场景：子角色权限子集

- 当 角色 B 的 `parent_id` 指向角色 A
- 那么 角色 B 绑定的权限点必须为角色 A 权限点的子集

#### 场景：超级管理员预置

- 当 Flyway 执行 system 域种子
- 那么 存在 `role_type=SYSTEM` 且 `code=super_admin` 的角色
- 并且 其 `data_scope=ALL` 并绑定 system 域全部预置权限点

### 需求：组织架构

系统应支持部门（组织树）及用户与部门的隶属关系；部门用于组织归属与数据范围计算，不单独替代 RBAC 功能权限。

#### 场景：部门树

- 当 已认证管理员请求部门树
- 那么 返回当前租户内层级化的 `sys_dept` 列表
- 并且 部门记录包含 `parent_id` 以表达上下级

#### 场景：用户多部门

- 当 用户归属多个部门
- 那么 通过 `sys_user_dept` 关联
- 并且 至少一个部门可标记为主部门（`primary_flag`）

#### 场景：列出部门

- 给定 已认证的管理员用户
- 当 请求部门树
- 那么 系统返回当前租户内层级化的部门列表

### 需求：系统表前缀

系统模块全部数据表必须使用 `sys_` 前缀。

#### 场景：表命名

- 给定 新建一张系统模块表
- 当 应用迁移脚本
- 那么 表名以 `sys_` 开头

### 需求：默认租户查询 API

系统应提供匿名可访问的管理端接口，返回 V1 默认租户信息供壳层展示。

#### 场景：查询默认租户成功

- 当 客户端请求 `GET /admin-api/system/tenant/default`
- 那么 响应 `code` 为 0
- 并且 `data` 包含 `id`、`code`、`name`、`status`、`createTime`

#### 场景：未携带 JWT 可访问

- 当 请求未带 `Authorization` 头
- 那么 仍返回成功结果（permitAll）

### 需求：API 模块拆分

系统模块必须拆分为 `relayflow-module-system-api` 与 `relayflow-module-system-biz`。

#### 场景：跨模块依赖

- 给定 其他模块需要用户信息
- 当 声明 Maven 依赖
- 那么 仅依赖 `relayflow-module-system-api`
- 并且 不依赖 `relayflow-module-system-biz`

### 需求：RBAC 运行时 API 鉴权

系统须通过 `permission.code` 对管理端 API 实施运行时鉴权；仅 JWT 认证不得访问受权限保护的端点。

#### 场景：授权 API 调用

- 当 已认证用户调用要求 `system:user:list` 的管理端端点
- 并且 其有效权限集包含 `system:user:list`
- 那么 请求成功返回 HTTP 200 且 `code=0`

#### 场景：缺少权限

- 当 已认证用户调用要求 `system:dept:create` 的端点
- 并且 其有效权限集不包含该 code
- 那么 系统返回 HTTP 403

### 需求：权限信息 API

系统须提供已认证端点，供当前用户获取角色与权限码列表，用于前端菜单与按钮门禁。

#### 场景：获取权限信息

- 当 客户端携带有效 Bearer JWT 请求 `GET /admin-api/system/auth/get-permission-info`
- 那么 响应 `code` 为 0
- 并且 `data.permissions` 为 `permission.code` 字符串数组
- 并且 `data` 包含基本用户身份信息

### 需求：部门管理 API

系统须提供管理端 API，在当前租户内列出、创建、更新、删除部门；各端点须以 `system:dept:*` 权限码保护。

#### 场景：列出部门

- 当 具备 `system:dept:list` 的用户请求部门列表 API
- 那么 返回当前租户内可组树的部门扁平列表

#### 场景：删除含子部门或用户的部门

- 当 用户尝试删除仍有子部门或关联用户的部门
- 那么 系统以业务错误拒绝操作

### 需求：角色管理 API

系统须提供管理端 API 管理角色、绑定权限点、配置 `data_scope`（CUSTOM 时含 `sys_role_dept`）；各端点须以 `system:role:*` 权限码保护。

#### 场景：创建自定义角色并绑权限

- 当 具备 `system:role:create` 的用户提交含权限 ID 的新角色
- 那么 持久化 `role_type=CUSTOM` 的角色
- 并且 创建 `sys_role_permission` 关联

#### 场景：系统角色不可删除

- 当 用户尝试删除 `role_type=SYSTEM` 的角色
- 那么 系统拒绝操作

### 需求：用户写操作管理端 API

系统须提供管理端 API，支持创建用户（含部门与角色分配）、查询详情、更新基本信息、更新成员状态、更新主部门与角色绑定；用户分页须按调用者有效 data_scope 过滤。

#### 场景：创建用户并分配部门与角色

- 当 具备 `system:user:create` 的管理员提交含部门与角色分配的有效创建请求
- 那么 系统创建 `sys_user`、`sys_tenant_user` 及相关关联行

#### 场景：用户分页按 data_scope 过滤

- 当 具备 `system:user:list` 且 data_scope 非 ALL 的用户请求用户分页 API
- 那么 返回用户须限制在其有效数据范围内（SELF 与允许 deptIds 并集，或 ALL 时全量）
