# 系统模块规格（system）

## Purpose

定义组织权限、认证与系统管理相关行为。

## Requirements

### 需求：JWT 用户认证

系统应通过用户名与密码认证用户，并签发 JWT 供后续 API 请求使用；JWT MUST 包含当前租户标识 `tenant_id`。

#### 场景：登录成功

- 给定 一名已注册且拥有 ACTIVE 租户成员关系的用户
- 当 向 `/admin-api/system/auth/login` 提交有效凭据
- 那么 系统返回 JWT 访问令牌
- 并且 令牌 payload 包含活跃租户的 `tenant_id`（多租户模式）或默认租户 ID（单租户模式）
- 并且 后续携带 `Authorization: Bearer <token>` 的请求被接受

#### 场景：凭据无效

- 给定 一次密码错误的登录尝试
- 当 用户提交凭据
- 那么 系统以未授权错误拒绝请求
- 并且 不签发令牌

### 需求：多租户登录与企业选择

当 `relayflow.tenant.enabled=true` 时，系统 SHALL 以全局凭据认证用户，并将会话绑定至一个 ACTIVE 租户成员关系。

#### 场景：单企业登录

- 当 用户提交有效凭据且仅有一个 ACTIVE 的 `sys_tenant_user`
- 那么 系统返回含该成员关系 `tenant_id` 的 JWT

#### 场景：多企业须选择

- 当 用户提交有效凭据且有多个 ACTIVE 成员关系，且请求未携带 `tenantId`
- 那么 系统返回业务错误 `TENANT_SELECTION_REQUIRED`
- 并且 响应包含可选企业列表（`tenantId`、`tenantName`）

#### 场景：指定企业登录

- 当 用户提交有效凭据及对其 ACTIVE 成员关系有效的 `tenantId`
- 那么 系统返回含该 `tenant_id` 的 JWT

#### 场景：手机号登录

- 当 登录标识匹配 `sys_user.mobile` 而非 `username`
- 那么 认证结果与用户名登录等价

#### 场景：无 ACTIVE 企业

- 当 凭据有效但用户无任何 ACTIVE 租户成员关系
- 那么 系统返回业务错误 `AUTH_NO_TENANT`

### 需求：开放账号注册

当 `relayflow.tenant.enabled=true` 且 `relayflow.tenant.allow-open-register=true` 时，系统 SHALL 暴露公开的用户端注册 API，创建全局用户、用户拥有的新租户及 ACTIVE 成员关系。

#### 场景：新手机号注册建企

- 当 客户端调用 `POST /app-api/system/auth/register`，提交唯一手机号、有效密码（≥6 字符）、昵称与 `tenantName`
- 那么 系统创建 `sys_user`（`username` 等于手机号）及 BCrypt 密码 hash
- 并且 创建 `sys_tenant`（`name=tenantName`，`owner_user_id` 为新用户）
- 并且 创建状态为 `ACTIVE` 的 `sys_tenant_user`
- 并且 引导最小租户结构（根部门、owner 主部门、`super_admin` 绑定）
- 并且 返回新租户的 `accessToken` 与 `tenantId`
- 并且 端点不要求认证

#### 场景：重复手机号拒绝

- 当 手机号已绑定且用户已设置登录密码
- 那么 系统返回业务错误 `USER_MOBILE_EXISTS`

#### 场景：弱密码拒绝

- 当 密码缺失或少于 6 个字符
- 那么 系统返回业务错误 `AUTH_REGISTER_PASSWORD_WEAK`

#### 场景：注册激活其他企业待加入邀请

- 当 该手机号在其他租户存在管理员邀请产生的 `NOT_JOINED` 成员关系
- 那么 系统设置用户密码
- 并且 将所有此类 `sys_tenant_user` 转为 `ACTIVE`
- 并且 仍按 `tenantName` 创建用户自有新租户
- 并且 签发的 JWT 以新创建租户为活跃租户

#### 场景：开放注册关闭

- 当 `relayflow.tenant.allow-open-register=false`
- 那么 `POST /app-api/system/auth/register` 被拒绝或未注册

