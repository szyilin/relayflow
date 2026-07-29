## 1. 契约与看板

- [x] 1.1 起草 `openspec/lanes/workspace-create-hub/contract.md`
- [x] 1.2 更新 `docs/dev/api-integration-board.md`：`workspace-create-hub` web=in_progress
- [x] 1.3 更新 `docs/dev/workspace-ui-patterns.md`：Create Hub「+」入口说明

## 2. Create Hub 菜单

- [x] 2.1 `useCreateHubStore`（canInvite 默认 isAdmin；弹窗开关）
- [x] 2.2 `WorkspaceCreateHubMenu` + 挂到 `WorkspaceRailHeader` 头像旁
- [x] 2.3 四项动作：建群 / 邀成员 / 加外部 / 建文档

## 3. 成员选择器与建群

- [x] 3.1 `WorkspaceMemberPicker`（组织内钻取、外部、我管理的群组；已选区）
- [x] 3.2 `WorkspaceCreateGroupModal` 使用 picker；接既有 `im.createGroup`
- [x] 3.3 消息页创建群聊改为共用新 modal（或等价）

## 4. 邀成员 / 加外部骨架

- [x] 4.1 `WorkspaceOrgInviteModal`：四 Tab 骨架（QR/链接/码/手机号）
- [x] 4.2 `WorkspaceAddExternalContactModal`：精确手机/邮箱搜索 UI + 临时结果

## 5. 验证

- [x] 5.1 `cd web && pnpm build && pnpm typecheck`
- [x] 5.2 看板 web → ui_ready；`openspec validate workspace-create-hub-web --strict`
- [x] 5.3 母 change tasks 3.1 勾选；浏览器路径：任意 `/app/*` → Rail「+」
