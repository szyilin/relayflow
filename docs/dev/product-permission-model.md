# 产品权限与双产品面模型

RelayFlow 面向企业协作：**同一账号**登录后，根据是否具备**管理角色**，分别使用**员工工作台（产品面）**或**管理后台（管理面）**。本文档是产品权限的**开发约定真源**；行为规格以 `openspec/specs/` 为准（后续 OpenSpec change 将对齐本文）。

行业参考：飞书、钉钉等均采用「客户端全员可用 + 管理后台仅管理员可进」的策略；RelayFlow V1 对齐该**产品面二分**，不复制席位、用户组、协作规则引擎等能力。

---

## 1. 两个产品面

| 产品面 | 前端路由 | API 前缀 | 谁可以用 |
|--------|----------|----------|----------|
| **员工工作台（产品面）** | `/app/**` | `/app-api/**` | 当前租户下**有效成员**（有账号、已加入组织、成员状态允许登录） |
| **管理后台（管理面）** | `/admin/**` | `/admin-api/**` | 同上，且具备**管理角色**（见 §2） |

```text
                    同一账号 · 同一 JWT · /app/login
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
     /app/* 员工工作台                  /admin/* 管理后台
     有效组织成员即可用                 须具备管理角色
     不按 RBAC 区分功能入口             进入后再按 permission 控菜单/API
```

**要点：**

- **不**提供「员工登录 / 管理员登录」两个入口；`/admin/login` 仅兼容重定向至 `/app/login`。
- 员工工作台**不**用 `sys_permission` 做路由或菜单级区分（有账号、有组织、登录成功即可使用产品面能力）。
- 管理后台**必须**先过「有没有管理角色」这一道门，再在内部用 RBAC 细粒度控制（见 §3）。

---

## 2. 身份判定

### 2.1 组织成员（能否用产品面）

满足以下条件视为**当前租户的有效成员**，可访问 `/app/**` 与 `/app-api/**`（在 API 鉴权通过的前提下）：

1. 持有有效 JWT；
2. 存在当前租户的 `sys_tenant_user` 关系；
3. 成员状态允许登录（V1 以 `ACTIVE` 为主；其他状态见 `system` spec / 数据库枚举）。

**不检查**是否绑定 `sys_role`、**不检查** `permission.code`。

### 2.2 管理身份（能否进管理后台）

**管理身份（`isAdmin`）** 定义：在当前租户下，用户至少绑定一个 **管理角色**（`sys_user_role` → `sys_role`），且该角色通过 `sys_role_permission` **至少关联一个** `sys_permission`（即具备至少一项后台管理能力）。

| 情况 | `isAdmin` |
|------|-----------|
| 未绑定任何角色 | 否 |
| 绑定了角色但角色零 permission | 否 |
| 绑定了角色且角色有 ≥1 个 permission | 是 |
| `super_admin` 等预置全权限角色 | 是 |

**说明：**

- 「管理角色」即 system 域用于后台 RBAC 的 `sys_role`；**不是** BPM 审批角色、**不是**未来的用户组。
- 同一用户可同时是**普通成员**（用工作台）和**管理员**（进后台）；二者不互斥。
- `isAdmin` 仅决定**能否进入管理后台产品面**；进入后具体能做什么由 §3 的 permission 决定。

### 2.3 与 JWT 的关系

- V1：**同一套登录接口**（`POST /admin-api/system/auth/login`）签发 JWT，产品面与管理面共用。
- Claims 可含 `tenant_id`、`sub`（userId）等；是否在 token 内缓存 `isAdmin` 由实现决定，**语义真源**仍是库内角色与 permission 关联。
- `/admin-api/**` 与 `/app-api/**` 在 Security 层按路径前缀区分；**管理面 API** 除登录等白名单外，须 JWT + 对应 `permission.code`；**产品面 API** 须 JWT + 有效成员身份，**不**按 `sys_permission` 做通用门禁（各业务模块可有自身 ACL，属后续域）。

---

## 3. 管理面 RBAC（进入后台之后）

管理后台内部采用已有 **双轴 RBAC**（详见 `system-schema-v1` design、`docs/dev/code-style.md` § RBAC）：

| 轴 | 作用 |
|----|------|
| **功能权限** | `permission.code` 控制菜单、按钮、API（如 `system:user:list`） |
| **数据范围** | `sys_role.data_scope` + `sys_role_dept` 控制可操作的组织数据边界 |

约定：

- 菜单/导航按 `permission.code` 过滤；无 permission 的 API 返回 HTTP 403。
- 多角色 permission **并集**；data scope **并集**（与现有 kernel 一致）。
- **禁止**用管理面 RBAC 的 permission 去限制 `/app/**` 路由或员工工作台菜单（除非未来单独 spec 的客户端策略层）。

---

