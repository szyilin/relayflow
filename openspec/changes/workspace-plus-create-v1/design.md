## Context

### 现状

- 工作台无飞书式「+」创建枢纽；建群仅消息页、建文档仅文档页、加人仅管理端邀请表单。
- 组织邀请：`POST /admin-api/system/user/invite` 按手机号写 `sys_user` + `sys_tenant_user(status=NOT_JOINED)` + dept/role；已 ACTIVE 用户走 org-assistant Bot 卡；无独立邀请表。
- 通讯录 enrich：外部/申请/星标已持久化，但 **无发起申请**（无 `POST request`），外部列表在真实使用中常空。
- 产品权限：`/app` 全员可用；`/admin` 须 `isAdmin`。邀请写入组织成员属于敏感操作，须设置驱动而非写死。

### 约束

- 0.x 开发期允许破坏性改表（须本 design 写清迁移）；产品 SemVer 前缀仍 `0.1.0`。
- 跨域：system 不直连 `im_`；建群走 im-api；文档走 docs app-api。
- 邀请审批 **不引入 Flowable**（轻量状态字段即可）。
- 短信通道仍 deferred；手机号邀请触达：已有账号走 Bot；无账号依赖注册/链接打开后注册。

## Goals / Non-Goals

**Goals:**

1. Create Hub：「+」四项入口（建群 / 邀企业成员 / 加外部联系人 / 建文档）
2. 飞书对齐的租户邀请设置 + 四渠道（QR / 链接 / 邀请码 / 手机号）
3. 邀请实体与成员结果态分离；支持过期、撤销、审批态
4. 通用成员选择器（组织内钻取、外部标签、我管理的群组）
5. 补齐外部联系人搜索 + 发起申请
6. 母 change 拆可独立交付的子切片与契约草案路径

**Non-Goals:**

- 飞书「+」菜单其余项（录音、多维表、问卷、会议等）
- 邮箱联系人目录、服务台
- 短信/邮件通道实装
- CRDT 共编、文档评论
- 完整 BPM；席位计费
- Phase 2 Gateway/Nacos

## Decisions

### D1. 母 change + 子切片顺序（前端优先）

```text
workspace-plus-create-v1（本母 change · 仅规划）
│
├── 0. workspace-create-hub-web
│      「+」菜单壳 + 四项跳转/占位弹窗 + 成员选择器 UI（可 Mock）
│      contract 草案：openspec/lanes/workspace-create-hub/
│
├── 1. org-invite-settings-web → -api → -integrate
│      管理端「成员与部门 > 邀请设置」；GET/PUT settings
│
├── 2. org-invite-channels-web → -api → -integrate
│      工作台「添加企业成员」四 Tab；QR/链接/码/手机号；接受加入流
│      （依赖 settings；可与 1 的 -api 同 lane 契约分章）
│
├── 3. workspace-contact-add-external-web → -api → -integrate
│      手机/邮箱精确搜索 + POST request；通讯录入口与 Hub 共用
│
├── 4. workspace-member-picker-web →（api 增量）→ integrate
│      建群选人增强：外部、「我管理的群组」；必要时 im-api 补 list-managed-groups
│
└── 5. workspace-create-hub-integrate
       四项入口联调收口；看板 done；archive 母 change
```

母 change `tasks.md` 只做规划验收与开子 change 检查项；业务代码落子 change。

**备选否决**：单切片一次做完 — 邀请域过大，违反「一次一组 tasks」。

### D2. 组织邀请数据模型（推倒 redesign · 核心）

#### D2.1 问题：为何不能继续只用 `NOT_JOINED`

| 能力 | 仅 membership | 独立邀请实体 |
|------|---------------|--------------|
| 链接/码/QR 未定向到人 | 无法表达 | ✅ token/code |
| 过期后失效 | 难（成员行一直在） | ✅ `expires_at` |
| 非管理员审批 | 插层尴尬（已占 dept/role） | ✅ `PENDING_APPROVAL` |
| 同一人多次邀请渠道 | 冲突 | ✅ 多行 invite |
| 审计 inviter / channel | 弱 | ✅ |

#### D2.2 新表（建议）

**`sys_tenant_invite_settings`**（每租户一行）

| 列 | 说明 |
|----|------|
| `tenant_id` PK | |
| `invite_mode` | `OFF` \| `ADMIN_ONLY` \| `ALL_MEMBERS` |
| `non_admin_scope` | `INVITER_DEPT` \| `DESIGNATED_DEPTS`（仅 ALL_MEMBERS 有意义） |
| `non_admin_dept_ids` | JSON/数组：指定部门 ID 列表 |
| `non_admin_expire_days` | int，如 3 |
| `non_admin_require_approval` | bool |
| 公共字段 | `updater`, `update_time`, … |

默认种子：`ADMIN_ONLY`（与今日行为接近：仅管理员可邀）；`non_admin_expire_days=3`；`require_approval=false`。

