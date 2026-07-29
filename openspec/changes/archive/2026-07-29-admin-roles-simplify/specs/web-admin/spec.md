## MODIFIED Requirements

### Requirement: 管理端前端权限门禁

管理端前端 **SHALL** 在登录后加载权限码，并在用户缺少关联 permission code 时隐藏导航项；**且**须在 `isAdmin=false` 时由门户准入要求拦截，不得仅依赖菜单过滤。导航文案中原「角色管理」入口 MUST 展示为「管理员角色」，路由仍为 `/admin/system/role`（或等价路径）。

#### Scenario: Sidebar 隐藏无权限菜单

- **WHEN** 已登录且 `isAdmin=true` 用户的权限集不包含 `system:role:list`
- **THEN** 管理端 sidebar MUST NOT 展示管理员角色入口

#### Scenario: Sidebar 隐藏部门管理

- **WHEN** 已登录且 `isAdmin=true` 用户的权限集不包含 `system:dept:list`
- **THEN** 管理端 sidebar MUST NOT 展示部门管理入口

#### Scenario: 超级管理员可见全部静态导航

- **WHEN** 已登录且 `isAdmin=true` 用户拥有全部 system 预置权限（如 `super_admin`）
- **THEN** 所有按 permission 门禁的 V1 静态 admin 导航项均可见

#### Scenario: 刷新页面重新加载权限

- **WHEN** 用户在 admin 路由携带有效 token 且 `isAdmin=true` 刷新浏览器
- **THEN** 前端在渲染过滤导航前再次请求权限信息

## ADDED Requirements

### Requirement: 管理员角色页面

管理端 SHALL 在 `/admin/system/role` 提供「管理员角色」页面，对齐飞书式心智：用户可见信息仅为角色名称、对应管理员、后台功能权限；MUST NOT 展示角色编码或数据范围。

#### Scenario: 列表列

- **WHEN** 具备 `system:role:list` 的管理员打开该页
- **THEN** 列表至少展示角色名称（系统角色须有可识别标签）、管理员摘要、权限摘要与操作
- **AND** MUST NOT 展示角色编码列或数据范围列

#### Scenario: 创建表单字段

- **WHEN** 管理员打开新建/编辑角色表单
- **THEN** 可填写角色名称、可选上级角色、功能权限、可选备注
- **AND** MUST NOT 展示或要求填写角色编码、数据范围
- **AND** 上级角色留空表示创建一级角色（须有说明或等价提示）

#### Scenario: 权限树折叠与级联

- **WHEN** 打开功能权限树
- **THEN** 节点默认折叠
- **AND** 勾选父节点时选中其全部子孙并展开该父节点

#### Scenario: 角色详情成员与权限

- **WHEN** 管理员打开某角色详情
- **THEN** 界面提供「成员」与「权限」两类视图（Tab 或等价）
- **AND** 成员视图支持查看、添加、移除该角色管理员（受权限门禁）
