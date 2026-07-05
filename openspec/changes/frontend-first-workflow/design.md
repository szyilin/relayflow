# 设计：前端优先

真源文档：[`docs/dev/frontend-first-workflow.md`](../../../docs/dev/frontend-first-workflow.md)

## 与旧流程差异

| 项 | 旧（后端先行） | 新（前端优先） |
|----|----------------|----------------|
| 第一步 | `-api` 冻结 contract | `-web` UI + Mock + contract 草案 |
| 看板 | 等 api archived 才 `-web` | `-web` 可先 `in_progress` |
| 契约作者 | 后端 lane | **前端 lane 起草**，后端实现前 review |
| 并行 | 双 Agent 并行 | 单人顺序：web → api → integrate |

## 仍保留

- `openspec/specs/` 行为真源
- Mock 仅在 store 内、`isApiUnavailable` 时
- integrate 阶段去 Mock
- `[平台]` 无 UI 切片仍后端单 change
