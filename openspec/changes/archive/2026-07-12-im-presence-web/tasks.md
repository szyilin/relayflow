# 任务：在线状态 UI（im-presence-web）

> **Lane**：`*-web` · 后端见 `im-presence-api`

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 1.2 阅读 [`contract.md`](../../lanes/im-presence/contract.md)

## 2. 前端（web/）

- [x] 2.1 起草 `openspec/lanes/im-presence/contract.md`
- [x] 2.2 `api/app/presence.ts`：`batchPresence(userIds)`
- [x] 2.3 `stores/presence.ts` + 30s 轮询
- [x] 2.4 `/app/messages` Aside：单聊对端在线状态
- [x] 2.5 `/app/contacts` Aside：成员在线点
- [x] 2.6 更新看板

## 3. 验证

- [x] 3.1 `cd web && pnpm build`
- [x] 3.2 `openspec validate im-presence-web --strict`

## 下一 change

- `im-presence-api`
