# Tasks：workspace-docs-library-integrate（联调）

> **范围**：`web/` store 接 API；对应母 change [`workspace-docs-library-v1`](../workspace-docs-library-v1/tasks.md) §4。

---

## 4. workspace-docs-library-integrate

- [x] 4.1 store 去临时数据；MD 导出走 `exportDocumentMarkdown`
- [x] 4.2 树 CRUD / 打开 / 保存 / 错误码映射；`resetLocal` 清 API 缓存
- [x] 4.3 母 change §4 勾选；contract 状态 → integrate 完成
- [x] 4.4 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build && pnpm typecheck`
- [x] 4.5 看板 `workspace-docs-library` → **done**；`openspec validate` strict

**验证**：登录 → `/app/docs` E2E（新建、编辑、刷新、移动、删除、导出、版本冲突）。
