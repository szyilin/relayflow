# 任务：群聊工作台 UI（im-group-chat-web）

> **Lane**：`*-web` 第一步。后端见 `im-group-chat-api`。

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 1.2 阅读 [`contract.md`](../../lanes/im-group-chat/contract.md) 与 `design.md`

## 2. 前端（web/）

- [x] 2.1 起草 `openspec/lanes/im-group-chat/contract.md`
- [x] 2.2 `api/app/im.ts`：建群、邀请、成员列表类型与 API
- [x] 2.3 `mocks/im.ts`：群会话、成员、system 消息 Mock
- [x] 2.4 `stores/im.ts`：群创建/邀请/成员；Mock 回退；system 消息辅助
- [x] 2.5 `ImCreateGroupModal` + `ImInviteMembersModal`
- [x] 2.6 `/app/messages`：建群入口、群列表项、系统消息样式、群成员侧栏
- [x] 2.7 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 3. 验证

- [x] 3.1 `cd web && pnpm build`
- [x] 3.2 `openspec validate im-group-chat-web --strict`

## 浏览器路径

1. `pnpm dev` → `/app/login` 登录
2. `/app/messages` →「建群」→ 输入群名、勾选成员 → 创建
3. 发送群消息；侧栏查看成员；「邀请成员」追加同事
4. 确认系统消息「XXX 加入了群聊」居中展示

## 下一 change

- `im-group-chat-api` — 按 contract 实现群聊 REST + 系统消息 + WS fanout
