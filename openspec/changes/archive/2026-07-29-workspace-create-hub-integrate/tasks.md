## 1. 联调核对

- [x] 1.1 确认 `contactAdd` / `orgInvite` / `createHub.canInvite` / 建群 picker / 创建文档无草稿真源
- [x] 1.2 确认「我管理的群组」用 IM 聚合；看板注明专用 managed API 可选延后
- [x] 1.3 小修：仅当联调发现缺口时改 store/api（不重做 UI）
  - 本切片无额外代码缺口（contact-add-external-integrate 已去草稿）

## 2. 文档与看板

- [x] 2.1 更新 `openspec/lanes/workspace-create-hub/contract.md`：标注四项前端已接 / integrate
- [x] 2.2 更新 `docs/dev/api-integration-board.md`：`workspace-create-hub`、`workspace-member-picker`、母 change → done
- [x] 2.3 写明浏览器路径（tasks 验收用）
  - `/app` Hub、`/app/contacts`、`/app/messages`、`/app/docs`、`/app/invite/join`、`/admin/system/invite-settings`

## 3. 验证

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`（随 contact-add integrate 已通过；本切片无额外 web 改动）
- [x] 3.2 `openspec validate workspace-create-hub-integrate --strict`
