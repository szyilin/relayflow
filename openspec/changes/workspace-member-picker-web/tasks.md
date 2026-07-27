## 1. 契约与看板

- [x] 1.1 扩展 create-hub / im contract：managed groups 草案
- [x] 1.2 看板 `workspace-member-picker` → ui_ready
- [x] 1.3 更新 workspace-ui-patterns 选人器说明

## 2. 数据层

- [x] 2.1 `stores/memberPicker.ts`：loadManagedGroups（IM 聚合）
- [x] 2.2 从 createHub 删除 managedGroupSeeds

## 3. UI

- [x] 3.1 `WorkspaceMemberPicker` 接 memberPicker + excludeUserIds
- [x] 3.2 `ImInviteMembersModal` 改用共享 picker
- [x] 3.3 CreateGroupModal 去掉 seed- 用户特殊文案（保留过滤 draft-ext 等）

## 4. 验证

- [x] 4.1 `pnpm build` + `pnpm typecheck`
- [x] 4.2 `openspec validate workspace-member-picker-web --strict`
- [x] 4.3 母 change 3.5 勾选
