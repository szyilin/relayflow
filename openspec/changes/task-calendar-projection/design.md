# 设计：任务 × 日历投影联动

## Context

- 现状：`task` 与 `calendar` 已分域上线；任务有 `due_time` + `task-bot` 提醒；日历有多图层日程、邀约、共享、RRULE。日历 V1 拍板「无任务图层」，侧栏与网格只展示 `cal_*`。
- 约束：跨域禁止 `*-biz → *-biz`、禁止跨域 Mapper；前端优先纵向切片；自部署 / 内网无额外 SaaS 依赖。
- 产品对标：壳层飞书；联动 TickTick「截止日进日历视图」；不做 Motion AI、不做 ClickUp 全能 PM。

## Goals / Non-Goals

**Goals:**

- 日历侧栏「我的任务」虚拟图层（可勾选；偏好可默认开关）
- 日/周/月网格投影当前用户带截止日的 `TODO` 任务（视觉与日程可区分）
- 点击 → `/app/tasks?taskId=` 深链
- `task` 提供按截止时间窗的只读查询；无新表、不写 `cal_event`

**Non-Goals:**

- 任务自动生成日程 / 合并实体
- 拖任务改截止日上日历网格（可后续）；拖成时间盒占忙闲
- AI 排期、清单/指派/子任务扩展
- 服务端强制把任务塞进 event list 响应（本版采用前端并行拉取）

## Decisions

### D1：投影而非写入日程

- **选择**：任务仍只存在于 `task_item`；日历仅渲染投影。
- **理由**：语义不同（截止点 vs 时段邀约）；避免双写与「完成任务是否删日程」歧义。
- **备选**：创建任务时同步插 `cal_event` — 否（耦合、删改复杂）。

### D2：前端并行拉取（主路径）

- **选择**：`/app/calendar` 在图层勾选时并行调用：
  - 既有 `GET /app-api/calendar/event/...`（日程）
  - 新 `GET /app-api/task/item/due-range?from=&to=`（任务投影）
- **理由**：改动面小；calendar-biz 无需依赖 task；与现有 store 模式一致；离线/内网无新端口。
- **备选**：`calendar` 聚合 endpoint 调 `TaskItemApi` — 可作为后续优化；本 change 不强制。
- **契约**：`-web` 在 `openspec/lanes/task-calendar-projection/contract.md` 冻结字段。

### D3：due-range 查询语义

- **范围**：当前 JWT 用户为 `assignee_id`；`status=TODO`；`due_time` 落在 `[from, to]`（闭或半开在 contract 写死，建议半开 `[from, to)`）；租户 = JWT tenant。
- **不含**：无 `due_time`、已 `DONE`、他人任务。
- **全天展示**：任务在日/周按 `due_time` 落点渲染为标记/条（非强制 1h 块）；月视图用点/徽标。精确像素在 UI 实现，spec 只要求「可见且可区分」。

### D4：跨域 TaskItemApi

- **选择**：本 change **可选**扩展 `TaskItemApi`（与 app-api 同语义），供未来 calendar/BPM 复用；**日历页不依赖**服务端聚合。
- **理由**：保持 `*-api` 边界就绪，但不扩大 V1 实现面。

### D5：偏好键 `showTaskLayer`

- **选择**：`settings.calendar.showTaskLayer` 默认 `true`；设置窗「日历」增加开关；侧栏勾选为会话内覆盖或写回偏好（实现选一，推荐：侧栏勾选写回 preference，与日历勾选图层体验一致则仅 session — **推荐侧栏勾选仅影响当前会话，设置窗控制默认**）。
- **拍板**：侧栏勾选 = 当前会话；设置窗 `showTaskLayer` = 进入页面时的默认是否勾选。

### D6：无 Flyway / 无新端口

- 无新表、无新环境变量、无 compose 变更。回滚 = 隐藏 UI + 停用 due-range（或保留 API 无害）。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 双请求竞态导致网格闪烁 | store 合并 loading；图层关闭时不请求 task |
| 大量截止日任务挤爆月视图 | 月视图折叠为「N 项」+ 当日展开；limit 上限写在 contract（如 200） |
| 用户误以为任务=会议 | 视觉区分（checkbox/任务色）+ 点击进任务页而非事件编辑器 |
| 与日历 V1 spec「MUST NOT 任务图层」冲突 | 本 change delta **MODIFIED** 该要求 |

## Migration Plan

1. 先 `-web`：Mock due-range + 侧栏/网格；更新 UI patterns 草案
2. `-api`：实现 due-range + preference 默认键
3. `-integrate`：去 Mock 联调；看板归档
4. 回滚：feature 关 UI；API 可留

## Open Questions

- （实现前可定）月视图过载时的折叠文案与 limit 精确值 — 默认 limit=200，超出截断并 toast/旁注「仅显示部分任务」。
- 是否允许在日历上「完成任务」快捷操作 — **本 change 不做**（仅深链）。
