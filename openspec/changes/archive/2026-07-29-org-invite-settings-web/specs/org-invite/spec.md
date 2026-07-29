## ADDED Requirements

### Requirement: Admin invite settings page web

管理端 MUST 提供「邀请设置」页（`/admin/system/invite-settings`），可编辑邀请功能（关闭 / 仅管理员 / 全员）、非管理员范围（本部门 / 指定部门）、非管理员有效期天数、是否需管理员审批。保存 MUST 更新客户端设置真源（`-web` 可为 store）；`-web` 阶段 MAY 不调用后端。

#### Scenario: Save all-members mode

- **WHEN** 管理员选择「全员均可发送邀请」、指定部门并保存
- **THEN** 页面展示保存成功，且再次进入页面仍显示已保存值（同会话 store）

#### Scenario: Non-admin fields hidden when admin-only

- **WHEN** 邀请功能为「仅管理员」或「关闭」
- **THEN** 非管理员范围/有效期/审批控件 MUST 隐藏或不可编辑

### Requirement: Invite settings nav under system

管理端系统导航 MUST 包含「邀请设置」入口，且受权限过滤；无权限 MUST NOT 显示该入口。

#### Scenario: Nav visible with permission

- **WHEN** 当前管理员具备查看用户相关权限
- **THEN** 侧栏可进入邀请设置页