## 4. 前端路由与登录门禁

### 4.1 统一规则：未登录不可进产品面/管理面

| 路径 | 未登录 | 已登录 |
|------|--------|--------|
| `/app/login` | 允许（唯一登录页） | 重定向至 `/app/messages` |
| `/app/**`（除 login） | **强制** → `/app/login?redirect=...` | 允许（有效成员） |
| `/admin/**` | **强制** → `/app/login?redirect=...` | 见 §4.2 |
| `/` | 未登录 → login；已登录 → `/app/messages` | — |

**禁止：**

- 未登录仍渲染 `/app` 或 `/admin` 壳层（空 sidebar、占位内容等），避免用户误以为已登录。
- 除 `/app/login` 外，将 `/app/**`、`/admin/**` 设为「可匿名访问再靠 API 403」。

### 4.2 管理后台（已登录）

| 条件 | 行为（目标态） |
|------|----------------|
| 已登录 + `isAdmin` | 进入 `/admin/**`；菜单/API 按 permission 过滤 |
| 已登录 + 非 `isAdmin` | **不得**进入管理后台；展示无权限引导（或重定向回工作台），**不**展示管理端壳层 |
| 工作台「管理后台」入口 | 仅 `isAdmin` 时展示 |

### 4.3 员工工作台（已登录）

- 有效成员即可使用 `/app/**`；**不**按 permission 隐藏主导航（消息、任务等）。
- 业务模块内部的文档/群/频道 ACL 等在 IM 等域单独建模，**不属于**本文的管理角色 RBAC。

---

## 5. 后端 API 门禁（摘要）

| API 前缀 | 鉴权 |
|----------|------|
| `/app-api/**` | JWT + 当前租户有效成员；**不**默认要求 `sys_permission` |
| `/admin-api/**` | JWT；受保护端点 + `@PreAuthorize(hasAuthority(...))`；**目标态**：无任一 admin permission 的用户对管理面全局 403（白名单：login、get-permission-info 等见各 change contract） |

登录、租户默认信息等端点的白名单以 `openspec/lanes/` contract 与 `SecurityAutoConfiguration` 为准。

---

## 6. 目标态 vs 当前实现（文档编写时）

以下 gap 将在后续 OpenSpec + 代码 change 中关闭；**实现前勿将「目标态」当作已完成**。

| 项 | 目标态（本文） | 当前代码（可能） |
|----|----------------|------------------|
| `/app/**` 未登录 | 跳转 login | 路由守卫已要求登录（除 login） |
| `/admin/**` 未登录 | 跳转 login | 路由守卫已要求登录 |
| `/admin/**` 已登录非 admin | 无权限引导 / 拒绝 | **仍可能**仅校验 token 即可进入壳层 |
| 工作台「管理后台」入口 | 仅 `isAdmin` 可见 | **仍可能**全员可见 |
| `/admin-api/**` 零 permission 用户 | 403 | 部分端点可能仅 JWT |

---

## 7. V1 范围（刻意不做）

| 能力 | 说明 |
|------|------|
| 席位/计费 | 自部署不做 |
| 用户组 | Phase 2 |
| 组织架构可见范围（客户端策略） | IM 通讯录前再 spec |
| 应用可用范围、沟通协作规则 | 客户端策略层，非 `sys_permission` |
| 审批角色 | BPM 域 |
| 用 RBAC 限制 `/app` 主导航 | **禁止**（产品面不按管理 permission 区分） |

---

## 8. 开发禁止项（防跑偏）

1. **禁止**假设「有 JWT 即可进 `/admin`」为最终产品行为。
2. **禁止**用 `sys_permission` / `hasPermission()` 控制员工工作台主导航或 `/app/**` 路由准入。
3. **禁止**未登录渲染 `/app` 或 `/admin` 布局壳层。
4. **禁止**为管理员单独做第二套账号或登录页。
5. **禁止**把审批角色、用户组概念写入 `sys_role` 种子或管理端 RBAC 文档而不标注域边界。
6. 新增 auth、guard、nav、Security 相关代码前 **必须先读本文** 与 `docs/dev/code-style.md` § 路由。

---

## 9. 相关文档

| 文档 | 内容 |
|------|------|
| [code-style.md](code-style.md) | 路由前缀、JWT、RBAC 代码格式 |
| [api.md](api.md) | 401/403、响应格式 |
| [vertical-slice-workflow.md](vertical-slice-workflow.md) | 切片流程与入口摘要 |
| [architecture.md](architecture.md) | `/admin-api` vs `/app-api` 模块边界 |
| `openspec/specs/web-auth/spec.md` | 登录行为（待 OpenSpec 补充门户准入） |
| `openspec/specs/system/spec.md` | RBAC 运行时 |

Cursor 规则：`.cursor/rules/product-permission-model.mdc`
