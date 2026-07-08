## ADDED Requirements

### Requirement: 管理身份判定

系统 SHALL 将 **管理身份（isAdmin）** 定义为：在当前租户下，用户经 `sys_user_role → sys_role → sys_role_permission → sys_permission` 解析得到的有效 `permission.code` 集合**非空**。

#### Scenario: 超级管理员为管理身份

- **WHEN** 用户 `admin` 在默认租户下绑定 `super_admin` 且该角色有关联 permission
- **THEN** 该用户的 `isAdmin` 为 true

#### Scenario: 无角色非管理身份

- **WHEN** 用户已登录且在当前租户下未绑定任何角色
- **THEN** 该用户的 `isAdmin` 为 false

#### Scenario: 零权限角色非管理身份

- **WHEN** 用户绑定了 `sys_role` 但该角色未关联任何 `sys_permission`
- **THEN** 该用户的 `isAdmin` 为 false

### Requirement: 权限信息 API 返回管理身份

`GET /admin-api/system/auth/get-permission-info` SHALL 在响应 `data` 中包含布尔字段 `isAdmin`，与 §管理身份判定 一致。

#### Scenario: 管理员获取 permission info

- **WHEN** 管理身份用户携带有效 Bearer JWT 调用 `get-permission-info`
- **THEN** HTTP 200 且 `code=0`
- **AND** `data.isAdmin` 为 true
- **AND** `data.permissions` 为非空数组

#### Scenario: 普通成员获取 permission info

- **WHEN** 非管理身份的有效组织成员携带有效 Bearer JWT 调用 `get-permission-info`
- **THEN** HTTP 200 且 `code=0`
- **AND** `data.isAdmin` 为 false
- **AND** `data.permissions` 为空数组

### Requirement: 管理面 API 门户准入

系统 SHALL 对 `/admin-api/**` 实施门户层鉴权：已认证但 `isAdmin=false`（security authorities 为空）的用户 MUST NOT 访问除明确白名单外的管理端 API。

#### Scenario: 非管理员调用受保护管理 API

- **WHEN** 已认证但 authorities 为空的用户调用 `GET /admin-api/system/user/page`
- **THEN** 系统返回 HTTP 403

#### Scenario: 非管理员仍可获取 permission info

- **WHEN** 已认证但 authorities 为空的用户调用 `GET /admin-api/system/auth/get-permission-info`
- **THEN** 请求成功返回 HTTP 200 且 `code=0`

#### Scenario: 非管理员仍可登录与读租户默认信息

- **WHEN** 未认证用户调用 `POST /admin-api/system/auth/login`
- **THEN** 请求按现有登录规则处理（permitAll）

#### Scenario: 管理员调用管理 API

- **WHEN** 已认证且 authorities 包含 `system:user:list` 的用户调用 `GET /admin-api/system/user/page`
- **THEN** 请求按既有 RBAC 规则处理（HTTP 200 且 `code=0` 当 permission 满足）

## MODIFIED Requirements

### Requirement: 权限信息 API

系统 **SHALL** 提供已认证端点，供当前用户获取角色、权限码列表及 **管理身份**，用于前端菜单、门户与按钮门禁。

#### Scenario: 获取权限信息

- **WHEN** 客户端携带有效 Bearer JWT 请求 `GET /admin-api/system/auth/get-permission-info`
- **THEN** 响应 `code` 为 0
- **AND** `data.permissions` 为 `permission.code` 字符串数组
- **AND** `data.isAdmin` 为布尔值，与权限集合是否非空一致
- **AND** `data` 包含基本用户身份信息
