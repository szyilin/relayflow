## 1. Contract & board（web）

- [x] 1.1 起草 `openspec/lanes/im-typing-indicator/contract.md`（WS 上下行、TTL、鉴权、curl/冒烟说明）
- [x] 1.2 更新 `docs/dev/api-integration-board.md` 登记本切片

## 2. Frontend（web）

- [x] 2.1 扩展 `useImWebSocket`：处理 `domain=presence` + 导出发送 envelope（typing）
- [x] 2.2 新增 typing store（按 conversationId → userIds）；本地 TTL 清理
- [x] 2.3 消息页 composer 防抖上行 start/stop；发送/清空/失焦 stop
- [x] 2.4 `headerSubtitle` 展示正在输入（私聊/群聊）；API 未就绪时可用本地 Mock 开关
- [x] 2.5 `cd web && pnpm build && pnpm typecheck`

## 3. Backend（api）

- [x] 3.1 实现 Presence 域 typing handler（替换/扩展 NoOp）：membership 校验
- [x] 3.2 Redis TTL 写入/续期/删除；fanout `typing.updated`
- [x] 3.3 `./mvnw -pl relayflow-server -am compile`

## 4. Integrate

- [x] 4.1 去掉前端 Mock；联调双端（或单端自测 + 日志）
- [x] 4.2 看板 → done；浏览器路径说明

### 浏览器验证路径

1. 后端 + Redis 已启动；两账号登录  
2. `/app/messages` 打开同一私聊：A 输入 → B 标题「对方正在输入…」；停止/发送后消失  
3. 群聊：可见「昵称正在输入…」（最多 3 人）  
4. `bot_dm` 不上报 typing