### 需求：企业切换

系统 SHALL 允许已认证用户切换活跃租户并获取新 JWT。

#### 场景：列出我的企业

- 当 已认证用户调用 `GET /app-api/system/tenant/my-list`
- 那么 响应列出用户所有 `ACTIVE` 成员关系的企业
- 并且 每项含 `tenantId`、`tenantName` 及是否为 `owner`

#### 场景：切换企业

- 当 已认证用户调用 `POST /app-api/system/tenant/switch` 并提交其 ACTIVE 成员关系的 `tenantId`
- 那么 系统返回含更新 `tenant_id` 的新 `accessToken`
- 并且 后续 API 使用新租户上下文

#### 场景：切换至无权限企业

- 当 用户非请求企业的 ACTIVE 成员
- 那么 系统返回业务错误 `TENANT_SWITCH_FORBIDDEN`

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

### 需求：租户主数据

系统 SHALL 维护租户（tenant）主数据，作为全局数据隔离边界。

#### 场景：默认租户种子

- 当 应用首次执行 Flyway 迁移
- 那么 创建 `sys_tenant` 表
- 并且 插入 `id=1`、`code=default` 的默认租户记录
- 并且 默认租户不可被删除
- 并且 多租户模式下用户注册建企会创建**额外**租户，不替换种子租户

### 需求：用户租户关系

系统 SHALL 通过 `sys_tenant_user` 维护用户与租户的多对多关系，以支持将来一账号多组织。

#### 场景：V1 单租户绑定

- 当 V1 单租户模式下创建用户
- 那么 自动在 `sys_tenant_user` 中建立用户与默认租户（`id=1`）的关联
- 并且 每个用户至少属于一个租户

### 需求：业务表租户字段

除租户元数据表外，所有业务表 MUST 包含 `tenant_id` 字段，且不得为空；codegen 生成的 infra/im DO MUST 继承 `TenantBaseDO`。

#### 场景：系统模块表

- 当 创建任意 `sys_` 前缀业务表（不含 `sys_tenant`、`sys_tenant_user`）
- 那么 表结构包含 `tenant_id BIGINT NOT NULL`
- 并且 租户内唯一约束包含 `tenant_id`（如 `UNIQUE(tenant_id, username)`）

#### 场景：基础设施与 IM 模块表

- 当 创建 `infra_` 或 `im_` 前缀业务表
- 那么 表结构包含 `tenant_id BIGINT NOT NULL`
- 并且 对应 DO 继承 `TenantBaseDO`

### 需求：租户上下文自动注入

系统 SHALL 通过框架层 TenantContext 与持久层插件，自动注入当前请求的租户标识，避免业务代码手动拼接。

#### 场景：单租户模式查询

- 当 `relayflow.tenant.enabled=false` 且已认证用户访问业务 API
- 那么 TenantContext 固定为默认租户 ID
- 并且 MyBatis 查询与写入自动附加 `tenant_id` 条件或填充
- 并且 忽略 JWT 中的 `tenant_id` claim

#### 场景：多租户模式 JWT 驱动

- 当 `relayflow.tenant.enabled=true` 且请求携带有效 JWT
- 那么 `TenantContextHolder` 从 JWT `tenant_id` 设置当前租户
- 并且 租户范围 SQL 使用该 tenant id

#### 场景：租户上下文切换隔离

- 当 TenantContext 设为租户 A 或 B
- 那么 MyBatis 租户插件使用对应 `tenant_id`
- 并且 不同租户上下文下的查询条件互不影响（单元测试可断言）

### 需求：跨存储租户隔离

Redis 缓存 key MUST 包含租户维度，防止跨租户数据混读。

#### 场景：Redis 缓存 key

- 当 写入 Redis 缓存（权限、token 吊销、WS online/fanout 等）
- 那么 key 使用 `t:{tenantId}:...` 格式（经 `TenantRedisKeyBuilder`）

### 需求：默认租户保护

系统 SHALL 防止删除种子默认租户（配置项 `relayflow.tenant.default-id`，默认 `1`）。

