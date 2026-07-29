## ADDED Requirements

### Requirement: Calendar product settings shell

管理端 SHALL 在 `/admin/products/calendar`（及必要二级路由）提供可配置的日历组织设置体验，替换占位页。导航 MUST 覆盖：**组织策略（V1 薄集）** 与 **全员日历管理**（可合并为单页分组）。产品注册表中日历 MUST 为可配置态（非 `placeholder`）。访问 MUST 要求管理身份及 `calendar:product-settings:query`（读）/ `calendar:product-settings:update`（写）。

#### Scenario: Admin opens calendar product settings

- **WHEN** 具备权限的管理员打开 `/admin/products/calendar`
- **THEN** 系统展示可配置表单或分组导航（非「后续版本开放」占位）
- **AND** MUST NOT 提供编辑 `user-preference` 个人日历段的控件

#### Scenario: Permission gate

- **WHEN** 无 `calendar:product-settings:query` 的用户访问该路由
- **THEN** 系统拒绝进入或展示无权限状态（与既有管理端 RBAC 一致）

### Requirement: Org calendar policy thin set

系统 SHALL 持久化每租户一行日历组织策略（如 `cal_tenant_settings`）。V1 MUST 至少包含是否允许成员新建 `OWNED` 日历（`allowMemberCreateOwnedCalendar`，默认 `true`）。工作台创建 OWNED 日历的写路径 MUST 以后端该策略为准。

#### Scenario: Disallow owned calendar create

- **WHEN** 管理员将 `allowMemberCreateOwnedCalendar` 设为 `false` 并保存
- **AND** 普通成员调用创建 OWNED 日历 API
- **THEN** 系统拒绝创建并返回明确业务错误

#### Scenario: Personal preference untouched

- **WHEN** 管理员保存组织日历策略
- **THEN** MUST NOT 修改任何用户的 `user-preference.settings.calendar`

### Requirement: Admin manages all-staff calendars

管理端 SHALL 允许管理员创建、更新与删除当前租户的**全员日历**（`type=ALL_STAFF`）：名称、描述、**默认颜色**、可见范围。可见范围 V1 MUST 支持至少「全员」与「指定用户」；若部门选择器已具备则可支持部门。创建后，可见范围内成员 MUST 在工作台自动获得订阅可见性（无需显式 share 行）。

#### Scenario: Create all-staff calendar for all members

- **WHEN** 管理员创建全员日历，可见范围为全员，并设置默认颜色
- **THEN** 系统持久化 `ALL_STAFF` 日历与 scope
- **AND** 租户内有效成员在日历 list 中看到该日历（订阅身份）
- **AND** 未设置本机色的成员，网格使用该默认颜色

#### Scenario: Scoped all-staff

- **WHEN** 管理员将可见范围设为指定用户集合
- **THEN** 仅范围内用户 list 可见该日历
- **AND** 范围外用户 MUST NOT 因全员日历机制看到其事件

#### Scenario: Update default color

- **WHEN** 管理员更新全员日历的默认颜色并保存
- **THEN** 所有未设置本机显示色的订阅者有效颜色变为新默认色
- **AND** 已设置本机色的用户有效颜色保持其本机色不变

#### Scenario: Delete all-staff calendar

- **WHEN** 管理员删除全员日历
- **THEN** 成员 list 不再出现该日历
- **AND** 其事件对自动订阅路径不可见