**`sys_org_invite`**（邀请实体）

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | |
| `channel` | `MOBILE` \| `LINK` \| `QR` \| `CODE` |
| `token` | 链接/QR 共用的高熵 token（唯一）；CODE 渠道可另存短码 |
| `invite_code` | 可选 6–8 位可读码（CODE 渠道；租户内唯一且未过期） |
| `inviter_user_id` | 发起人 |
| `target_mobile` | 手机号邀请时必填；其它渠道可空 |
| `target_user_id` | 若已解析到全局用户 |
| `target_dept_id` | 加入后的默认部门（受 settings 范围约束） |
| `target_role_ids` | JSON 可选；V1 可仅默认成员角色 |
| `status` | 见状态机 |
| `expires_at` | |
| `approved_by` / `approved_at` | 审批通过时 |
| `consumed_at` | 成功加入时 |
| `membership_user_id` | 成功后关联的 user_id |
| 公共字段 | |

**索引**：`(tenant_id, token)` unique；`(tenant_id, invite_code)` partial unique where not deleted/expired；`(tenant_id, target_mobile, status)` 便于查 pending。

#### D2.3 状态机

```text
DRAFT?（可选，一般直接 ACTIVE）
    │
    ▼
ACTIVE ──────过期──────► EXPIRED
    │
    ├─（需审批的非管理员邀）──► PENDING_APPROVAL ─通过─► ACTIVE（或直接 CONSUMED 路径）
    │                              └拒绝─► REJECTED
    │
    ├─被邀请人接受/注册激活──► CONSUMED  (+ 写/更新 sys_tenant_user ACTIVE)
    │
    └─撤销──────────────────► REVOKED
```

简化落地建议：

- 管理员渠道默认：创建即 `ACTIVE`，消费时直接写入/激活 membership。
- 非管理员且 `require_approval=true`：创建为 `PENDING_APPROVAL`；通过后变 `ACTIVE`（或生成可用 token）；拒绝 → `REJECTED`。
- 共享 LINK/QR/CODE：通常一条租户级「开放邀请」记录（可重置 token），多次消费；每次成功加入仍写 membership，invite 行可保持 ACTIVE 直至过期/重置（对齐飞书「重置二维码」）。

#### D2.4 与 `sys_tenant_user` 的关系

```text
sys_org_invite（过程真源）
        │ consume / approve
        ▼
sys_tenant_user（结果真源：ACTIVE / …）
```

- **手机号定向邀请**：可在 invite ACTIVE 时仍预创建 `NOT_JOINED`（兼容现注册激活），或 **推迟到消费时再写 membership**（更干净）。  
  **拍板建议（0.x 推倒）**：消费时才 upsert membership；废弃「邀请即插 NOT_JOINED + 预分配 role」的旧路径。迁移：现存 `NOT_JOINED` 行生成对应 `sys_org_invite(channel=MOBILE, status=ACTIVE)` 并保留可激活。
- 注册 `activatePendingInvites`：改为扫 `sys_org_invite`（mobile 匹配 + ACTIVE + 未过期）再写 membership。
- Bot 卡：挂 `inviteId`；accept handler 走 consume。

#### D2.5 旧 API 兼容

| 旧 | 新 |
|----|-----|
| `POST /admin-api/system/user/invite` | 内部转调 `OrgInviteService.createMobileInvite`（管理员）；保留路径一版本避免管理端白屏，contract 标注委托 |
| Deprecated `member-invite/*` | 删除或改为新 accept；前端已不接 |

### D3. 邀请设置与权限门禁

```text
invite_mode=OFF
  → 任何人不可创建新邀请；已发 LINK/QR/CODE 失效（status→REVOKED 或校验时拒绝）

invite_mode=ADMIN_ONLY
  → 仅 isAdmin（且建议 permission `system:user:invite` 或沿用 `system:user:create`）可邀
  → 工作台「+」中「添加企业成员」仅 isAdmin 可见

invite_mode=ALL_MEMBERS
  → ACTIVE 成员可见入口
  → 非管理员：校验 scope（本部门 / 指定部门列表）、expires、审批标志
  → 管理员不受 non_admin_* 限制（或仅受 OFF 关闭）
```

**App 只读投影**：`GET /app-api/system/org-invite/capability`  
返回 `{ canInvite: boolean, mode, … }` 供菜单显隐；**不**把完整 settings 泄露给非管理员（指定部门列表可按需裁剪）。

管理端：`GET/PUT /admin-api/system/invite-settings`，permission 建议 `system:dept:update` 或新建 `system:invite-settings:update`（实现时在 RBAC 种子补齐）；导航挂在用户/部门附近：`/admin/system/invite-settings`，文案「邀请设置」。

### D4. 四渠道行为