#### 场景：断言默认租户不可删除

- 当 应用代码对默认租户 ID 调用删除保护
- 那么 系统以业务错误拒绝
- 并且 默认租户记录保留

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

#### 场景：租户根部门

- 当 租户初始化组织功能
- 那么 租户须有且仅有一个 `parent_id = 0` 的根部门
- 并且 根部门展示名应与 `sys_tenant.name` 对齐（飞书式企业根节点）

#### 场景：根部门不可删除

- 当 管理员尝试删除根部门（`parent_id = 0`）
- 那么 系统拒绝该操作

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

#### 场景：用户分页缺少 list 权限

- 当 已认证用户不具备 `system:user:list` 调用 `GET /admin-api/system/user/page`
- 那么 系统返回 HTTP 403

#### 场景：用户分页具备 list 权限

- 当 用户 `admin` 具备 `system:user:list` 调用 `GET /admin-api/system/user/page`
- 那么 请求成功返回 HTTP 200 且 `code=0`

### 需求：认证请求加载权限

系统须在每次已认证的管理端请求中，从数据库加载当前用户有效 `permission.code` 并写入 Security 上下文（authorities 等于 `sys_permission.code`）。

#### 场景：超级管理员加载种子权限

- 当 用户 `admin` 使用有效 JWT 认证
- 那么 Security 上下文 authorities 包含 `system:user:list`
- 并且 包含绑定至 `super_admin` 的其他 system 域预置权限码

### 需求：权限信息 API

系统须提供已认证端点，供当前用户获取角色与权限码列表，用于前端菜单与按钮门禁。

#### 场景：获取权限信息

- 当 客户端携带有效 Bearer JWT 请求 `GET /admin-api/system/auth/get-permission-info`
- 那么 响应 `code` 为 0
- 并且 `data.permissions` 为 `permission.code` 字符串数组
- 并且 `data.isAdmin` 为布尔值，与权限集合是否非空一致
- 并且 `data` 包含基本用户身份信息

#### 场景：超级管理员 permission info

- 当 客户端携带有效 Bearer token 调用 `get-permission-info`
- 那么 HTTP 200 且 `code=0`
- 并且 `data.permissions` 对 `super_admin` 为非空数组
- 并且 `data.isAdmin` 为 true

#### 场景：未认证 permission info

- 当 客户端未携带 token 调用 `get-permission-info`
- 那么 系统返回 HTTP 401

#### 场景：普通成员 permission info

- 当 非管理身份的有效组织成员携带有效 Bearer JWT 调用 `get-permission-info`
- 那么 HTTP 200 且 `code=0`
- 并且 `data.isAdmin` 为 false
- 并且 `data.permissions` 为空数组

### 需求：管理身份判定

系统须将 **管理身份（isAdmin）** 定义为：在当前租户下，用户经 `sys_user_role → sys_role → sys_role_permission → sys_permission` 解析得到的有效 `permission.code` 集合**非空**。

#### 场景：超级管理员为管理身份

- 当 用户 `admin` 在默认租户下绑定 `super_admin` 且该角色有关联 permission
- 那么 该用户的 `isAdmin` 为 true

#### 场景：无角色非管理身份

- 当 用户已登录且在当前租户下未绑定任何角色
- 那么 该用户的 `isAdmin` 为 false

#### 场景：零权限角色非管理身份

- 当 用户绑定了 `sys_role` 但该角色未关联任何 `sys_permission`
- 那么 该用户的 `isAdmin` 为 false

### 需求：管理面 API 门户准入

系统须对 `/admin-api/**` 实施门户层鉴权：已认证但 `isAdmin=false`（security authorities 为空）的用户不得访问除明确白名单外的管理端 API。

#### 场景：非管理员调用受保护管理 API

- 当 已认证但 authorities 为空的用户调用 `GET /admin-api/system/user/page`
- 那么 系统返回 HTTP 403

#### 场景：非管理员仍可获取 permission info

- 当 已认证但 authorities 为空的用户调用 `GET /admin-api/system/auth/get-permission-info`
- 那么 请求成功返回 HTTP 200 且 `code=0`

