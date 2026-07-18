## Context

接 `workspace-task-view-model-v1` D1/D3；快速访问已交付。

## Goals / Non-Goals

**Goals:** 工具栏可演示；配置按上下文本地持久；contract 定 REST 形状。

**Non-Goals:** 真 API；字段分组看板列；个人自定义组 CRUD；清单共享服务端配置。

## Decisions

1. `USE_LOCAL_VIEW_CONFIG=true`：`localStorage` 键 `relayflow-task-view-config-v1:{tenantId}:{userId}`。
2. 个人 context 配置私有；LIST 本地也暂存当前用户键下（api 后改为共享默认）。
3. 工具栏改 `groupBy` 只落配置；分区渲染由后续 `group-by-field` 做。
4. `activity` 无工具栏。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 本地配置与他端不同步 | integrate 换 API |
| LIST 本地非共享 | contract 写明；api 修正 |

## Open Questions

（无）