| 渠道 | 创建 | 加入 |
|------|------|------|
| **手机号** | 输入手机号 + 目标部门（受 scope）；生成 invite；已有 ACTIVE 用户 → Bot；无用户 → 待其注册/打开链接 | 注册激活或 Bot accept → consume |
| **企业链接** | 租户共享 invite（重置换 token）；复制 URL 如 `/app/invite/join?t=…` | 未登录 → 登录/注册 → consume；已是成员 → 提示已加入 |
| **企业二维码** | 与 LINK 同一 token 的 QR 图（前端渲染或后端返回 dataURL）；失效时间展示 + 重置 | 同 LINK |
| **企业邀请码** | 短码；用户在加入页输入 | 校验码 → consume |

内网/离线：链接主机来自配置 `relayflow.public-base-url`（或前端 origin）；无公网亦可本机复制。

### D5. Create Hub UI 落点

- 入口：`WorkspaceRail` / `WorkspaceProfileCard` 头像旁「+」（与飞书一致侧）；**不要**塞进通讯录分类导航顶栏独占。
- 菜单项顺序：创建群组、添加企业成员、添加外部联系人、创建文档。
- 「添加企业成员」受 `canInvite`；其余三项对有效成员常显。

### D6. 成员选择器（建群）

复用并增强 `ImCreateGroupModal` 或抽出 `WorkspaceMemberPicker`：

- 数据源：组织内（dept tree + list-by-dept）、外部（external/list）、我管理的群组（需 `GET /app-api/im/group/managed` 或会话列表过滤 owner/admin）
- UX：搜索框、面包屑、部门「下级」、外部「外部」标签、多选右侧已选
- V1 建群仍只提交 `memberUserIds[]`；从「我管理的群组」选中时 **展开成员 ID** 并入已选（对齐飞书批量加人语义的简化版）

### D7. 添加外部联系人

```text
搜索：确切 mobile 或 email（禁止模糊搜全站）
结果：该标识绑定的全部账号（跨租户展示「企业名」标签）
操作：添加 → POST /app-api/system/contact/request { targetUserId, message? }
对方：既有 request/list + accept；本期补 reject
已添加：按钮「已添加」不可再发
```

搜索 API 建议：`GET /app-api/system/contact/search-account?mobile=` 或 `?email=`（二选一），返回可能多人。

**隐私**：仅精确匹配；不返回未注册手机号的枚举列表（无结果空态）。

### D8. 创建文档

Hub 点击 → `router.push('/app/docs')` + 调用既有 `docs.createPage()`（或 query `?create=1`）；不新建 docs 域模型。

### D9. 模块与契约

| Lane | 路径 |
|------|------|
| create hub | `openspec/lanes/workspace-create-hub/contract.md` |
| org invite | `openspec/lanes/org-invite/contract.md`（settings + channels + consume） |
| contact add | 扩展 `workspace-contacts-enrich` 或新 `workspace-contact-add` |

Flyway 建议序（落地时顺延）：`V0.1.0.41__org_invite_settings.sql`、`V0.1.0.42__org_invite.sql`、`V0.1.0.43__migrate_not_joined_to_org_invite.sql`。

端口/环境：无新端口；可选 `relayflow.public-base-url` 用于邀请链接绝对 URL。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 推倒 `NOT_JOINED` 邀请路径导致注册激活回归失败 | 迁移脚本 + 双读窗口（先读 invite，fallback membership）一版本；单测覆盖 register |
| 开放 LINK/QR 被滥用 | settings 可 OFF；重置 token；可选后续加人数上限（本期可不做） |
| 非管理员审批无通知 | V1 管理端待审批列表 + 可选 Bot 提醒管理员；不做完整收件箱 |
| 成员选择器复杂度 | 先 web Mock 通 UX，再接 API；「我管理的群组」可二期若 im 缺 owner 字段 |
| 精确手机号搜索撞隐私 | 仅精确匹配；错误文案统一「未找到」 |
| 邀请设置与 isAdmin 定义不一致 | 文档钉死：ADMIN_ONLY = `isAdmin`；细粒度 permission 可选加强 |

## Migration Plan

1. 加 settings 表并 seed `ADMIN_ONLY`
2. 加 `sys_org_invite`；迁移现有 `NOT_JOINED` → mobile invites
3. 切换 invite/register/Bot 到新服务；旧 `user/invite` 委托
4. 前端：管理端设置页 → Hub → 四渠道 → 外部添加 → 选人器
5. **回滚**：关闭 `invite_mode=OFF`；DB 回滚须同意；保留 `sys_tenant_user` 不删

## Open Questions

1. **共享 LINK/QR 是否限制加入人数？** 建议 V1 不限制，靠 OFF/重置。
2. **非管理员审批的管理端入口**：独立「待审批邀请」页 vs 用户列表筛选项？建议独立轻量页挂邀请设置旁。
3. **邀请码长度与字符集**：建议 6 位数字或 Crockford Base32 8 位。
4. **`POST user/invite` 是否长期保留？** 建议保留为兼容别名 ≥1 个次要版本。
5. **「我管理的群组」若当前无 owner 字段**：是否用「我创建的群」近似？实现前查 `im_group` / member role 后在子 change 拍板。