#### 场景：非管理员登录与租户默认信息白名单

- 当 未认证用户调用 `POST /admin-api/system/auth/login` 或 `GET /admin-api/system/tenant/default`
- 那么 请求按现有 permitAll 规则处理

#### 场景：管理员调用管理 API

- 当 已认证且 authorities 包含 `system:user:list` 的用户调用 `GET /admin-api/system/user/page`
- 那么 请求按既有 RBAC 规则处理（HTTP 200 且 `code=0` 当 permission 满足）

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

系统须提供管理端 API，支持按手机号邀请成员（管理员不可设置账号密码）、创建用户（含部门与角色分配，保留种子管理员场景）、查询详情、更新基本信息、更新成员状态、更新主部门与角色绑定；用户分页须按调用者有效 data_scope 过滤；分页响应须含 `memberStatus` 与 `mobile`。

#### 场景：按手机号邀请成员

- 当 管理员调用 `POST /admin-api/system/user/invite` 并提交有效手机号及可选组织字段
- 那么 系统创建或复用该手机号对应的全局 `sys_user`，且不接受管理员传入密码
- 并且 创建 `sys_tenant_user`，状态为 `NOT_JOINED`，租户为**管理端 JWT 当前租户**（多租户模式）或默认租户（单租户模式）
- 并且 在当前租户内分配主部门与角色
- 并且 该成员在状态变为 `ACTIVE` 之前不得登录本租户

#### 场景：重复邀请已加入成员

- 当 手机号已属于当前租户现有成员
- 那么 系统以业务错误拒绝邀请

#### 场景：分页响应含成员状态

- 当 管理员查询 `GET /admin-api/system/user/page`
- 那么 每行包含 `memberStatus`（枚举名）与 `mobile`
- 并且 `memberStatus` 反映 `sys_tenant_user.status`，含 `NOT_JOINED` 与 `ACTIVE`

#### 场景：创建用户并分配部门与角色

- 当 具备 `system:user:create` 的管理员提交含部门与角色分配的有效创建请求
- 那么 系统创建 `sys_user`、`sys_tenant_user` 及相关关联行

#### 场景：创建用户未指定部门

- 当 管理员创建用户时未提供 `deptId`
- 那么 系统将租户根部门设为用户主部门（`sys_user_dept.primary_flag = 1`）

#### 场景：有效成员必有主部门

- 当 用户为当前租户的有效成员（`sys_tenant_user` 为允许登录状态）
- 那么 用户须至少有一条 `primary_flag = 1` 的 `sys_user_dept` 记录

#### 场景：禁止清空主部门

- 当 管理员更新用户主部门时传入 null 或缺失 `deptId`
- 那么 系统拒绝该请求

#### 场景：用户分页按 data_scope 过滤

- 当 具备 `system:user:list` 且 data_scope 非 ALL 的用户请求用户分页 API
- 那么 返回用户须限制在其有效数据范围内（SELF 与允许 deptIds 并集，或 ALL 时全量）

#### 场景：用户分页按部门过滤

- 当 管理员请求 `GET /admin-api/system/user/page` 并携带 query `deptId`
- 那么 响应列表仅含主部门（`sys_user_dept.primary_flag = 1`）等于 `deptId` 的用户
- 并且 结果仍受调用者 data_scope 规则约束

### 需求：工作台组织目录 API

系统须向已认证的工作台成员暴露只读 `/app-api/system/` 端点，用于浏览租户组织目录；**不**要求 `sys_permission`。

#### 场景：工作台部门树

- 当 已认证工作台成员请求 `GET /app-api/system/dept/tree`
- 那么 系统返回当前租户部门层级供导航
- 并且 调用者无需持有任何 `sys_permission` 权限码

#### 场景：按部门列出成员

- 当 已认证工作台成员请求 `GET /app-api/system/user/list-by-dept` 并携带有效 `deptId`
- 那么 系统返回主部门等于 `deptId` 的用户
- 并且 每项含名片所需字段（如 id、昵称、部门名、头像字）
