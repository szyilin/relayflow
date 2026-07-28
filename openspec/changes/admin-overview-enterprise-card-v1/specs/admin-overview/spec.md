## ADDED Requirements

### Requirement: Admin overview enterprise identity card

管理端 `/admin` 概览页 MUST 在「概览统计」位置展示当前租户企业身份卡：企业头像（无则名称首字占位）、企业名称、只读企业编号，以及四项组织指标。指标数字 MUST 来自服务端统计，不得写死。

#### Scenario: Overview card renders identity and metrics

- **WHEN** 已登录管理员打开 `/admin` 且具备 `system:enterprise:query`
- **THEN** 页面展示企业头像/名称/编号与四项计数（组织总人数、部门数、超级管理员、子管理员）

#### Scenario: Metric navigation

- **WHEN** 用户点击「组织总人数」
- **THEN** 导航至 `/admin/system/user`
- **WHEN** 用户点击「部门数」
- **THEN** 导航至 `/admin/system/dept`
- **WHEN** 用户点击「超级管理员」或「子管理员」
- **THEN** 导航至 `/admin/system/role`

### Requirement: Admin overview stats API

系统 MUST 提供 `GET /admin-api/system/enterprise/overview`，鉴权为 JWT + 管理身份 + `system:enterprise:query`，响应 `code=0` 时 `data` 含：`code`、`name`、`logoFileId`（可空）、`memberCount`、`deptCount`、`superAdminCount`、`subAdminCount`（均为非负整数）。

#### Scenario: Overview returns live counts

- **WHEN** 管理员调用 overview API
- **THEN** `memberCount` 为当前租户 ACTIVE 成员数
- **AND** `deptCount` 为当前租户未删除部门数
- **AND** `superAdminCount` 为绑定 `super_admin` 角色的去重用户数
- **AND** `subAdminCount` 为绑定 CUSTOM 角色的去重用户数
