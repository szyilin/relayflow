## MODIFIED Requirements

### Requirement: Invite settings page in admin shell web

管理端壳层 MUST 能导航至邀请设置页；页面使用管理端布局与 `AdminPageHeader`，视觉与其它系统设置页一致。页面数据 MUST 经 `stores/inviteSettings` 调用 admin-api，不得以会话内存为唯一真源。

#### Scenario: Open from sidebar

- **WHEN** 管理员点击「邀请设置」
- **THEN** 渲染邀请设置表单且不离开 `/admin` 前缀路由

#### Scenario: Load settings from API on enter

- **WHEN** 管理员进入邀请设置页且具备 `system:user:list`
- **THEN** 客户端请求 `GET /admin-api/system/invite-settings` 并展示返回值
