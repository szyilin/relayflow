## Context

`POST /app-api/task/list/archive` 已将清单标为 `archived=1` 且不删任务；`GET /list/mine` 过滤已归档。缺列出与恢复能力。

## Goals / Non-Goals

**Goals:**

- 成员可列出自己参与的已归档清单
- OWNER 可恢复（`archived=0`），清单重回侧栏
- UI：侧栏「已归档」折叠区 + 恢复；归档确认说明可恢复

**Non-Goals:**

- 永久删除清单 API
- 已归档清单内继续写任务（打开后只读提示「请先恢复」可选；V1 允许打开已归档清单只读查看，写操作仍走既有权限但建议 UI 引导恢复）
- 改表结构

## Decisions

1. **API**
   - `GET /app-api/task/list/archived` → 当前用户为成员且 `archived=1` 的清单（含 `myRole`）
   - `POST /app-api/task/list/unarchive` Body `{ id }` → 仅 OWNER；幂等（已未归档则 no-op success）
2. **UI**：侧栏「任务清单」下「已归档（n）」可折叠；行内 OWNER 显示「恢复」；归档按钮改确认弹窗
3. **前端优先**：contract 先扩；web store 接新端点；api 同步实现（本切片体量小，同 change 交付）

## Risks / Trade-offs

- 深链打开已归档 `?listId=`：`get` 仍可读（成员）；侧栏不在 mine 中 → 恢复后才出现在日常列表。可接受。

## Migration Plan

无 DB 迁移。部署新二进制 + 前端即可。
