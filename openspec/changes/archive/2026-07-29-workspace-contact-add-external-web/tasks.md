## 1. 契约与看板

- [x] 1.1 扩展 `openspec/lanes/workspace-contacts-enrich/contract.md`（search / POST request / reject）
- [x] 1.2 看板 `workspace-contact-add-external` → ui_ready
- [x] 1.3 更新 `workspace-ui-patterns` 通讯录添加说明

## 2. Store 与 API 类型

- [x] 2.1 `api/app/contacts.ts` 增加搜索/发起/拒绝类型与预留函数
- [x] 2.2 `stores/contactAdd.ts`：搜索、outgoing、与 external 去重
- [x] 2.3 `contacts.rejectRequest` 本地/预留 API

## 3. UI

- [x] 3.1 重写 `WorkspaceAddExternalContactModal` 接 contactAdd
- [x] 3.2 通讯录外部列表「添加」入口
- [x] 3.3 RequestsList 拒绝按钮 + 页面 wiring
- [x] 3.4 从 createHub 移除搜索种子（改 re-export 或删除）

## 4. 验证

- [x] 4.1 `pnpm build` + `pnpm typecheck`
- [x] 4.2 `openspec validate workspace-contact-add-external-web --strict`
- [x] 4.3 母 change 3.4 勾选；浏览器：Hub / 通讯录外部 / 新的联系人拒绝
