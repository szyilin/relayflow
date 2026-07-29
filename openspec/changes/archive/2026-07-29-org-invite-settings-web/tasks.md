## 1. 契约与看板

- [x] 1.1 起草 `openspec/lanes/org-invite/contract.md`（settings GET/PUT 章）
- [x] 1.2 更新对接看板 `org-invite-settings` web=in_progress → ui_ready
- [x] 1.3 更新 `docs/dev/admin-ui-patterns.md`：邀请设置页说明

## 2. Store 与 API 草案类型

- [x] 2.1 `api/admin/inviteSettings.ts` 类型 + 预留 get/put（`-web` store 可不调）
- [x] 2.2 `stores/inviteSettings.ts` 默认值 + load/save（内存）

## 3. 管理端页面

- [x] 3.1 `pages/admin/system/invite-settings/index.vue` 飞书式表单
- [x] 3.2 `useAdminNav` 增加「邀请设置」
- [x] 3.3 指定部门多选（部门列表）

## 4. 验证

- [x] 4.1 `cd web && pnpm build && pnpm typecheck`
- [x] 4.2 `openspec validate org-invite-settings-web --strict`
- [x] 4.3 母 change tasks 3.2 部分勾选（开 settings-web）；浏览器：`/admin/system/invite-settings`
