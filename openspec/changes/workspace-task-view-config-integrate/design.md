## Context

联调切片。

## Goals / Non-Goals

**Goals:** 真 API；`pnpm build` + `typecheck`。

**Non-Goals:** 字段分组看板列。

## Decisions

1. `setActiveContext` 异步拉取；`patchActiveConfig`/`reset` 异步保存。
2. 保存失败保留内存配置并 console/忽略（工具栏可后续 toast）。
3. 启动时清除旧 `relayflow-task-view-config-v1:*` localStorage。

## Risks / Trade-offs

无。
