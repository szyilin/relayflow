## Context

- 母 change `admin-product-settings-v1` 已交付产品设置壳层与 IM 组织策略；日历页仍为占位。
- 工作台日历已具备：`PRIMARY`/`OWNED` 容器、邀约、`cal_calendar_share`（READ）、RRULE、拖拽、「我的任务」图层。
- 飞书对照（帮助中心 + 实机）：
  - **全员日历**：管理员在管理后台创建；可见范围内成员**自动订阅**；适合放假安排、全员宣讲等。
  - **日历设置**：侧栏「我管理的 / 我订阅的」行 `···` → 分享 / 日历设置 / 改色等；设置面板含基本信息、共享人。
  - **显示色分层**：管理员（或所有者）设的日历默认色自动映射到所有订阅者；订阅者可改**本机显示色**，仅自己可见；无名称/描述编辑权时仍可改色（飞书 tooltip「暂无编辑权限」针对名称等字段，色仍可点）。

现状缺口：无全员日历；侧栏为 hover「共享」「删除」散按钮，无统一设置面板；无本机色覆盖。

## Goals / Non-Goals

**Goals**

1. 管理端 `/admin/products/calendar` 可配置：组织策略（V1 薄集）+ **全员日历** CRUD / 可见范围。
2. 工作台：侧栏 `···` 收敛分享与设置；「日历设置」面板（按角色读写）。
3. 默认色（日历实体 `color`）vs 本机色（每用户覆盖）语义清晰并落库。
4. 与 `user-preference.settings.calendar`、既有 READ 共享边界清晰。
5. 母 change 规划真源；实现拆子切片，前端优先。

**Non-Goals**

- 公共日历、五级默认权限 ACL、组织外、导出 / CalDAV、会议室日历。
- 一次 apply 吞全栈。
- 用组织策略覆盖个人主题 / 周起始等 preference。

## Decisions

### D1 — 三层设置边界

```text
┌──────────────────────────────────────────────────────────────┐
│ 组织面 · 产品设置 Admin（本 change）                            │
│  全员日历 CRUD、可见范围；可选组织级日历策略（见 D3）              │
└──────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│ 日历实体面 · cal_calendar                                      │
│  name / description / color（默认色）/ type                     │
│  所有者或全员日历管理员可改；订阅者只读（本机色除外）              │
└──────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────┐
│ 个人面                                                         │
│  A. user-preference.settings.calendar — 周起始、默认时长等     │
│  B. 本机显示色 — 每 (tenant,user,calendar) 可选覆盖；不进 A     │
└──────────────────────────────────────────────────────────────┘
```

**替代案**：本机色塞进 preference JSON → 拒绝（按日历多行、与列表耦合重）。

### D2 — 管理端 IA

```text
/admin/products/calendar                 # 日历产品设置壳（二级侧栏）
/admin/products/calendar/org             # 组织策略（V1 薄集，可与全员同页分组）
/admin/products/calendar/all-staff       # 全员日历列表与创建/编辑
```

- 产品注册表：`calendar.adminEnabled = true`（去掉 placeholder）。
- 权限码（对齐 IM）：`calendar:product-settings:query` / `calendar:product-settings:update`；全员日历写操作复用 `update`（V1 不拆过多码）。
- 全员日历 UI：名称、描述、**默认颜色**、可见范围（全员 / 指定部门 / 指定用户组——V1 至少「全员」+「指定用户」；部门若 system-api 已就绪则做）。

### D3 — 组织策略 V1 薄集

V1 **不以**飞书日历管理后台全部开关为目标。首批建议：

| 项 | 说明 | 默认 |
|----|------|------|
| （隐含）全员日历管理 | 本产品设置核心能力 | — |
| `allowMemberCreateOwnedCalendar` | 是否允许成员新建 OWNED 日历 | `true` |
| （可选占位）后续：组织默认周起始强制等 | **本版不做强制**，避免与 preference 打架 | — |

若实现期认为组织策略页过空，可将「组织策略」合并为全员日历页顶部说明 + 单开关，不必强行两路由。

持久化：`cal_tenant_settings`（每租户一行，A 类 upsert；C 类读合并代码默认亦可，**推荐 A 类一行** 与 IM 一致）。

### D4 — 全员日历数据模型

扩展 `cal_calendar.type`：

