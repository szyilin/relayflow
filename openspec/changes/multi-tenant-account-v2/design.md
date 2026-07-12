# 设计：多租户账号与开放注册（V2 · 飞书对齐）

## Context

### 当前状态（V1）

```text
relayflow.tenant.enabled = false
  → TenantWebFilter 固定 tenant_id = 1
  → 登录 JWT tenant_id = 1
  → 管理端邀请 → sys_user(随机密码) + NOT_JOINED
  → /app/invite/accept 设密 + ACTIVE（独立「接受邀请」产品面）
```

### 目标状态（V2 · enabled=true）

```text
relayflow.tenant.enabled = true
  → JWT claim tenant_id = 用户当前选中的企业
  → 开放注册 → sys_user + 新 sys_tenant + ACTIVE(owner)
  → 同手机号 NOT_JOINED 邀请 → 注册/设密时一并 ACTIVE
  → 一账号多企业 → 企业切换器 + switch API 重签 JWT
```

### 飞书对齐映射

| 飞书 | RelayFlow V2 |
|------|----------------|
| 全局账号（手机号+密码） | `sys_user`（`mobile` 唯一登录标识之一） |
| 创建企业 | 注册时 `tenantName` → 新 `sys_tenant` + owner |
| 被邀请加入 | `sys_tenant_user NOT_JOINED` → 注册/设密 → `ACTIVE` |
| 一账号多企业 | 多条 `sys_tenant_user` + 切换器 |
| 首次登录激活 | 注册或 switch 后进入对应 tenant 工作台 |
| 管理端邀请 | 仅对 **当前 tenant** 发邀请，不替用户设密码 |

## Goals / Non-Goals

**Goals:**

- 配置开关启用多租户产品模式，与 V1 单租户模式共存（同一代码库）
- 工作台开放注册：手机号、密码、昵称、企业名称
- 注册创建企业：根部门、owner、`super_admin` 角色绑定
- 注册时激活该手机号所有 `NOT_JOINED` 成员关系（跨 tenant）
- 登录支持 mobile/username；多企业时返回企业列表
- 企业切换：重签 JWT，刷新 `tenant_id` 上下文
- 前端：注册页、企业切换器；登录页文案对齐飞书
- 管理端邀请在 `enabled=true` 下使用 JWT 当前 tenant

**Non-Goals:**

- 短信验证码、邀请 deep link token
- 个人号（无企业纯个人使用）
- SaaS 计费、平台运营后台
- 跨 tenant 数据查询（除「我的企业列表」元数据）
- 废弃 `enabled=false` 自托管路径

## Decisions

### D1：产品模式由配置切换

```yaml
relayflow:
  tenant:
    enabled: true              # false = V1 单租户（现有行为）
    default-id: 1              # 种子租户，不可删
    allow-open-register: true  # enabled=true 时允许 /auth/register
```

| 模式 | 注册 | 租户来源 | 切换 UI |
|------|------|----------|---------|
| `enabled=false` | 仅邀请 accept（现有） | 固定 `default-id` | 无 |
| `enabled=true` | 开放注册 + 邀请激活 | 自建 + 被邀请 | 有 |

**理由**：自托管客户无需迁移；SaaS/多组织场景打开开关即可。

### D2：注册 API 语义

**`POST /app-api/system/auth/register`**（permitAll）

| 字段 | 必填 | 说明 |
|------|------|------|
| `mobile` | 是 | 11 位手机号（V2 先支持 +86） |
| `password` | 是 | ≥6 位 |
| `nickname` | 是 | 用户展示名 |
| `tenantName` | 是* | 创建的企业名称 |

\* 若 `mobile` 已存在且仅因邀请预建（无有效密码、存在 `NOT_JOINED`）且无自建企业意图，可省略 `tenantName` 仅完成激活；**MVP 简化：始终要求 tenantName 创建自建企业**，同时激活所有 `NOT_JOINED`。

**事务内步骤：**

