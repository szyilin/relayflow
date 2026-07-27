## Context

母 change：`workspace-docs-share-star-v1`（design D7/D9/D10 已冻结）。本切片仅前端 + contract。

## Goals / Non-Goals

**Goals:** UI 可演示分享/星标/最近列与菜单；contract 可供 `-api` 实现。

**Non-Goals:** Java/Flyway；真实 Bot；去掉临时 Mock（`-integrate`）。

## Decisions

1. 临时数据仅 `stores/docs.ts`（及必要时同文件辅助结构）；禁止 `web/src/mocks/`
2. Library / Drive 继续走既有真实 API；分享/星标/visit 增强在 store 本地层，直到 integrate
3. 权限档位 UI：`VIEW` + `EDIT`（Q1）
4. 菜单严格按母 design D10

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 本地分享态与真实文档 id 混用 | seed 用明确前缀 `local-`；打开本地文档用 store 假 body |
| integrate 时字段漂移 | contract 为真源 |
