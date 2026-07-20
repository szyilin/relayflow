## Context

按 `openspec/lanes/workspace-task-view-config/contract.md`。

## Goals / Non-Goals

**Goals:** get/save；默认配置；权限。

**Non-Goals:** 关前端 localStorage（integrate）；字段分组渲染。

## Decisions

1. 表 `task_view_config`：`config_json` JSONB；个人行 `owner_user_id`；LIST 共享行 `owner_user_id IS NULL` + `context_id=listId`。
2. 无行时返回服务端默认 JSON（对齐前端 default）。
3. `canMutateTasks`（OWNER/EDITOR）可保存 LIST 配置。

## Risks / Trade-offs

无重大风险。

## Open Questions

（无）
