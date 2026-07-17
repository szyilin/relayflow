# user-preference Specification

## Purpose

企业内用户工作台偏好持久化：主题、主题色、会话气泡布局等，按 `(tenant_id, user_id)` 隔离，遵循 C 类默认数据惯例。

## Requirements

### Requirement: 企业内用户偏好持久化

系统 MUST 按 `(tenant_id, user_id)` 持久化当前成员的工作台偏好。同一全局账号在不同租户下 MUST 允许不同偏好。表 MUST 使用 JSON 文档存储设置，并包含 `schema_version`。

#### Scenario: 首次读取无行

- **WHEN** 成员从未保存过偏好且调用获取偏好 API
- **THEN** 系统返回与代码默认合并后的完整有效配置
- **AND** MUST NOT 仅为该次读取而强制插入数据库行

#### Scenario: 首次保存

- **WHEN** 成员提交有效的偏好更新
- **THEN** 系统 upsert 该租户下该用户的偏好行
- **AND** 再次 GET 返回合并后的有效配置（含新默认键）

#### Scenario: 跨租户隔离

- **WHEN** 同一用户在租户 A 保存深色主题后切换到租户 B 且 B 无偏好行
- **THEN** 租户 B 的 GET 返回默认（或 B 自己的行），MUST NOT 自动沿用 A 的行

### Requirement: 仅本人可读写当前租户偏好

获取与更新偏好 API MUST 仅作用于 JWT 中的当前用户与当前租户。MUST NOT 提供通过路径参数修改其他用户偏好的 V1 接口。

#### Scenario: 鉴权

- **WHEN** 未登录客户端调用偏好 API
- **THEN** 系统拒绝（与其他 app-api 一致）

### Requirement: 默认数据类别为 C

用户偏好实现 MUST 遵循默认数据惯例的 **C 类**（读合并、写 upsert），MUST NOT 在成员激活时批量预插默认偏好行。

#### Scenario: 成员激活

- **WHEN** 新成员在租户内激活
- **THEN** 系统不因此插入 `sys_user_preference` 行

### Requirement: Calendar section in user preference

The user preference document MUST include a `settings.calendar` object for per-tenant-member calendar UI defaults. Keys MUST be merged with code defaults on GET (C-class). Changing these defaults MUST NOT rewrite existing `cal_event` reminder snapshots. Defaults MUST include `showTaskLayer` (boolean, default `true`) controlling whether the calendar page initially enables the「我的任务」virtual layer.

#### Scenario: Default calendar settings

- **WHEN** a member has no preference row or no `calendar` keys
- **THEN** GET preference returns code defaults including at least: `weekStartsOn`, `defaultEventDurationMinutes`, `defaultRemindBeforeMinutes`, `allDayRemindTime`, `dimPastEvents`, `showTaskLayer`

#### Scenario: Save calendar settings

- **WHEN** a member updates `settings.calendar` via PUT preference
- **THEN** the values persist for `(tenant_id, user_id)`
- **AND** other settings sections (e.g. `general`, `im`) remain intact under deep merge rules

#### Scenario: Default showTaskLayer

- **WHEN** `showTaskLayer` is absent from a stored preference document
- **THEN** GET merges it as `true`

### Requirement: Settings panel Calendar category

The workspace global settings panel (opened from the profile/avatar entry) MUST expose a **日历** category for editing `settings.calendar`. Calendar preferences MUST NOT use a separate page-local settings store as the source of truth. The category MUST include a control for `showTaskLayer` (显示「我的任务」图层).

#### Scenario: Open calendar category

- **WHEN** a user opens Settings and selects 日历
- **THEN** they can edit week start, default event duration, default reminder fields, and whether the task layer is shown by default
- **AND** saving uses the user-preference API (or store that targets it after integrate)

### Requirement: Frontend preference store is server-backed

工作台前端的用户偏好 Pinia store MUST 在登录成功与租户/账号切换后通过偏好 GET API 水合状态。权威设置 MUST 来自 API 合并结果。localStorage MUST NOT 作为跨会话、跨设备的写权威；若使用缓存，键 MUST 包含当前 `tenantId` 与 `userId`。PUT 失败时 store MUST 向调用方或 UI 暴露错误，MUST NOT 静默忽略。

#### Scenario: Hydrate after tenant switch

- **WHEN** 成员从租户 A 切换到租户 B
- **THEN** 偏好 store 清空 A 的内存状态并 GET B 的偏好
- **AND** 主题与日历等设置反映 B 的合并结果（或代码默认）

#### Scenario: Types shared with API module

- **WHEN** 前端编译偏好相关代码
- **THEN** store 使用的 settings 类型与 `api/app/userPreference`（或共享 types）一致
- **AND** MUST NOT 存在两套字段互相漂移且无编译关联的平行接口定义
