# 设计：系统域数据模型 V1（system-schema-v1）

## Context

- **上游**：`tenant-ready-foundation` 已提供 `sys_tenant`、`sys_tenant_user`、MyBatis 租户插件。
- **参考**：飞书帮助中心归档（账号与设置 + 企业管理后台，403 篇），重点文档包括管理员角色与权限、部门管理、成员邀请/暂停/离职、权限审计等。
- **约束**：`docs/dev/database.md`（雪花 ID、`tenant_id`、公共字段）、`docs/dev/architecture.md`（`*-api` 边界）。

本设计 **吸收飞书管理后台 RBAC 与组织架构思想**，不复制飞书产品能力（席位、用户组、协作规则引擎等）。

## Goals / Non-Goals

**Goals:**

- 定型 system 域 11 张业务表 + 2 张已有租户元数据表的职责边界
- 双轴 RBAC：功能权限 + 数据范围（部门维度）
- 成员生命周期状态挂在 **租户成员关系**（`sys_tenant_user.status`），支持将来一账号多企业
- Flyway 迁移 + 预置 `super_admin` 与最小权限树

**Non-Goals（V1 本 change 及近期不做）:**

- IM **在线/presence 状态**（请勿打扰、会议中）— 属 IM 域，与成员生命周期无关
- 审批角色（飞书「角色管理」中的审批人角色）— 归 BPM 域
- 用户组、动态用户组、单位管理
- 主体-客体协作权限规则、字段可见性、搜索权限
- 完整权限审计 UI

## 概念澄清（来自讨论结论）

### 成员状态 vs 在线状态

| 概念 | 存储位置 | 示例 | 谁设置 |
|------|----------|------|--------|
| **成员生命周期** | `sys_tenant_user.status` | 未激活、正常、暂停、待离职、已离职 | 管理员 / 邀请流程 |
| **在线/presence** | IM 域（后续） | 请勿打扰、会议中、休息中 | 用户自己，临时有效 |

登录鉴权：校验全局账号凭据后，必须检查 **当前 tenant 下** `sys_tenant_user.status = ACTIVE`（或业务允许的状态）。

### 部门是什么

部门首先是 **组织事实**（汇报线、通讯录结构），其次才用于 **数据范围**：

- 研发部经理的管理范围 = 「研发部」→ 只能管该部门成员数据
- 销售总监 = 「全公司」或「销售部及子部门」
- 普通员工归属部门，但不因此获得「管别人」的数据权限

数据范围由 **角色** 配置（`data_scope` + `sys_role_dept`），不是部门表自带权限。

### 飞书「三种角色」与 RelayFlow 映射

| 飞书概念 | RelayFlow V1 |
|----------|----------------|
| 管理员角色（管理后台 RBAC） | `sys_role` + `sys_permission` |
| 审批角色（流程审批人） | **不做**（BPM 域） |
| 用户组（跨部门分组） | **不做**（后续 change） |

## Decisions

### D1：全局账号 vs 租户成员

```text
sys_user          — 跨租户身份（登录标识、密码 hash、昵称等）
sys_tenant_user   — 用户 × 租户成员关系 + status + 租户内扩展
sys_* 业务表      — tenant_id 隔离（部门、角色等归属租户）
```

`sys_user` **不含**成员生命周期 `status`；一人在 A 企业在职、B 企业离职，由两条 `sys_tenant_user` 记录表达。

### D2：双轴 RBAC

**轴 1 — 功能权限（能不能做）**

```text
user → sys_user_role → sys_role → sys_role_permission → sys_permission.code
```

- `sys_permission`：树形权限点，`code` 用于 API 鉴权（如 `system:user:list`）
- `sys_menu`：管理端 UI 菜单，可选关联 `permission_id`；**不作为鉴权真源**

**轴 2 — 数据范围（能管哪些数据）**

- `sys_role.data_scope`：`ALL` | `DEPT` | `DEPT_AND_CHILD` | `SELF` | `CUSTOM`
- `sys_role_dept`：当 `data_scope = CUSTOM` 时，指定部门 ID 列表
- 同一角色内所有管理员共享相同数据范围（对齐飞书「同一管理员角色管理范围相同」）

### D3：角色树与委托

| 字段 | 说明 |
|------|------|
| `sys_role.parent_id` | 上级管理员角色；子角色权限 ⊆ 父角色 |
| `sys_role.role_type` | `SYSTEM`（预置，不可删）/ `CUSTOM` |
| `sys_role.can_delegate` | 是否允许向子角色分配自己拥有的权限 |
| `sys_role.code` | 系统角色编码，如 `super_admin` |