```text
1. 校验 mobile 未占用（已 ACTIVE 且已设密 → USER_MOBILE_EXISTS）
2. INSERT/UPDATE sys_user（username=mobile, password=BCrypt）
3. INSERT sys_tenant（code=随机, name=tenantName, owner_user_id=userId）
4. INSERT sys_tenant_user（ACTIVE）
5. 初始化租户：根部门(name=tenantName)、super_admin 角色绑定 owner
6. FOR EACH 其他 tenant 的 NOT_JOINED(mobile) → ACTIVE
7. 签发 JWT（tenant_id = 新建企业 id，优先进入自建企业）
8. 返回 AuthLoginResp + tenants[] 摘要
```

**备选（拒绝）**：注册时不建企业、仅个人账号 — 与飞书「个人号」相关，V2 不做。

### D3：登录 API 增强

**`POST /app-api/system/auth/login`**（现有路径，扩展行为）

| 字段 | 说明 |
|------|------|
| `username` | 用户名 **或** 手机号 |
| `password` | 密码 |
| `tenantId` | 可选；多企业且未指定时返回 `TENANT_SELECTION_REQUIRED` + 列表 |

**流程：**

```text
1. 按 username 或 mobile 查 sys_user，校验密码
2. 查 sys_tenant_user WHERE user_id AND status=ACTIVE
3. IF 0 条 → AUTH_NO_TENANT
4. IF 1 条 → 签发 JWT(tenant_id)
5. IF 多条 AND tenantId 缺失 → 返回 tenants 列表 + 错误码 TENANT_SELECTION_REQUIRED
6. IF 多条 AND tenantId 有效 → 签发 JWT(tenant_id)
```

`enabled=false` 时保持现有：仅查 default tenant 或第一条 ACTIVE。

### D4：企业切换

**`GET /app-api/system/tenant/my-list`**（authenticated）

返回当前用户所有 `ACTIVE` 企业：`tenantId`、`tenantName`、`isOwner`。

**`POST /app-api/system/tenant/switch`**

| 字段 | 说明 |
|------|------|
| `tenantId` | 目标企业 |

校验用户在该 tenant 为 `ACTIVE` → 重签 JWT → 返回新 `accessToken`。

**理由**：切换 tenant 须刷新 JWT 内 `tenant_id`，与现有 TenantWebFilter / MyBatis 插件一致。

### D5：邀请与注册合并

| 旧（V1） | 新（V2） |
|----------|----------|
| `GET/POST member-invite/*` | **Deprecated**；逻辑迁入 `AuthRegisterService` |
| `/app/invite/accept` | 重定向 `/app/register`；query 可预填 mobile |
| 登录页「收到邀请？设置密码加入」 | 「没有账号？注册」 |

管理端 `POST /admin-api/system/user/invite` **不变**，但 `resolveTenantId()` 在 `enabled=true` 时 **必须** 来自 JWT，禁止静默 fallback 到 default-id（除非平台 bootstrap 场景）。

### D6：JWT 与 TenantContext

```json
{ "sub": "userId", "tenant_id": 123, "username": "...", ... }
```

- `enabled=true`：`TenantWebFilter` 从 JWT 读 `tenant_id`
- `enabled=false`：忽略 JWT 中 tenant_id，固定 `default-id`（向后兼容）
- WebSocket 握手同步使用 JWT `tenant_id`

### D7：租户初始化（注册建企）

新 tenant 须具备最小可管理结构（复用现有 seed 逻辑抽取）：

```text
sys_tenant
sys_dept（根节点，name = tenant.name）
sys_tenant_user（owner, ACTIVE）
sys_user_dept（owner → 根部门, primary）
sys_role（super_admin 或租户管理员角色）
sys_user_role（owner → super_admin）
sys_permission 树（复制模板或引用全局 permission 定义）
```

**实现**：抽取 `TenantBootstrapService`（自 `V0.1.0.3__seed_admin_user` 逻辑演化），注册与安装向导共用。

### D8：前端

| 页面/组件 | 说明 |
|-----------|------|
| `/app/register` | 手机号、密码、确认密码、昵称、企业名称；提交后 establishSession |
| `/app/login` | 链接「没有账号？注册」；多企业时登录后弹窗/页内选企业 |
| `WorkspaceTenantSwitcher` | 壳层 header；调 `my-list` + `switch` |
| `/app/invite/accept` | 301/路由 redirect → `/app/register?mobile=` |

