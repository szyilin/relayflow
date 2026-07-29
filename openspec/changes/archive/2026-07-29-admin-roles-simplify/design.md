## Context

当前管理端「角色管理」实现了早期双轴 RBAC：`sys_role.data_scope` + `sys_role_dept`，创建表单强制「上级角色 / 角色编码 / 数据范围」。产品真源已明确：工作台按组织成员准入；管理后台仅「角色 + 功能权限」，有权限即看全量。飞书「管理员角色」心智为：角色名称、管理员成员、功能权限；上级可选；不向用户暴露编码与数据范围。

开发期 `0.x`（见 `docs/dev/database.md`）：产品负责人已同意本变更可做破坏性 schema（删列/删表、本地库可重置），**不做**历史数据适配。

## Goals / Non-Goals

**Goals:**

- 删除角色数据范围模型（库、API、运行时过滤、文档双轴表述）。
- `code` 仅系统角色锚点；自定义角色 `code=NULL`；UI 永不展示。
- 上级角色可选（空 = 一级角色）；子角色权限仍须为上级子集。
- 管理端改名为「管理员角色」，交互对齐飞书：列表（名称、管理员、权限摘要）、详情（成员 / 权限 Tab）、创建/编辑字段精简。
- 权限树：默认折叠；勾选父节点 → 展开并全选子孙。

**Non-Goals:**

- 不为「同权限不同部门边界」另建新模型（本阶段明确不做）。
- 不改工作台成员准入、不引入对 `/app` 的 RBAC。
- 不重做用户管理页整体（仅去掉 data_scope 过滤副作用）。
- 不在本 change 做管理员操作审计日志 / 导出角色详情（飞书有、本切片不抄）。

## Decisions

### D1：暴力删除数据范围（0.x）

**决策**：新 Flyway（建议 `V0.1.0.44__drop_role_data_scope.sql`）：

1. `DROP TABLE IF EXISTS sys_role_dept;`
2. `ALTER TABLE sys_role DROP COLUMN IF EXISTS data_scope;`
3. 自定义角色：`UPDATE sys_role SET code = NULL WHERE role_type = 'CUSTOM' AND deleted = 0;`（若列上仍有唯一索引，保持 `WHERE code IS NOT NULL` 的 partial unique）。
4. 系统角色保留 `code='super_admin'`。

同步删除 Java：`DataScope` 枚举、`DataScopeHelper*`、`SysRoleDept*`、Role VO/Service 中相关字段、用户分页 `filterUserIdsByDataScope`。

**备选**：改写历史 `V0.1.0.2` —— 拒绝（Flyway 纪律：不改已执行历史脚本）。

### D2：单轴鉴权

**决策**：管理 API 仅校验 `permission.code`；具备 `system:user:list` 等权限时，用户分页返回当前租户可见成员全量（仍可按 `deptId` 等显式筛选，但不再与角色数据范围取交）。

### D3：`code` 策略

| 类型 | `code` | 查找 |
|------|--------|------|
| SYSTEM（超管） | 固定 `super_admin` | `TenantBootstrap` 等继续按 code |
| CUSTOM | 一律 `NULL` | 仅用 `id` |

创建/更新 API **不再接收** `code`；服务端写入时 CUSTOM 强制 `null`，SYSTEM 禁止改 code。列表/详情响应可仍返回 `code`（给调试），前端不展示。

**备选**：所有 CUSTOM 写死 `custom` —— 拒绝（与 `(tenant_id, code)` unique 冲突）。

### D4：上级角色

**决策**：`parentId` 可选；`null`/`0` = 一级。去掉 `@NotNull`；前端去掉 required，提供「留空即一级」提示。有上级时继续校验权限子集。

### D5：飞书式 UI 范围（本切片）

**决策**：

1. 导航/标题：「管理员角色」；路由仍 `/admin/system/role`。
2. 列表列：角色名称（系统角色标签）、管理员（头像+名）、权限摘要（由已绑 permission 树顶层/模块名拼简短文案）；去掉编码、数据范围列。
3. 行操作：详情、添加管理员；主 CTA：创建角色。
4. 详情：右侧抽屉或等价面板，Tab「成员」「权限」。
5. 创建/编辑表单字段：上级（可选）、名称、功能权限树、备注（可选）；无 code、无 dataScope。
6. 权限树：默认全部折叠；勾选父 → 选中全部子孙并展开该父节点；取消父 → 取消子孙（常见级联）。

**成员写入**：优先新增角色侧 API（`GET .../role/members`、`POST .../role/add-members`、`POST .../role/remove-member`），内部写 `sys_user_role`；与现有 `PUT /user/update-role` 并存（后者仍可用于用户页）。列表「管理员」列由 members 摘要或 page 接口附带 `admins[]`。

### D6：契约与文档

更新 `openspec/lanes/admin-role/contract.md`、`docs/dev/product-permission-model.md`（去掉双轴数据范围）、`docs/dev/api-integration-board.md` 备注。本切片前后端同一 change（已有页面重构 + schema 推倒，不拆 `-web/-api`）。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 本地已有库含 data_scope 旧数据 | 开发期重置 DB 或跑新迁移；文档写明 |
| 去掉数据范围后细粒度部门管理员无法裁切用户列表 | 产品明确接受；未来若需要另开 change |
| 角色成员 API 与 user/update-role 双写路径 | 同一张 `sys_user_role`；权限码统一 `system:role:update` 或 `system:user:update`（design 实现时选 role 侧 `system:role:update`） |
| 权限摘要文案过长/不准 | V1 用已选权限的一级模块名去重拼接，超长截断 |

## Migration Plan

1. 合并本 change 代码 + `V0.1.0.44`（序号以仓库最新迁移为准递增）。
2. 开发机：`docker compose` 重建 Postgres 或 Flyway migrate；必要时 `flyway clean`（仅本地）。
3. 验证：超管仍 `code=super_admin`；新建角色无 code/dataScope；用户列表全量；UI 改名与权限树行为。
4. 回滚：Git revert + 库重置；不提供生产向后兼容。

## Open Questions

- （无阻塞）详情用抽屉还是整页：默认 **抽屉**（对齐飞书截图）；实现时可按 Nuxt UI `USlideover`/`UModal` 选型。
