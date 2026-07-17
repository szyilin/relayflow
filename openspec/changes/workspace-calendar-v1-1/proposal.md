# 提案：工作台日历 V1.1（workspace-calendar-v1-1 · 母 change）

## Why

日历 V1（[archive](../archive/2026-07-17-workspace-calendar-v1/proposal.md)）已交付个人多日历、日/周/月、邀约与 Bot 提醒。对照飞书，协作与效率仍缺三块：**整日历共享/订阅同事**、**重复日程（RRULE）**、**拖拽改期**。本母 change 规划这三项，按纵向切片落地，默认前端优先。

## What Changes

本 change 为 **母 change（规划真源）**；实现按下列子切片分批编码（通常每会话一个子切片内一组 task）。

1. **整日历共享 / 订阅同事**：拥有者可将日历共享给租户内成员（读权限；可选 busy-free）；订阅方侧栏可见图层并参与区间查询
2. **重复日程（RRULE）**：创建/编辑支持重复规则；区间 list **展开实例**；支持「仅此 / 此及以后」例外（最小可用集）
3. **拖拽改期**：日/周视图拖动移动与拉边改时长；落库走既有或轻量 update；重复实例拖拽规则与 RRULE 切片对齐

## Capabilities

### New Capabilities

- （无新 capability 目录名；行为扩展既有 `calendar`）

### Modified Capabilities

- `calendar`：共享/订阅可见性；RRULE 与例外；网格拖拽改期行为

## Impact

| 层 | 变更 |
|----|------|
| DB | 可能新增 `cal_calendar_share`（或等价）；`cal_event` 增加 `rrule` / 例外表或列 |
| 后端 | `calendar-biz` list/get/create/update；共享 ACL；RRULE 展开 |
| 前端 | `/app/calendar` 侧栏订阅层、编辑器重复区、日/周拖拽 |
| 契约 | 扩展 [`workspace-calendar/contract.md`](../../lanes/workspace-calendar/contract.md) 或分子 lane |
| 看板 | 登记 `workspace-calendar-share` / `-rrule` / `-dnd` |

## 非目标（本母 change）

- 会议室 / 视频会议 / CalDAV / 第三方同步
- 公共/全员日历（可另立项）
- 侧栏「我的任务」虚拟图层
- 完整 CalDAV RRULE 全集（仅常用频率：每日/每周/每月 + 有限例外）

## 子 change 切片（建议顺序）

```text
workspace-calendar-v1-1                 ← 本 change（规划母版）
├── workspace-calendar-share            共享/订阅（schema → web → api → integrate）
├── workspace-calendar-rrule            重复日程与例外
└── workspace-calendar-dnd              拖拽改期（可与 share 并行开工 web；完整规则依赖 rrule）
```

**建议实施顺序**：`share` → `rrule` → `dnd`（dnd 单次事件可先做；重复实例改期在 rrule 后补齐）。
