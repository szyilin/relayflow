## Context

接 `workspace-task-mine-groups-web`；契约见 `openspec/lanes/workspace-task-mine-groups/contract.md`。

## Goals / Non-Goals

**Goals:** 表 + ensure 默认组；CRUD；move；删组回默认；创建根任务写入默认组归属。

**Non-Goals:** 前端去 Mock；跨用户可见性 E2E（integrate）。

## Decisions

1. `is_default` 用 `SMALLINT` 0/1（与 infra 一致）。
2. `GET /list` 返回 `groups` + `memberships`（否则前端无法分区）。
3. 组归属按 `(tenant_id, user_id, task_id)` 唯一；仅当前 user 的行。
4. move 用 `TaskAccessService.requireAccessible`；不要求必须是 assignee（可读即可挂到我的个人组——但语义上仅 MINE 有意义；若非我负责的任务仍允许挂组，integrate 前保持简单：accessible 即可）。
5. DO/Mapper 按现有 assignee 手写对齐（无必须跑 codegen 若表简单）；可选 codegen 参照。

## Risks

无 memberships 字段会阻断 integrate → 已在 list 响应补齐。
