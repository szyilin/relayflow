## ADDED Requirements

### Requirement: Invite settings page in admin shell web

管理端壳层 MUST 能导航至邀请设置页；页面使用管理端布局与 `AdminPageHeader`，视觉与其它系统设置页一致。

#### Scenario: Open from sidebar

- **WHEN** 管理员点击「邀请设置」
- **THEN** 渲染邀请设置表单且不离开 `/admin` 前缀路由
