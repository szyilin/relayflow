# 设计：工作台日历 V1.1（共享 / RRULE / DnD）

## Context

- V1 基线已归档：`cal_calendar` / `cal_event` / `cal_attendee`；可见性 = 自有日历 ∪ 被邀约
- UI：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md) 日历页；契约 [`workspace-calendar/contract.md`](../../lanes/workspace-calendar/contract.md)
- 触达：继续 `calendar-bot` + `ImBotApi`（共享变更是否通知可在 share 切片拍板；默认 best-effort）

## Goals / Non-Goals

**Goals:**

- 租户内整日历共享与订阅（侧栏图层 + list 可见）
- 常用 RRULE + 最小例外模型；区间查询返回展开实例
- 日/周拖拽改开始时间与时长（组织者；订阅只读层不可拖）

**Non-Goals:** 见 proposal。

## Decisions

### D1：共享模型（share）

推荐表（名可微调，须 `cal_`）：

| 概念 | 说明 |
|------|------|
| `cal_calendar_share` | `(tenant_id, calendar_id, grantee_user_id)`；`permission`：`READ` \| `BUSY_FREE`（V1.1 至少 READ；BUSY_FREE 可同表预留） |
| 授予方 | 仅日历 `owner_user_id` |
| 撤销 | 软删 share 行；订阅方图层消失 |

**可见性扩展**（用户 U）：

1. U 拥有的日历上的事件  
2. U 作为 attendee 的事件  
3. **NEW** U 被授予 READ 的日历上的事件（侧栏勾选过滤）  
4. BUSY_FREE：仅返回忙闲块（无标题/描述）— 若 V1.1 时间紧可只做 READ，BUSY_FREE 标后续

**备选**：不做 ACL 表、仅「复制事件」— 否（飞书语义是订阅图层）。

API 草图：

| 资源 | 方法 |
|------|------|
| share | `list`（我共享出去的 / 共享给我的）· `create` · `delete` |
| calendar list | 增加 `SHARED` 或 `subscribed=true` 日历条目 |

前端：侧栏「共享给我的」分组；勾选驱动 `calendarIds` / 本地 visible。

### D2：RRULE（rrule）

| 字段/表 | 说明 |
|---------|------|
| `cal_event.rrule` | 文本 RRULE（或结构化 JSON）；空 = 单次 |
| `cal_event_exception`（推荐） | `(master_event_id, original_start, …)`：取消实例或覆盖字段 |
| 展开 | 服务端在 `list(from,to)` **按窗口展开**；响应带 `instanceStart` / `masterEventId` / `isException` |
| 编辑范围 | `THIS` \| `THIS_AND_FUTURE` \| `ALL`（V1.1 至少 THIS + ALL；THIS_AND_FUTURE 优先实现） |

常用规则：每天 / 每周（byday）/ 每月（bymonthday）；`COUNT` 或 `UNTIL` 二选一。

**不做**：复杂 BYSETPOS、跨时区例外边缘 case 穷尽；第三方库选型在 `-api` 时定（Java 侧优先成熟 RRULE 库）。

提醒：对展开实例在提醒窗口内触发；dedupe 含 `instanceStart`。

### D3：拖拽（dnd）

| 交互 | 行为 |
|------|------|
| 日/周 | 拖动块改 `start`（保持 duration）；上下拉边改 `end` |
| 月视图 | V1.1 可选：拖到另一天（整日平移）；不做则仅日/周 |
| 权限 | 仅 `viewerRole=ORGANIZER`；共享 READ / 邀约 ATTENDEE 不可拖 |
| API | 优先复用 `PUT …/event/update`；若 payload 过大可加 `PATCH …/event/reschedule`（仅时间） |
| 乐观 UI | store 乐观更新 + 失败回滚 toast |
| 与 RRULE | 无 rrule：直接改单次；有 rrule：默认 `THIS` 例外，或弹层选范围（与编辑器一致） |

### D4：切片依赖与并行

```text
share:   schema 可先行；web 侧栏分组 + api ACL
rrule:   依赖 V1 event 模型；与 share 数据正交，可在 share integrate 后或并行 schema
dnd:     web 可与 share 并行（先单次事件）；rrule 落地后补「改此实例」
```

### D5：契约与看板

- 扩展 lane contract 分节：`## Share` / `## RRULE` / `## DnD`，或拆 `workspace-calendar-share/contract.md` 等
- 看板三行：`workspace-calendar-share` / `-rrule` / `-dnd`

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| RRULE 展开性能 | 窗口限制（如 ±2 月）；禁止无 UNTIL 的无限展开在过大窗口 |
| 共享泄露标题 | BUSY_FREE 单独 permission；默认共享为 READ 时产品文案说清 |
| 拖拽与滚动冲突 | 指针捕获 + 阈值阈值；沿用现有网格坐标系 |

## Open Questions（实现前拍板）

1. BUSY_FREE 是否必须进本母 change 第一刀，还是仅 READ？
2. 共享是否发 `calendar-bot` 通知？
3. 月视图是否支持拖拽改日？
