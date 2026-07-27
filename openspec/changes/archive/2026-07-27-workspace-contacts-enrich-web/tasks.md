# Tasks：workspace-contacts-enrich-web

## 1. Contract 与看板

- [x] 1.1 起草 `openspec/lanes/workspace-contacts-enrich/contract.md`（外部/申请/星标草案端点 + 组织内引用既有契约）
- [x] 1.2 更新 `docs/dev/api-integration-board.md`：切片 `web → in_progress`，完成后 `ui_ready`

## 2. Store 与工具

- [x] 2.1 扩展 `stores/contacts.ts`：分类态、组织面包屑导航、外部/申请/星标临时数据、首字母分组、租户 reset
- [x] 2.2 新增首字母排序/分组工具（拉丁 A–Z，其余 `#`；`zh-CN` 排序）

## 3. UI

- [x] 3.1 `/app/contacts` 左栏改为带图标的分类导航；主区按分类切换
- [x] 3.2 组织内主区：面包屑 + 子部门行 + 成员列表 + 名片/发消息（复用既有 API）
- [x] 3.3 外部 / 新的联系人 / 星标 / 我的群组面板（临时数据 + 同意/取消星标；群组优先 IM）
- [x] 3.4 更新 `docs/dev/workspace-ui-patterns.md` 通讯录章节

## 4. 验证

- [x] 4.1 `cd web && pnpm build && pnpm typecheck`
- [x] 4.2 `openspec validate workspace-contacts-enrich-web --strict`
