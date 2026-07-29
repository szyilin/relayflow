## 1. Shared layout

- [x] 1.1 新增 `WorkspaceModalBody.vue`（max-height + overflow + 可选 minHeight prop）
- [x] 1.2 在 `docs/dev/workspace-ui-patterns.md` 简短记录弹窗稳定布局约定

## 2. Apply to high-traffic modals

- [x] 2.1 改造 `WorkspaceCreateGroupModal` + 确保 MemberPicker 宿主限高
- [x] 2.2 改造 `ImInviteMembersModal`
- [x] 2.3 改造 `ImAddGroupBotModal`（loading/list/empty 同 min-h）
- [x] 2.4 改造 `ImCreateGroupModal`（若仍在用）

## 3. Verify

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`
