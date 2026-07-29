## ADDED Requirements

### Requirement: Docs admin shell with three modules

管理端产品设置中的云文档入口 SHALL 提供可导航壳层，二级入口 MUST 包含：**权限与分享**、**文件管理**、**存储空间**。路由根 MUST 为 `/admin/products/docs`；默认进入权限与分享页。产品注册表 MUST 将云文档标为可配置（非整页「后续版本开放」占位）。

#### Scenario: Admin opens docs product settings
- **WHEN** 具备相应权限的管理员打开 `/admin/products/docs`
- **THEN** 系统进入云文档管理壳层并展示上述分组导航
- **AND** 默认落地权限与分享模块

#### Scenario: Undelivered module placeholder
- **WHEN** 某分组对应波次尚未 integrate
- **THEN** 该页 MAY 显示即将开放说明
- **AND** MUST NOT 呈现可保存的虚假成功状态

### Requirement: Org policy settings UI (Wave B)

管理端 SHALL 在 `/admin/products/docs/permissions` 提供组织级开关，至少包括：是否允许成员分享文档、是否允许 Markdown 导出、是否允许云盘文件上传。保存 MUST 调用 Admin API（integrate 后）；web 阶段 MAY 使用 store Mock。

#### Scenario: Admin toggles member share
- **WHEN** 管理员关闭「允许成员分享」并保存成功
- **THEN** 后续读取产品设置 MUST 反映为关闭

### Requirement: Docs product-settings permission codes

系统 SHALL 使用权限码 `docs:product-settings:query` 与 `docs:product-settings:update`（及可选菜单码）控制 B 模块读写；导航 MUST 按权限过滤。

#### Scenario: Query without permission denied
- **WHEN** 无 `docs:product-settings:query` 的管理员请求读取产品设置
- **THEN** 系统拒绝该请求

### Requirement: Boundary with personal preference

云文档管理端配置 MUST NOT 修改任何用户的 `user-preference`。

#### Scenario: Save does not touch user preference
- **WHEN** 管理员保存云文档组织设置
- **THEN** 系统 MUST NOT 写入 `user-preference` 相关存储