| type | 含义 |
|------|------|
| `PRIMARY` / `OWNED` | 既有 |
| `ALL_STAFF` | 全员日历；**仅**管理端创建；`owner_user_id` = 创建管理员用户（或系统占位约定，实现选一并写死） |

可见范围（推荐独立表，避免把 ACL 塞 JSON 难查）：

```text
cal_all_staff_scope
  id, tenant_id, calendar_id
  scope_type   -- ALL | USER | DEPT（V1 至少 ALL + USER）
  scope_id     -- USER/DEPT 时为目标 id；ALL 时 null
```

**自动订阅语义（关键）**：

- **不**为全员逐人插入 `cal_calendar_share`（规模与离职同步成本高）。
- list 日历时：若当前用户落在某 `ALL_STAFF` 的 scope 内 → 以订阅身份出现在「我订阅的」（或侧栏等价分组）。
- 事件可见性：与 READ 共享同等——范围内成员可读该日历上事件详情。
- 退出组织 / 离开部门：下次 list 自然不可见（scope 解析）；本机色行可留或 GC（best-effort）。

**替代案**：物化 `cal_calendar_share` 全员行 → 拒绝（人多时写放大）。

与既有共享关系：

| | 整日历 READ 共享 | 全员日历 |
|--|------------------|----------|
| 创建 | 所有者 App | 管理员 Admin |
| 订阅 | 显式 share 行 | scope 解析自动 |
| 侧栏 | 「共享给我的」 | 「我订阅的」（可与共享合并展示，须用 type/来源区分） |

V1 UI 建议：侧栏分组对齐飞书——「我管理的」|「我订阅的」（含 share + all-staff）；或「共享给我的」保留并把 all-staff 放入「我订阅的」。design 冻结：**「我管理的」+「我订阅的」**；既有「共享给我的」文案迁入「我订阅的」。

### D5 — 本机显示色

```text
cal_calendar_user_pref
  id, tenant_id, user_id, calendar_id
  display_color  VARCHAR(32) NULL  -- null = 使用日历默认 color
  UNIQUE (tenant_id, user_id, calendar_id) WHERE deleted=0
```

解析规则（list / 网格渲染）：

```text
effectiveColor = userPref.display_color ?? calendar.color
```

- 改 **默认色**（`cal_calendar.color`）：仅所有者 / 全员日历管理员；影响所有未覆盖本机色的用户。
- 改 **本机色**：任意对该日历有订阅可见权的用户（含 READ 共享、全员自动订阅）；**不**改 `cal_calendar.color`。
- 无名称/描述编辑权时：设置面板名称/描述只读（可灰态 +「暂无编辑权限」）；**颜色控件仍可写本机色**。
- 清除本机色：API 允许置 null，恢复默认映射。

**不做**：本机色同步到其他设备以外的「组织默认」；本机色进 preference。

### D6 — 工作台「日历设置」与 `···` 菜单

**侧栏行（我管理的 / 我订阅的）**：

```text
···
├── 仅显示此日历          -- 前端：sole visible；可无后端
├── 分享                  -- 仅所有者；打开共享人管理（可与设置内共享人同一组件）
├── 修改颜色              -- 快捷改本机色（订阅）或默认色（所有者，若产品选择）
│                            推荐：快捷一律写本机色；默认色仅在「日历设置」改 —— 降低误伤
├── 日历设置              -- 打开设置面板
└── （所有者非 PRIMARY）删除日历 -- 可放设置内，菜单可不重复
```

**拍板（本 design）**：`···` →「修改颜色」= **本机色**；「日历设置」内颜色：所有者改默认色，订阅者改本机色（控件旁文案区分）。

**日历设置面板字段**：

| 字段 | 所有者 / 全员管理员 | 订阅者（READ / 自动订阅） |
|------|---------------------|---------------------------|
| 名称、描述 | 可编辑 | 只读 |
| 颜色 | 默认色 | 本机色 |
| 共享人列表 | 可增删（OWNED/PRIMARY）；ALL_STAFF 由 Admin 管范围，App 只读展示「订阅人数」可选 | 只读 |
| 删除日历 | 非 PRIMARY 的 OWNED | 无；可「退订」仅对显式 share，全员不可退订 |

新建 OWNED 日历：可复用同一表单（无「默认权限」五级块）。

