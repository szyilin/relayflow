## Context

母 change `workspace-plus-create-v1` design D1/D5/D6。现有：`WorkspaceRailHeader` 头像、`ImCreateGroupModal` 仅当前部门成员、`docs.createPage`、`auth.isAdmin`。

## Goals / Non-Goals

**Goals:**

- 「+」四项可点、可演示
- 成员选择器飞书式三源 UI（外部/群组可用临时数据）
- contract 草案 + 看板 `web → in_progress/ui_ready`

**Non-Goals:**

- 邀请 settings/四渠道真 API、外部联系人搜索真 API、managed-groups API
- 改 Java / Flyway

## Decisions

1. **入口**：`WorkspaceRailHeader` 头像行右侧放「+」`UDropdownMenu`（对齐飞书头像旁）。
2. **canInvite**：`useCreateHubStore` 临时 `canInvite`，默认 `auth.isAdmin`；后续接 capability API。
3. **建群**：新 `WorkspaceMemberPicker` + `WorkspaceCreateGroupModal`（或增强 ImCreateGroupModal）；消息页旧入口可继续用旧 modal 或共用新组件。
4. **邀成员 / 加外部**：独立 Modal 骨架；Tab/搜索 UI 齐，提交 toast「即将对接」或写 store 临时。
5. **建文档**：`router.push('/app/docs')` + `docs.createPage()`。

## Risks / Trade-offs

- [Risk] 选择器与 ImCreateGroupModal 双份 → 优先抽出共用 picker，消息页逐步切过去
- [Risk] canInvite 仅 isAdmin 与将来 ALL_MEMBERS 不一致 → store 单一开关，接 API 时替换

## Open Questions

- 无阻塞；邀请弹窗四 Tab 内容深度留给 `org-invite-channels-web`
