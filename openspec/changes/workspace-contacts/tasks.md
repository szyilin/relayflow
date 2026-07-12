# 任务：工作台通讯录（workspace-contacts）

> **前置**：[`org-member-dept-default`](../org-member-dept-default/tasks.md)；建议 [`admin-user-by-dept`](../admin-user-by-dept/tasks.md)  
> **契约**：[`contract.md`](../../lanes/workspace-contacts/contract.md)

## Lane `-web`（workspace-contacts-web）

- [ ] 1.1 起草 `openspec/lanes/workspace-contacts/contract.md`
- [ ] 1.2 `api/app/contacts.ts`、`mocks/contacts.ts`、`stores/contacts.ts`
- [ ] 1.3 `/app/contacts`：部门树 + 成员列表 + 搜索 + 名片卡 UI
- [ ] 1.4 名片「消息」按钮：`-web` 阶段 Mock 跳转 `/app/messages`（带 query `peerUserId`）
- [ ] 1.5 `cd web && pnpm build`；看板 web → `ui_ready`

## Lane `-api`（workspace-contacts-api）

- [ ] 2.1 `controller/app`：`GET /app-api/system/dept/tree`
- [ ] 2.2 `GET /app-api/system/user/list-by-dept`（主部门过滤 + keyword）
- [ ] 2.3 Security：`/app-api/system/**` JWT + 成员身份；curl 见 contract
- [ ] 2.4 `./mvnw -pl relayflow-server -am compile`；看板 api → `ready`

## Lane `-integrate`（workspace-contacts-integrate）

- [ ] 3.1 `stores/contacts.ts` 去 Mock
- [ ] 3.2 `stores/im.ts`：`openDirectChat(peerUserId)`；messages 页消费 pending peer
- [ ] 3.3 端到端：通讯录 → 发消息 → 单聊会话出现
- [ ] 3.4 看板 `workspace-contacts` → **done**；`openspec validate workspace-contacts --strict`

## 不在本 change

- IM change archive（可单独批量 archive `im-direct-chat-*`）
