## 1. 组件

- [x] 1.1 新增 `WorkspaceOwnerChip.vue`（头像+名+可选「我」角标；`stopPropagation`；popover + `WorkspaceBusinessCard`）
- [x] 1.2 解析显示名/头像：props → auth → contacts → 回退；禁止单独「我」

## 2. 接线

- [x] 2.1 `DocsRecentList` / `DocsSharedList` / `DocsStarredList` 所有者改用芯片
- [x] 2.2 `/app/tasks` 负责人改用芯片（最多 3 + 溢出）

## 3. 文档与验证

- [x] 3.1 更新 `workspace-ui-patterns.md`（OwnerChip 约定）
- [x] 3.2 `cd web && pnpm build && pnpm typecheck`
- [x] 3.3 `openspec validate workspace-owner-chip-web --strict`
