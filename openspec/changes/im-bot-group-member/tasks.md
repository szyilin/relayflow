# Tasks：im-bot-group-member（G1）

> 验证：`openspec validate im-bot-group-member --strict`；`./mvnw -pl relayflow-server -am compile`；含 web 时 `cd web && pnpm build && pnpm typecheck`  
> 依赖：`im-bot-notify-foundation`（schema G0）

## 1. 契约与后端 API

- [ ] 1.1 起草/更新 lane contract（扩展 `im-group-chat` 或新建 `im-group-bot`）：bots add/remove + members 含 `subjectType`
- [ ] 1.2 `ImGroupService`：addBot / removeBot；owner-only；系统 Bot 可达；幂等挂载
- [ ] 1.3 Controller：`POST /app-api/im/group/bots/add|remove`；扩展 members 列表返回 Bot 元数据
- [ ] 1.4 挂载成功写 `sender_type=system` 环境文案（可选移除文案）
- [ ] 1.5 单测或 curl：挂载/重复挂载/移除/非 owner 拒绝

## 2. 前端

- [ ] 2.1 `/app/messages` 群成员面板展示 Bot 行（名称/头像）
- [ ] 2.2 添加/移除 Bot 交互（最小可用：选系统 Bot）
- [ ] 2.3 `pnpm build` + `pnpm typecheck`；浏览器冒烟路径写入 tasks 注释

## 3. 收口

- [ ] 3.1 `openspec validate im-bot-group-member --strict`
- [ ] 3.2 更新 `docs/dev/api-integration-board.md`；勾选母 change foundation §7.4（若仍 open）
- [ ] 3.3 `./mvnw -pl relayflow-server -am compile` 通过