预置角色：

- `super_admin`：全部权限 + `data_scope = ALL`
- （可选）租户创建人标记：`sys_tenant.owner_user_id` 或绑定 `super_admin` 角色

### D4：组织架构

| 表 | 说明 |
|----|------|
| `sys_dept` | 部门树：`parent_id`、排序、负责人（主负责人标记） |
| `sys_user_dept` | 用户-部门 M:N，`primary_flag` 标识主部门 |

### D5：成员生命周期枚举

`sys_tenant_user.status`（SMALLINT 或 VARCHAR 存枚举名）：

| 值 | 含义 | 能否登录该租户 |
|----|------|----------------|
| `NOT_JOINED` | 已邀请未加入 | 否 |
| `PENDING_ACTIVATION` | 已加入未激活 | 否 |
| `ACTIVE` | 正常 | 是 |
| `SUSPENDED` | 管理员暂停 | 否 |
| `PENDING_LEAVE` | 待离职 | 视策略（建议否） |
| `LEFT` | 已离职 | 否 |

### D6：表清单

**租户元数据（已有，本 change 扩展）**

- `sys_tenant` — 可选增 `owner_user_id`
- `sys_tenant_user` — **增 `status`**

**本 change 新建（均含 `tenant_id` + 公共字段）**

- `sys_user` — 全局账号（**无** tenant_id，**无** lifecycle status）
- `sys_dept`
- `sys_user_dept`
- `sys_role`
- `sys_user_role`
- `sys_permission`
- `sys_role_permission`
- `sys_role_dept`
- `sys_menu`

共 9 张新业务表 + 2 张租户表（1 张扩展列）。

### D7：ER 关系

```text
sys_tenant ──< sys_tenant_user >── sys_user
sys_tenant ──< sys_dept (tree)
sys_tenant ──< sys_role (tree)
sys_tenant ──< sys_permission (tree)
sys_tenant ──< sys_menu

sys_user ──< sys_user_dept >── sys_dept
sys_user ──< sys_user_role >── sys_role
sys_role ──< sys_role_permission >── sys_permission
sys_role ──< sys_role_dept >── sys_dept (when CUSTOM scope)
sys_menu ── optional ──> sys_permission
```

### D8：Flyway 与种子

- 文件名：`V1.0.0.2__init_system.sql`
- 种子内容：
  - 扩展 `sys_tenant_user.status`，已有行默认 `ACTIVE`
  - 插入 system 域权限点树（用户/部门/角色/菜单 CRUD + 登录占位）
  - 插入 `super_admin` 角色并绑定全部权限
  - **不**插入默认超管用户（留给 `system-auth-minimal` 或安装向导）

### D9：与 tenant-ready-foundation 分工

| 内容 | tenant-ready-foundation | system-schema-v1 | system-auth-minimal |
|------|-------------------------|------------------|---------------------|
| 租户插件 | ✓ | | |
| sys_user 表 | | ✓ | |
| sys_tenant_user.status | | ✓ | |
| JWT / 登录 API | 任务 4.x | | ✓ |
| 数据权限拦截器 | | 设计预留 | ✓ 实现 |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| `sys_user` 无 tenant_id，查询易串租 | 业务读用户列表必须 JOIN `sys_tenant_user` 且带 tenant 条件 |
| 角色数据范围过粗 | V1 先 DEPT/ALL；细粒度资源范围（会议室等）后续按模块扩展 |
| 与 tenant-ready §4 任务重叠 | 本 change 只建表+DO；JWT 明确放到下一 change |

## Migration Plan

1. 执行 `V1.0.0.2__init_system.sql`
2. system-biz 增加 DO/Mapper，编译通过
3. `./mvnw -pl relayflow-server -am compile`
4. 归档本 change 后启动 `system-auth-minimal`

## Open Questions

- `sys_tenant.owner_user_id` 是否在本迁移一并添加？（建议：是，nullable）
- `PENDING_LEAVE` 是否允许登录？（建议：否，与飞书暂停语义一致）
- 安装向导创建首个超管：独立 change 还是并入 `system-auth-minimal`？

## 路线图衔接

```text
tenant-ready-foundation（进行中，框架+租户表）
  → system-schema-v1（本 change：表结构+种子+DO）
  → system-auth-minimal（JWT+登录+权限注解骨架）
  → system-admin-api（用户/部门/角色 CRUD + 管理端页面）
```
