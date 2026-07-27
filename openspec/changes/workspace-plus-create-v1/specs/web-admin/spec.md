## ADDED Requirements

### Requirement: Invite settings under members and departments

管理端导航在「成员与部门」相关分组下 MUST 提供「邀请设置」入口（路由 `/admin/...`），页面 MUST 支持编辑飞书对齐的邀请功能模式、非管理员范围、有效期与审批开关，并调用管理端邀请设置 API 保存。无管理身份 MUST NOT 渲染该页壳层。

#### Scenario: Admin navigates to invite settings

- **WHEN** 管理员从侧栏打开邀请设置
- **THEN** 页面展示当前租户邀请配置且可保存成功

#### Scenario: Non-admin cannot open invite settings

- **WHEN** 非管理身份用户尝试进入邀请设置路由
- **THEN** 系统拒绝渲染管理端该页（重定向无权限引导或等价门禁）
