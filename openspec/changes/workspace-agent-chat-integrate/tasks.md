## 1. API client

- [x] 1.1 新增 `sendAgentSessionMessageStream(sessionId, content, { onEvent, signal })`：`fetch` + JWT，解析 SSE `data:` JSON
- [x] 1.2 类型：stream 事件与 contract 对齐；复用 `normalizeMessage` 处理 `message.*` 载荷

## 2. Store

- [x] 2.1 `sendMessage` 改走 stream API；映射事件到 parts；用服务端 message 替换临时 id
- [x] 2.2 `stopStreaming` abort 真实请求
- [x] 2.3 删除 `USE_MOCK_AGENT_STREAM`、`mockAgentStream` 引用与文件

## 3. Verify & board

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`
- [x] 3.2 更新 `docs/dev/api-integration-board.md`：chat-ui-web / stream-api / integrate 状态
- [x] 3.3 浏览器路径说明（需后端 + 已启用模型）

### 浏览器验证路径

1. 后端已启动（含 Flyway `V0.1.0.58`）；管理端启用模型连接  
2. 登录 → `/app/agents` → 选智能体 → 发送  
3. 可见 SSE 流式正文（Markdown）；有 tool/reasoning 时服务端透出则 UI 展示  
4. 点停止可中断；历史重开可回放 `parts`（若有）
