# 设计：workspace-docs-library-api

## Context

- Schema：`doc_object`、`doc_library_node`（`docs-schema-v1`）
- Contract：[`openspec/lanes/workspace-docs-library/contract.md`](../../lanes/workspace-docs-library/contract.md)
- 鉴权：JWT + 有效成员；数据范围仅 `owner_user_id`

## Decisions

### D1：Controller 拆分

- `DocLibraryController` — `/library/*`
- `DocDocumentController` — `/documents/*`
- `DocRecentController` — `/recent`

### D2：body 存储

- DB JSONB ↔ DO `String` + `JsonbTypeHandler`
- API 出入参 `body` 为 JSON 对象（`Map` / `JsonNode`）；Jackson 序列化

### D3：树移动防环

- 加载用户全部节点建 `parent → children` 索引；移动时若新 parent 在子树内则 `DOC_PARENT_INVALID`

### D4：删除

- 软删节点 + 关联 object；BFS 收集后代节点，逐个软删 node 与 object

### D5：Markdown 导出

- `TipTapToMarkdown` 服务端 walker，规则对齐 `web/.../tipTapToMarkdown.ts`

## 非目标

- 前端 integrate、`doc_embed`、DOCX/PDF、分享
