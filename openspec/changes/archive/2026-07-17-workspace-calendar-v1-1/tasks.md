# Tasks：workspace-calendar-v1-1（母 change · 执行路线图）

> **用法**：总路线图。编码按子切片分批；每会话通常只做一个子切片内一组 task。  
> **顺序**：`share` → `rrule` → `dnd`（dnd 单次事件可与 share 并行 web）。  
> **前序**：[`workspace-calendar-v1`](../archive/2026-07-17-workspace-calendar-v1/proposal.md) 已归档。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / `specs/calendar` delta / 本 `tasks.md`
- [x] 0.2 `openspec validate workspace-calendar-v1-1 --strict`
- [x] 0.3 看板登记 `workspace-calendar-share` / `-rrule` / `-dnd`（planned）
- [x] 0.4 拍板 Open Questions：BUSY_FREE 本刀不做；共享发 Bot 通知；月视图不可拖

---

## 1. workspace-calendar-share

**目标**：整日历共享 / 订阅同事；侧栏图层 + list 可见性。

- [x] 1.1 扩展 contract（Share 节）+ Flyway `cal_calendar_share`（或等价）
- [x] 1.2 `-web`：侧栏「共享给我的」；共享管理 UI（可选弹层）；临时数据仅 store
- [x] 1.3 `-api`：share CRUD；calendar list 含订阅；event list 含 READ 共享日历
- [x] 1.4 `-integrate`：无 Mock；store→API；看板 share → done

**验证**：compile + typecheck + build 通过；双人浏览器 E2E 建议本地再点一遍。

---

## 2. workspace-calendar-rrule

**目标**：重复日程 + 最小例外；list 窗口展开。

- [x] 2.1 扩展 contract（RRULE 节）+ `cal_event.rrule` / exception 表
- [x] 2.2 `-web`：编辑器重复区；实例点击编辑范围（THIS / ALL）
- [x] 2.3 `-api`：持久化规则；`list` 展开；例外写路径；提醒 dedupe 含 instance
- [x] 2.4 `-integrate`：API+UI 接通；看板 rrule → done

**验证**：`pnpm build` + compile + typecheck 通过。

---

## 3. workspace-calendar-dnd

**目标**：日/周拖拽改期/改时长。

- [x] 3.1 扩展 contract（DnD 节）
- [x] 3.2 `-web`：日/周 pointer 拖动 + 拉边；乐观更新；组织者门禁
- [x] 3.3 `-api`：确认 update/reschedule；重复实例默认 THIS（依赖 §2）
- [x] 3.4 `-integrate`：拖动走 reschedule；失败回滚；看板 dnd → done

**验证**：浏览器日/周拖动后刷新仍保持新时间（建议本地点验）。

---

## 4. 母 change 归档前

- [x] 4.1 子切片完成并（若拆目录）各自 archive — 未拆独立目录，跳过
- [x] 4.2 `openspec archive workspace-calendar-v1-1`
- [x] 4.3 确认 board / `workspace-ui-patterns` / contract 已更新

---

## 执行顺序速查

```text
Session 0   §0 规划基线 + 拍板 Open Questions
Session 1+  §1 share（web → api → integrate）
Session n   §2 rrule
Session m   §3 dnd（单次事件可提前并行 web）
Session z   §4 归档
```

## 会话开场白模板

```text
Using change: workspace-calendar-share（workspace-calendar-v1-1 子切片）
Read: openspec/changes/workspace-calendar-v1-1/design.md
Tasks: workspace-calendar-v1-1/tasks.md §1
```
