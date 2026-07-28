## Context

工作台已有即时消息（单聊/群聊）、云文档、日历；用户头像菜单的偏好（主题、日历默认等）按 `(tenant, user)` 隔离，只影响个人体验。管理端现有企业信息、用户、角色、部门、邀请设置、存储/文件，**没有**按产品组织的企业策略面。

飞书参考：管理后台 → 应用管理 → **产品设置** mega menu → 进入具体产品（即时消息等）→ 左侧子导航（群规模 / 群权限 / 消息权限 / 群组管理）。

现状缺口（相对本规划）：

| 能力 | 现状 |
|------|------|
| 租户级 IM 策略 | 无 |
| 建群/加成员人数上限 | 无强制 |
| 公开群（可搜可加） | 无 |
| 部门群 | 无（需本轮补齐最小可用模型） |
| 群成员导出 | 无 |
| 消息撤回 / 编辑 | 无 |
| 产品设置壳层 / 统一图标 | 无；工作台 `useWorkspaceNav` 内联 Lucide |

## Goals / Non-Goals

**Goals:**

1. 管理端「产品设置」入口 + 三产品（云文档 / 即时消息 / 日历）目录；图标单源。
2. IM 三块组织设置（群规模、群权限、消息权限）可配置、可强制执行；群组管理不做。
3. 补齐设置依赖的工作台能力，使开关真正生效。
4. 云文档 / 日历仅占位，详规另开 change。
5. 以母 change 路线图拆子 change，按 `-web` → `-api` → `-integrate` 交付。

**Non-Goals:**

- 本版不做：群组管理列表、话题群、全员群/超级大群特例表、外部协作/跨租户、飞书级搜索管理。
- 不为云文档/日历写具体设置项、表结构或 API。
- 不把个人偏好迁入产品设置；不反向用产品设置覆盖个人主题等。
- 不引入独立配置中心 / Nacos；策略存本域表。

## Decisions

### D1 — 两层设置边界

```text
┌─────────────────────────────────────────────────────────┐
│ 组织面 · 产品设置（Admin · 本 change）                      │
│  影响：当前租户内所有成员对该产品的策略与管理能力              │
│  例：默认群上限 50；禁止公开群；允许 24h 内撤回               │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│ 个人面 · 用户偏好（已有 user-preference）                   │
│  影响：仅当前租户下该用户的使用习惯                           │
│  例：气泡布局、日历周起始日、是否默认显示任务图层             │
└─────────────────────────────────────────────────────────┘
```

**替代案**：全部塞进 `sys_tenant` JSON → 拒绝（跨域脏数据、IM 规则集需独立行）。

### D2 — 产品目录与图标注册表（前端单源）

`web/src/products/registry.ts`（名称可微调）导出：

| 字段 | 说明 |
|------|------|
| `id` | `im` / `docs` / `calendar`（后续可扩） |
| `label` | 展示名 |
| `icon` | Nuxt UI / Lucide 名，如 `i-lucide-message-circle` |
| `adminSettingsPath` | `/admin/products/im` 等；无详规则为占位 |
| `workspacePath` | `/app/messages` 等（可选，供入口跳转） |
| `adminEnabled` | 是否进入可配置态（IM=`true`；docs/calendar=`placeholder`） |

工作台 `useWorkspaceNav`、产品设置 mega menu、IM 设置面包屑 **均 import 该表**，禁止散落硬编码图标。

**替代案**：后端下发产品目录 → V1 过重；产品清单前端静态即可。

### D3 — 管理端 IA 与路由

```text
/admin/products                    # 产品设置总览（或直接 mega menu 无独立页）
/admin/products/im                 # 即时消息设置壳（二级侧栏）
/admin/products/im/group-size      # 群规模管控
/admin/products/im/group-permission
/admin/products/im/message-permission
/admin/products/docs               # 占位
/admin/products/calendar           # 占位
```

侧栏：在 `useAdminNav` 增加「产品设置」分组（或顶栏产品切换器 + 内容区二级侧栏，对齐飞书「企业管理 > 即时消息」）。推荐：

- 一级：Sidebar 增加「产品设置」→ `/admin/products`（卡片/列表展示三产品）。
- 二级：进入 IM 后内容区左侧子导航（群规模 / 群权限 / 消息权限）。

权限码建议（种子）：

- `im:product-settings:query` / `im:product-settings:update`
- 或统一 `product-settings:im:query|update`（实现时二选一，写入 system 权限种子）。

### D4 — IM 策略数据模型（租户级）