Store：`auth.ts` 增加 `tenants`、`activeTenant`、`switchTenant()`。

### D9：错误码（新增）

| Code | 说明 |
|------|------|
| `USER_MOBILE_EXISTS` | 手机号已注册 |
| `AUTH_REGISTER_PASSWORD_WEAK` | 密码过短 |
| `AUTH_NO_TENANT` | 无 ACTIVE 企业可登录 |
| `TENANT_SELECTION_REQUIRED` | 多企业须选择 |
| `TENANT_SWITCH_FORBIDDEN` | 非成员或未 ACTIVE |

### D10：数据迁移与兼容

- **无** 破坏性 DDL；`sys_tenant.owner_user_id` 已存在
- 种子 `tenant_id=1` 保留；新注册企业使用雪花 ID
- `enabled=false` 部署：零迁移
- `enabled=true` 新部署：可不依赖 tenant 1 业务数据（除全局 permission 模板）

### D11：开发环境数据策略（产品负责人授权）

**开发/联调阶段允许直接清理脏数据，不做历史数据适配。**

| 策略 | 说明 |
|------|------|
| 允许清库 | 实现 V2 前可 `DROP` 业务库或 `docker compose down -v` 后重跑 Flyway，无需写回填/迁移脚本兼容旧脏数据 |
| 不做脏数据适配 | 不为开发期遗留的 `NOT_JOINED`、错误 tenant 关联、测试用户等编写 `UPDATE`/数据修复逻辑 |
| 种子重建 | 清库后依赖 Flyway 种子（`V0.1.0.1`–`V0.1.0.3` 等）+ 新注册流程验证；admin 种子账号可保留或一并清掉后手动注册 |
| 生产例外 | 本授权 **仅适用于开发/联调**；若将来有生产库升级，另开 change 写正式迁移 |

**理由**：当前无生产数据；V2 模型变更大，清库比重写兼容脚本成本更低。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 开放注册 spam 租户 | 配置 `allow-open-register`；后续加验证码/限流 |
| 切换 tenant 后 IM/WS 会话串上下文 | switch 后断开 WS 重连；store 清 tenant 缓存 |
| 邀请与自建企业重复手机号逻辑复杂 | 单事务 + 集成测试覆盖「邀请+注册+切换」 |
| V1/V2 双模式测试矩阵 | CI 至少覆盖 `enabled=false` 回归 + `enabled=true` 冒烟 |
| 管理端 bootstrap 无 JWT tenant | 平台安装种子仍写 tenant 1；文档说明 |

## Migration Plan

1. **Phase 0**：本 change 规格归档；`TenantBootstrapService` 抽取
2. **Phase 1**（`-web`）：注册页 + 切换器 UI（Mock）
3. **Phase 2**（`-api`）：register、login 增强、my-list、switch
4. **Phase 3**（`-integrate`）：去 Mock、废弃 invite/accept 前端
5. **Phase 4**：`application-dev.yml` 增加 `enabled=true` profile 供开发验证
6. **回滚**：`enabled=false` 即回 V1 行为；已注册多 tenant 数据保留

## Open Questions

- [ ] 开发环境默认 `enabled=true` 还是保持 `false` 直到 integrate 完成？→ **已确认：dev profile 默认 `enabled=true`（`application-dev.yml`）；生产/base 仍为 `false`**
- [ ] 注册后 IM 欢迎消息 / 邀请通知 → 留 `org-member-invite-notify` change
- [ ] 用户名登录是否允许与 mobile 不同？→ **V2 MVP：username=mobile，后续可改昵称式 username**

## 子 change 切片（实现顺序）

```text
multi-tenant-account-v2          ← 本 change（规划母版）
├── tenant-bootstrap-api         [平台] TenantBootstrapService 抽取
├── account-register-v2-web      注册页 + login 入口 + Mock
├── account-register-v2-api      POST /auth/register
├── tenant-switch-v2-web         企业切换器 UI + Mock
├── tenant-switch-v2-api         my-list + switch + login 多企业
├── account-register-v2-integrate  联调注册闭环
├── tenant-switch-v2-integrate   联调切换 + 登录选企业
└── member-invite-v2-migrate     废弃 invite/accept、invite 租户上下文
```
