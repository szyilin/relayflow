## 1. 契约与看板

- [x] 1.1 扩展 `openspec/lanes/org-invite/contract.md`（capability + 四渠道 + join 草案）
- [x] 1.2 看板登记 `org-invite-channels` web=in_progress → ui_ready
- [x] 1.3 更新 `workspace-ui-patterns`：加入页 / 四渠道说明

## 2. 草稿 Store

- [x] 2.1 `stores/orgInvite.ts`：token、code、expiresAt、mobileDrafts、reset、validate
- [x] 2.2 租户切换 reset；与 `inviteSettings.nonAdminExpireDays` 对齐展示

## 3. 工作台 UI

- [x] 3.1 增强 `WorkspaceOrgInviteModal` 接 orgInvite store
- [x] 3.2 页面 `/app/invite/join`：token/code 校验骨架
- [x] 3.3 确认 Create Hub `canInvite` 读取 inviteSettings

## 4. 验证

- [x] 4.1 `pnpm build` + `pnpm typecheck`
- [x] 4.2 `openspec validate org-invite-channels-web --strict`
- [x] 4.3 母 change 3.3 勾选；浏览器：`+` → 添加企业成员；`/app/invite/join`
