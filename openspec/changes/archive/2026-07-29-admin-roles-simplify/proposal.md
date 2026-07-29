## Why

管理端「角色管理」按早期「双轴 RBAC」（功能权限 + 数据范围）建模，并在创建表单暴露「角色编码」「数据范围」「必填上级角色」，与产品真源及飞书式「管理员角色」心智不一致：员工工作台按组织成员准入、管理后台仅按角色+功能权限控制，有权限即看全量组织数据。开发期 `0.x` 允许推倒 schema，现将管理员角色模型与 UI 收敛为：角色名称、成员（管理员）、功能权限（上级可选）。

## What Changes

- **BREAKING**：从 `sys_role` **删除** `data_scope` 列；删除表 `sys_role_dept`；移除 `DataScope` / `DataScopeHelper` 及用户分页等对数据范围的过滤。
- **BREAKING**：角色创建/更新 API **不再接受** `dataScope`、`deptIds`；管理端表单/列表不再展示数据范围。
- 角色编码：`code` **仅系统角色**使用（如 `super_admin`）；自定义角色 `code` 一律为 `NULL`；管理端 UI **永不展示、不让用户填写**。
- 上级角色：`parent_id` **非必填**；留空表示一级角色（对齐飞书说明）。
- 管理端文案与导航：由「角色管理」改为「管理员角色」；交互对齐飞书心智（列表突出角色名与管理员；详情区分成员/权限；权限树默认折叠、勾选父节点全选并展开子节点）。
- 鉴权模型改为单轴：`user → role → permission.code`；具备某管理权限即对该类后台数据全量可见（不再按部门裁切）。

## Capabilities

### New Capabilities

- （无）本变更修订既有 system / web-admin 行为，不新增独立能力域。

### Modified Capabilities

- `system`：删除 RBAC 数据范围需求；角色管理 API、用户分页、系统预置角色与组织说明中与 `data_scope` / `sys_role_dept` 相关的要求改为单轴功能权限；明确 `code` 仅系统角色、上级可选。
- `web-admin`：管理员角色页命名、列表/表单字段可见性、权限树交互与飞书式详情（成员/权限）行为。

## Impact

- **领域 / 模块**：`relayflow-module-system`（api+biz）、`relayflow-server` Flyway、`web/` 管理端角色页与导航、合同/看板文档（`openspec/lanes/admin-role`、`product-permission-model` 中双轴表述）。
- **API**：`/admin-api/system/role/*` 请求/响应去掉 dataScope；用户 `page` 不再按 data_scope 过滤。
- **数据库（0.x 破坏性，可重置）**：删列/删表；自定义角色 code 可空；本地/测试库可 drop 后重跑迁移链，**不做**历史数据回填。
- **回滚**：开发期以 Git + 重置库为主；正式 `1.x` 前不承诺兼容旧角色 payload。