### D7 — API 草图（contract 在 -web 冻结）

Admin：

```text
GET/PUT  /admin-api/calendar/product-settings
GET/POST /admin-api/calendar/all-staff
GET/PUT/DELETE /admin-api/calendar/all-staff/{id}
```

App：

```text
# 既有 calendar / share / event
PUT  /app-api/calendar/calendar/update          # 所有者：name/color/description
PUT  /app-api/calendar/calendar/{id}/display-color   # 本机色；body { displayColor | null }
GET  日历 list 响应增加：defaultColor、displayColor?、effectiveColor、source(OWNED|SHARED|ALL_STAFF)
```

事件 list 着色：客户端用 `effectiveColor`（或等价字段）。

### D8 — 交付切片

```text
admin-calendar-product-settings-v1          ← 本母 change（规划真源）
├── workspace-calendar-settings             侧栏 ··· + 设置面板 + 本机色（web→api→integrate）
├── admin-calendar-org-settings             占位→组织策略薄集 + 注册表（可与下项合并）
└── admin-calendar-all-staff                全员日历 Admin + App 自动订阅可见
```

推荐实现顺序：

1. **workspace-calendar-settings**（用户感知强、复用现有 share；先打通 `···` + 设置 + 本机色，含对 SHARED）
2. **admin-calendar-all-staff**（+ 组织壳 / 薄策略）
3. integrate 与看板收尾

可并行：admin 壳 web 与 workspace settings web。

### D9 — 与 meeting / preference 边界

- meeting 预约仍走 `CalEventApi`；全员日历上也可挂日程（管理员/授权写路径 V1：仅管理员身份在 Admin 或 App 以所有者身份写——**V1 规定**：全员日历事件由拥有管理权的用户在工作台对该日历有写权时创建；自动订阅者只读事件。实现上 ALL_STAFF 的 `owner_user_id` 可写；其他管理员是否可写另开，**默认仅 owner_user_id**）。
- `user-preference` 不出现本机色字段。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 全员 scope 每次 list 解析成本 | 租户内全员日历数量极少；scope 行少；可缓存 |
| 「修改颜色」误改默认色 | D6：快捷菜单只写本机色 |
| 与「共享给我的」文案迁移 | 规格写清分组；UI 一次改到位 |
| 权限模型半套（无游客/编辑者） | 规格显式 Non-Goal；避免暗示五级 ACL |
| 母 change 过大 | 强制子切片；tasks 分组勾选 |

## Migration Plan

1. Flyway：扩展 `cal_calendar.type` CHECK；加 `cal_all_staff_scope`、`cal_calendar_user_pref`、`cal_tenant_settings`（按切片增量，可多版本号）。
2. 权限种子：`calendar:product-settings:query|update`。
3. 前端：先 settings UX（无 ALL_STAFF 时本机色对 SHARED 生效）；再 Admin 全员。
4. 回滚：关 Admin 入口 / 停用 ALL_STAFF 创建；表可保留。

## Open Questions（已拍板）

| # | 问题 | 结论 |
|---|------|------|
| 1 | V1 是否做五级默认权限？ | **否** |
| 2 | 本机色存哪？ | `cal_calendar_user_pref`，不进 preference |
| 3 | 快捷「修改颜色」改谁？ | **本机色** |
| 4 | 全员用 share 行物化？ | **否**，scope 解析 |
| 5 | 公共日历？ | **不做**，另立项 |
| 6 | 组织策略首批？ | 薄集：`allowMemberCreateOwnedCalendar` + 全员日历管理；可合并单页 |

## 参考

- 飞书：[创建和管理个人日历](https://www.feishu.cn/hc/zh-CN/articles/360049067562)、[订阅日历](https://www.feishu.cn/hc/zh-CN/articles/360024761294)、[Admin 全员日历](https://www.feishu.cn/hc/en-US/articles/360049067696-admin-create-and-manage-all-staff-calendars)
- 开放平台日历类型说明：[简介](https://open.feishu.cn/document/quickly-build-a-feishu-calendar-schedule/introduction?lang=zh-CN)
- 既有：`openspec/specs/calendar/spec.md`、`docs/dev/workspace-ui-patterns.md` 日历节、`openspec/lanes/workspace-calendar/contract.md`