**C 类默认**（读合并代码默认；写 upsert；不为每个租户预插行——或 A 类每租户一行，二选一；**推荐 A 类一行主配置** + 规则集子表，因管理端几乎总会打开保存）：

```text
im_tenant_settings          -- 每租户一行主配置
  tenant_id PK
  group_member_limit_default INT NOT NULL DEFAULT 50
  allow_public_group BOOL NOT NULL DEFAULT true
  dept_group_leave_policy ENUM('ALLOW','DENY') DEFAULT 'ALLOW'
  member_export_policy ENUM('ALLOW_OWNER_ADMIN','DENY_ALL','ALLOW_DESIGNATED')
  member_export_designated_json JSONB  -- 仅 ALLOW_DESIGNATED 时使用
  recall_enabled BOOL DEFAULT true
  recall_within_hours INT DEFAULT 24   -- null=无时限（若产品需要；V1 跟飞书用有限枚举）
  edit_enabled BOOL DEFAULT true
  edit_within_hours INT DEFAULT 24

im_group_size_user_rule     -- 用户规则集
  id, tenant_id
  subject_type ENUM('DEPT','USER')
  subject_id BIGINT
  member_limit INT NOT NULL
  sort_order INT

im_group_size_group_rule    -- 群组规则集
  id, tenant_id
  conversation_id BIGINT    -- 必须 type=group 且本租户
  member_limit INT NOT NULL
```

`im_group` 增列（配套能力）：

| 列 | 用途 |
|----|------|
| `searchable` / `is_public` | 公开群（可搜可加） |
| `group_kind` | `NORMAL` / `DEPT`（部门群最小模型） |
| `dept_id` | 部门群绑定部门；可空 |
| `member_limit_override` | 可选；群组规则集也可只存在规则表，运行时解析，不必冗余 |

**人数上限解析顺序**（建群发起人视角 / 加成员时对目标群）：

1. 若存在该群的 **群组规则集** → 用其 `member_limit`
2. 否则：发起人命中 **用户规则集**（用户规则优先于其所属部门规则；多部门取 **最大** 上限，避免过严）→ 该上限
3. 否则：企业默认 `group_member_limit_default`

说明文案对齐飞书：「全员群、部门群、超级大群、会议群不受此上限约束」——V1 我们仅有对话群 + 计划中的部门群；**部门群不受默认/用户规则上限约束**（与飞书一致）；无全员/会议群则文档注明预留。

预设选项：`5 | 10 | 30 | 50 | CUSTOM(≥2)`；UI 默认选 50。

### D5 — 群权限管控（对齐飞书语义）

1. **进群管理（公开群）**  
   - 允许：群主/管理员可在群设置打开「允许被搜索」；展示公开标记；本企业成员可搜索并申请/直接加入（V1：**直接加入**，审批流不做）。  
   - 禁止：群设置隐藏该开关；已公开群在保存策略后应关闭 `searchable`（或禁止新开，已开保持——**推荐保存时批量关闭**并推送提示）。

2. **部门群管理**  
   - 需先交付 **部门群最小能力**：部门可绑定一个部门群（创建入口可在通讯录/部门详情或建群时选「部门群」）；成员随部门同步的完整方案可分期，V1 可「创建时拉入当前部门成员 + 手动维护」。  
   - 允许退出 / 禁止退出：`leave` API 对 `group_kind=DEPT` 按策略拒绝。

3. **群成员信息导出**  
   - 禁止任何人 / 允许群主与管理员 / 仅指定成员且其为群主或管理员时可导出。  
   - 导出字段对齐名片可见字段（部门、直属上级等——复用已有名片/用户基础 API，不另造字段）。  
   - V1 交付：工作台群成员面板「导出」按钮 + CSV/Excel 下载。

### D6 — 消息权限管控（对齐飞书）

| 项 | 行为 |
|----|------|
| 不允许撤回 | 普通成员不可撤回；**群主/群管理员撤回群内消息不受此开关限制**（与飞书说明一致） |
| 允许撤回 + 时间窗 | 发送后 N 小时内可撤回（默认 24）；超时拒绝 |
| 编辑同理 | 无「群主豁免编辑」飞书原文；V1：**编辑仅发送者本人**，受企业开关与时间窗约束 |

实现要点：

- `im_message` 增：`recalled_at`、`edited_at`、`content_json` 可更新；撤回后列表展示「消息已撤回」占位。
- App API：`POST .../message/recall`、`POST .../message/edit`；WS fanout `message.recalled` / `message.edited`。
- 工作台需读取生效策略（`GET /app-api/im/settings/effective` 或会话上下文附带）以控制菜单显隐；**强制以后端为准**。

