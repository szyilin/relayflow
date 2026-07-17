# 设计：工作台日历 V1（workspace-calendar-v1）

## Context

- 产品参考：飞书日历（容器日历 + 日程 + 参与人 + 助手提醒）；开放平台 calendar-v4 资源模型
- 工作台 UI：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)
- 架构：新官方域 `relayflow-module-calendar`（对标 `task`），表前缀 **`cal_`**
- 触达：[`im-bot-dm`](../../lanes/im-bot-dm/contract.md) / `ImBotApi`；种子 bot 对标 `task-bot`
- 偏好：已有 `sys_user_preference`（C 类）；日历设置进全局设置窗，**非**日历页内独立设置
- 拍板：邀约要；整日历共享 V1.1；无任务图层；无 RRULE；设置进个人全局偏好

## Goals / Non-Goals

**Goals:**

- 多日历图层 + 日/周/月 + 快捷创建/编辑删除日程（含全天）
- 租户内成员邀约 + Bot 通知 + 被邀请人可见
- 开始前提醒 + 深链 `/app/calendar?eventId=`
- 全局设置「日历」：周起始、默认时长、默认提醒等，按 `(tenant_id, user_id)` 持久化

**Non-Goals:**

- 订阅同事日历、忙闲、整日历 ACL、公共/全员日历
- 会议室、视频会议、CalDAV/第三方、重复日程、拖拽改期、任务虚拟日历

## Decisions

### D1：Maven 与表前缀

```text
relayflow-module-calendar/
  ├── relayflow-module-calendar-api/
  └── relayflow-module-calendar-biz/
```

- 表：`cal_calendar`、`cal_event`、`cal_attendee`
- `docs/dev/database.md` 增补 `cal_`（并注明已有 `task_`）
- `calendar-biz` → `system-api`（校验成员）、`im-api`（`ImBotApi`）；禁止直查 `sys_`/`im_`/`task_` 表

**备选**：塞进 `task` — 否（语义与成长路径不同）。

### D2：日历容器与主日历 ensure（A 类）

| 字段（概念） | 说明 |
|--------------|------|
| `type` | `PRIMARY` \| `OWNED`（V1；共享类型留 V1.1） |
| `owner_user_id` | 拥有者 |
| `name` / `color` / `description` | 展示 |
| `tenant_id` | JWT 租户 |

- 成员激活 / 首次进入日历：本域 **ensure PRIMARY**（名称默认「我的日历」）；遵循 [`default-data-provisioning.md`](../../../docs/dev/default-data-provisioning.md) A 类，禁止跨域上帝填充器
- PRIMARY **不可删除**；OWNED 可删（删前需处理其下事件：禁止删除非空或级联软删 — **推荐：有未删事件则拒绝删除**）

### D3：日程与可见性查询

`cal_event`：`calendar_id`、`title`、`description`、`start_time`/`end_time`（`TIMESTAMPTZ`）、`all_day`、`organizer_id`、提醒字段（如 `remind_before_minutes`；全天可用「当天本地 08:00」策略存在设计注释与代码默认）、`status`（`CONFIRMED`/`CANCELLED`）

**列表可见集合**（当前用户 U）：

1. U 拥有的日历上的事件（且侧栏勾选过滤在前端或 API `calendarIds`）
2. **OR** U 作为 `cal_attendee` 的事件（邀请我的；**不**要求拥有对方日历）

被邀请人 V1：**不可**改时间/标题/参与人；可 **接受/拒绝**（`response`：`NEEDS_ACTION`/`ACCEPTED`/`DECLINED`）。拒绝后仍可在列表以弱化样式出现或按偏好隐藏——V1 默认仍返回，前端降低亮度；「隐藏已拒绝」可后续挂 preference。

组织者：完整 CRUD；取消时 `CANCELLED` + 通知参与人。

**不做 RRULE**：无重复展开表。

### D4：参与人与通知

- `cal_attendee`：`(event_id, user_id)`、`role`（`ORGANIZER`/`ATTENDEE`）、`response`
- 仅租户内有效成员；创建/更新时经 `system-api` 校验
- Bot：`calendar-bot`（`type=system`），`ImBotApi.send` SINGLE
  - 邀约：`dedupeKey` 如 `CAL_INVITE:{eventId}:{userId}`
  - 变更/取消：新 key 或带版本后缀
  - 开始前提醒：`CAL_REMIND:{eventId}:{userId}`；写路径尝试 + 列表/定时补偿（对标 task due）
