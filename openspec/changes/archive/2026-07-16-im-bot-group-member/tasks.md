# Tasks：im-bot-group-member（G1）

> 验证：`openspec validate im-bot-group-member --strict`；`./mvnw -pl relayflow-server -am compile`；含 web 时 `cd web && pnpm build && pnpm typecheck`  
> 依赖：`im-bot-notify-foundation`（schema G0）

## 1. 契约与后端 API

- [x] 1.1 起草/更新 lane contract（扩展 `im-group-chat` 或新建 `im-group-bot`）：bots add/remove + members 含 `subjectType`
- [x] 1.2 `ImGroupService`：addBot / removeBot；owner-only；系统 Bot 可达；幂等挂载
- [x] 1.3 Controller：`POST /app-api/im/group/bots/add|remove`；扩展 members 列表返回 Bot 元数据
- [x] 1.4 挂载成功写 `sender_type=system` 环境文案（可选移除文案）
- [x] 1.5 单测或 curl：挂载/重复挂载/移除/非 owner 拒绝

## 2. 前端

- [x] 2.1 `/app/messages` 群成员面板展示 Bot 行（名称/头像）
- [x] 2.2 添加/移除 Bot 交互（最小可用：选系统 Bot）
- [x] 2.3 `pnpm build` + `pnpm typecheck`；浏览器冒烟：登录 → `/app/messages` 打开群 → 侧栏「添加机器人」（群主）→ 成员列表见 Bot → 移除

## 3. 收口

- [x] 3.1 `openspec validate im-bot-group-member --strict`
- [x] 3.2 更新 `docs/dev/api-integration-board.md`；母 change foundation §7.4 已在 archive 勾选
- [x] 3.3 `./mvnw -pl relayflow-server -am compile` 通过；`ImGroupServiceBotMembershipTest` 3/3