### D7 — Admin / App API 草图（IM）

```text
# Admin（需 im 产品设置权限）
GET  /admin-api/im/product-settings
PUT  /admin-api/im/product-settings          # 主配置 + 规则集全量或分块 PUT（实现时在 contract 冻结）

# 规则集也可拆：
PUT  /admin-api/im/product-settings/group-size
PUT  /admin-api/im/product-settings/group-permission
PUT  /admin-api/im/product-settings/message-permission

# App（成员可读生效策略；写路径内嵌校验）
GET  /app-api/im/settings/effective
POST /app-api/im/group/create               # 已有 + limit 校验
POST /app-api/im/group/members/add          # 已有 + limit 校验
POST /app-api/im/group/leave                # 新增或已有扩展
POST /app-api/im/group/public               # 设置 searchable
GET  /app-api/im/group/search               # 搜公开群
POST /app-api/im/message/recall
POST /app-api/im/message/edit
GET  /app-api/im/group/members/export       # 导出
```

选择器复用：`workspace-member-picker`（部门+用户）；管理端群选择器需新做「本租户 group 会话搜索」。

### D8 — 子 change 切片（编码真源在 tasks.md）

| 序 | 子 change | 内容 |
|----|-----------|------|
| 0 | `admin-product-settings-shell-web` | 产品设置入口、图标注册、三产品页；IM 子导航壳；docs/calendar 占位 |
| 1a | `admin-im-group-size-web` → `-api` → `-integrate` | 群规模 UI + 策略 API + 建群/加成员强制 |
| 1b | `im-public-group-v1`（可并入 1c） | 公开群模型 + 搜索加入 |
| 1c | `admin-im-group-permission-web` → `-api` → `-integrate` | 群权限三块 UI + 部门群最小 + 导出 |
| 1d | `im-message-recall-edit-v1` + `admin-im-message-permission-*` | 撤回/编辑能力 + 管理端开关时间窗 |
| 2 | `admin-docs-product-settings-*` | **IM 完成后再立**，本母 change 仅占位 |
| 3 | `admin-calendar-product-settings-*` | 同上 |

并行约束：壳层（0）可先做；1a–1d 可按依赖串行（规模 → 权限依赖公开群/部门群 → 消息权限可与规模并行启动 web）。

### D9 — 云文档 / 日历占位

- 产品目录可点进占位页：标题 +「组织级设置将在后续版本开放」+ 链到工作台产品（可选）。
- `openspec/specs/docs` / `calendar` 仅 ADDED 一条「预留组织设置」需求，无字段清单。
- **禁止**在本母 change 实现期为 docs/calendar 建策略表。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 部门群模型过大拖垮 IM 设置 | V1 最小：`group_kind` + `dept_id` + 退群策略；成员自动同步另开 change |
| 规则集与默认冲突难懂 | UI 复用飞书说明文案；后端单测覆盖解析顺序 |
| 已超员群在下调上限后 | 禁止继续加人；不强制踢人（与多数 IM 一致） |
| 撤回/编辑破坏已读语义 | 撤回后占位仍占 seq；编辑不改 seq，只改 content + edited_at |
| 权限码膨胀 | 产品设置共用少量 permission；子页不拆过多码 |
| 一次做完导致会话失控 | 母 change 只规划；编码严格按子 change tasks |

## Migration Plan

1. Flyway 增 `im_tenant_settings` 与规则表、`im_group`/`im_message` 列；默认值保证旧行为近似「50 人、可公开、可撤回/编辑 24h」。
2. 前端壳层先上线（占位 + IM 表单可先 store 草稿）。
3. API 与强制执行联调后去掉草稿。
4. 回滚：保留列但代码忽略策略 = 回退旧行为；或开关全部回到默认宽松。

## Open Questions（规划默认 · 实现期可微调）

| # | 问题 | 规划默认 |
|---|------|----------|
| 1 | 部门群创建入口 | **先做建群向导「类型=部门群」**；部门详情入口可二期 |
| 2 | 公开群加入 | **直接加入**（无审批流） |
| 3 | 撤回/编辑时间窗 | **枚举**：至少含 `2` / `24` / `168`（小时）；默认 **24**；不做任意小时自由输入 |
| 4 | 群组规则集可选群 | **本企业全部对话群**（管理员视角，非仅「我管理的」） |
| 5 | 导出格式 | **CSV** 先行 |
| 6 | 多部门命中用户规则 | 取各部门规则中的 **最大** 上限；用户规则优先于部门规则 |