- 失败 **不**阻断 CRUD（best-effort）
- 深链：`route=/app/calendar?eventId=`

**备选**：无邀约只做个人日历 — 已拍板否决。

### D5：App API（草图）

前缀：`/app-api/calendar/`

| 资源 | 方法 |
|------|------|
| calendar | `list` / `create` / `update` / `delete` |
| event | `list?from=&to=&calendarIds=` / `get` / `create` / `update` / `delete` |
| attendee | 可嵌在 event create/update；或 `respond` 单独接口 |

鉴权：JWT + 有效成员；**无**管理面 permission。

错误码示例：`CALENDAR_NOT_FOUND`、`CALENDAR_FORBIDDEN`、`EVENT_NOT_FOUND`、`EVENT_FORBIDDEN`、`PRIMARY_CALENDAR_DELETE_FORBIDDEN`。

### D6：用户偏好 `settings.calendar`

扩展现有 preference JSON（schemaVersion 递增规则：兼容合并未知键；新增键给代码默认）：

```json
{
  "calendar": {
    "weekStartsOn": 0,
    "defaultEventDurationMinutes": 30,
    "defaultRemindBeforeMinutes": 5,
    "allDayRemindTime": "08:00",
    "dimPastEvents": true
  }
}
```

| 键 | 含义 |
|----|------|
| `weekStartsOn` | `0`=周日 … `6`=周六（与常见 date-fns 一致） |
| `defaultEventDurationMinutes` | 快捷创建默认时长 |
| `defaultRemindBeforeMinutes` | 非全天默认提醒 |
| `allDayRemindTime` | 全天提醒钟点（本地墙钟，V1 按浏览器/租户展示时区理解） |
| `dimPastEvents` | 前端是否降低已结束亮度 |

UI：`WorkspaceSettingsPanel` 左栏增加 **「日历」**（与「通用」并列）；**不**在 `/app/calendar` 页内做第二套设置真源。

侧栏日历勾选可见性：V1 可用前端 localStorage；正式可后续并入 preference（本母版不强制）。

### D7：前端

| 项 | 选择 |
|----|------|
| 路由 | `/app/calendar`；Rail 增加日历 |
| 壳层 | `#panel`：迷你月历 +「我管理的」列表 + 添加日历；主区：顶栏 + 日/周/月网格 |
| 视图库 | **自研网格** + `date-fns` / `@internationalized/date` + Nuxt UI；不上 FullCalendar |
| 创建 | 点格/按钮 → 快捷弹层；「更多」含参与人、提醒 |
| 无 | 「我的任务」图层；会议室 Tab |

Store：`stores/calendar.ts` → `api/app/calendar.ts`；设置走 `userPreference`。

### D8：切片边界

| 子 change | 范围 |
|-----------|------|
| `calendar-schema-v1` | pom、Flyway、codegen、`calendar-bot` 种子、空模块可启动 |
| `workspace-calendar-web` | UI + store 临时数据 + `openspec/lanes/workspace-calendar/contract.md` + 设置窗日历分类（可先本地合并默认） |
| `workspace-calendar-api` | REST + ensure + Bot + preference 默认键后端合并 |
| `workspace-calendar-integrate` | 去临时数据；E2E；看板 **done** |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 邀约可见性与「仅自己的日历勾选」混淆 | 文档与 UI：邀请事件始终进入主网格（或单独图例）；勾选只过滤自有日历 |
| 全天 + 时区边界 | 存 UTC 区间；全天用日期语义在 API 约定（contract 写清） |
| Bot 刷屏 | dedupeKey；变更合并通知文案 |
| 自研周视图复杂度 | V1 不做拖拽；固定小时轴高度与绝对定位块 |
| preference 与日历域双源 | 设置只进 preference；事件提醒快照写在 `cal_event` 上（创建时用默认填入，之后改设置不影响已建事件） |

## Migration Plan

1. Flyway 增量建表 + 种子 `calendar-bot`
2. 部署含 `calendar-biz` 的 server；旧客户端无日历页无影响
3. 回滚：去掉模块依赖与路由；表可留空

## Open Questions

无（拍板已闭合）。V1.1 另开：订阅同事、busy/free、整日历 ACL、RRULE、拖拽。
