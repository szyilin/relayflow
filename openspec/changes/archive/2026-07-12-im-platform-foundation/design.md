# 设计：IM 平台基础架构（im-platform-foundation）

> **性质**：架构真源 / 史诗文档。本 change **不写业务代码**；实现按文末路线图拆为独立 change。

## Context

- **产品定位**：自托管企业协作平台，IM 为工作台（`/app/messages`）核心能力
- **现状**：
  - `relayflow-module-im`：Maven 骨架，无 DO / Service / Controller
  - `relayflow-spring-boot-starter-websocket`：占位，无 Session 管理
  - `web/src/pages/app/messages/index.vue`：壳层 UI，数据为空
  - `openspec/specs/im/spec.md`：单聊 / 群聊 / 频道 / 先持久化再 ACK，缺平台抽象与数据模型细节
- **约束**（bootstrap + architecture）：
  - V1 单体 `relayflow-server`；传输 **WebSocket**；多实例 **Redis Pub/Sub**
  - 禁止 V1：Kafka、自研 Netty 协议、Gateway/Nacos、音视频
  - 跨域仅 `*-api`；im 禁止直连 `sys_*` 表
  - 全链路 `tenant_id`；Redis key `t:{tenantId}:*`
  - 业务切片默认 **前端优先**：`-web` → `-api` → `-integrate`（`[平台]` 除外）

## Goals / Non-Goals

**Goals:**

- 确定 **WebSocket + JSON envelope** 为 V1 唯一客户端实时通道
- 定义 **四层分离** 架构，避免 IM 业务与传输层耦合
- 预留 **飞书式扩展面**：通知中心、Bot、系统消息、Presence、跨模块事件（BPM/任务）
- 定义 **数据模型**（Conversation-centric、seq、client_msg_id、Content Block）
- 定义 **`*-api` 契约接口**，V1 实现 IM 子集，其余 no-op 或 stub
- 给出 **可执行的纵向切片路线图**

**Non-Goals（本设计不要求 V1 实现）:**

- 通知中心表 / UI / 投递
- Bot 会话、交互卡片 callback
- SSE 第二通道、邮件 / 移动端 Push
- @提及、Thread、Reaction、全文搜索
- 音视频（WebRTC）
- Phase 2 独立 `im-server` / 分库（仅预留边界）

---

## Decisions

### D1：传输层 — 原生 WebSocket + JSON Envelope

| 选项 | 结论 |
|------|------|
| 原生 WebSocket + JSON | **V1 采用** |
| STOMP over WebSocket | 不采用；订阅语义可用 envelope `domain` + 前端路由替代 |
| SSE 通知专用通道 | V2 可选；V1 单连接 + `domain=notify` 占位 |
| Centrifugo 等独立网关 | Phase 2 连接数瓶颈时再评估 |

**配置**（沿用 bootstrap）：

```yaml
relayflow:
  websocket:
    enable: true
    path: /infra/ws
    sender-type: local   # 单机 local；多实例 redis
    heartbeat-interval: 30s
```

**握手**：`GET /infra/ws?token=<JWT>` → 校验 JWT → 解析 `user_id`、`tenant_id` → 注册 Session。

---

### D2：四层架构

```text
┌─────────────────────────────────────────────────────────────┐
│ L4 产品触达 Product                                          │
│   IM 会话页 (/app/messages)  |  通知中心 (V2)  |  Presence   │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│ L3 投递编排 DeliveryOrchestrator（im-biz / 将来 notify-biz）   │
│   事件 → 落库？→ 推 WS？→ 目标用户解析                        │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│ L2 域事件 RealtimeEvent                                      │
│   domain: im | notify | presence | system                    │
│   各模块通过 RealtimeEventPublisher 发布，禁止直调 WebSocket   │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│ L1 传输 RealtimeTransport（infra-biz + starter-websocket）     │
│   SessionRegistry、Redis fanout、envelope 投递                 │
│   不懂「请假审批」「单聊」业务语义                              │
└─────────────────────────────────────────────────────────────┘
```

**铁律**：`system-biz` / 将来 `bpm-biz` **不得** import WebSocket Session；只调 `RealtimeEventPublisher`。

---

### D3：WebSocket Envelope 规范

**统一外壳**（上下行共用）：

```json
{
  "domain": "im",
  "type": "message.send",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "ts": 1718169600000,
  "payload": { }
}
```

| 字段 | 说明 |
|------|------|
| `domain` | `im` \| `notify` \| `presence` \| `system` |
| `type` | 域内事件类型，见下表 |
| `requestId` | 客户端 UUID，ACK 关联 |
| `ts` | 毫秒时间戳 |
| `payload` | 域内载荷 |

**V1 实现的 type**：

| domain | 方向 | type | 说明 |
|--------|------|------|------|
| im | ↑ | `message.send` | 发消息 |
| im | ↑ | `read.report` | 上报已读 seq |
| im | ↓ | `message.ack` | 发送确认（DB 已提交后） |
| im | ↓ | `message.new` | 新消息推送 |
| im | ↓ | `conversation.update` | 会话摘要 / 未读变更 |
| system | ↑↓ | `ping` / `pong` | 心跳 |

**V1 占位（infra 路由存在，handler 可 no-op）**：

| domain | type | 将来用途 |
|--------|------|----------|
| notify | `inbox.item.new` | 通知中心新条目 |
| presence | `update` | 在线状态 |
| system | `event` | 组织变更等 |

**前端**：单 WS 连接 → 按 `domain` 分发至 `imStore` / `notifyStore` / `presenceStore`（V1 仅 im + ping/pong 有逻辑）。

---

### D4：发送路径（先持久化再 ACK）

```text
Client ──[im.message.send]──► ImWebSocketHandler
                                  │
                                  ▼
                         ImMessageService.send()
                           1. 校验会话成员（im_conversation_member）
                           2. 幂等检查 client_msg_id
                           3. INSERT im_message + 分配 seq（同事务）
                           4. UPDATE im_conversation 摘要
                           5. COMMIT
                           6. WS → 发送方 message.ack
                           7. RealtimeTransport → 其他在线成员 message.new
```

步骤 6 **必须**在步骤 5 事务提交之后。步骤 7 失败不影响 ACK（接收方 REST 增量补拉）。

**REST 降级**：`POST /app-api/im/message/send` — WS 不可用时客户端同逻辑调用。

---

### D5：数据模型

#### 核心表（V1 Flyway，`im-schema-v1` change 实现）

```text
im_conversation
  id, tenant_id, type (direct|group|channel)
  title, avatar_file_id
  last_msg_id, last_msg_at, last_msg_preview
  settings_json          -- V1 可 NULL

im_conversation_member
  id, tenant_id, conversation_id, user_id
  role (owner|admin|member|subscriber)
  read_seq, unread_count
  join_time, mute_until, pinned

im_message
  id, tenant_id, conversation_id
  sender_id, sender_type (user|system|bot|app)   -- V1 实现 user|system
  type (text|image|file|system)
  content_json         -- Content Block 数组
  client_msg_id        -- UNIQUE(tenant_id, client_msg_id)
  seq                  -- 会话内单调递增
  reply_to_msg_id      -- V1 可 NULL，预留
  create_time

im_group               -- type=group 时扩展
im_channel             -- type=channel 时扩展
```

**单聊 dedup**：创建 direct 会话时，将两 userId 规范化排序后查重，避免 A↔B 双会话。

**seq 生成**：事务内 `SELECT MAX(seq)+1 FOR UPDATE` 或 Redis `INCR t:{tenantId}:conv:{id}:seq` + 落库（V1 优先 DB 行锁，简单可靠）。

#### Content Block（`content_json`）

```json
{
  "version": 1,
  "blocks": [
    { "type": "text", "text": "你好" },
    { "type": "file", "fileId": "1234567890" }
  ]
}
```

V1 实现 `text`、`file`（引用 `infra_file.fileId`）。预留 `link`、`actions`（交互卡片）、`mention`。

#### 通知中心（V2，本设计仅留 API）

```text
notify_inbox_item        -- 不在 im-schema-v1 建表
  source_domain, source_event_type
  title, body, link_url, aggregate_key, read_status
```

---

### D6：`*-api` 契约接口（V1 预留）

#### infra-api — 传输层

```java
// 投递 envelope 给指定用户（tenant 从 TenantContext 或参数）
public interface RealtimeTransportApi {
    void sendToUsers(Long tenantId, Collection<Long> userIds, RealtimeEnvelope envelope);
    void sendToUser(Long tenantId, Long userId, RealtimeEnvelope envelope);
    boolean isUserOnline(Long tenantId, Long userId);
}

public record RealtimeEnvelope(
    String domain, String type, String requestId, Long ts, Object payload
) {}
```

#### infra-api — 事件发布（各模块入口）

```java
public interface RealtimeEventPublisher {
    void publish(RealtimeEvent event);
}

public record RealtimeEvent(
    String domain,           // im | notify | presence | system
    String type,
    Long tenantId,
    Collection<Long> targetUserIds,
    Object payload,
    DeliveryOptions options  // persist, wsPush, priority...
) {}
```

**V1 实现**：`RealtimeEventPublisherImpl` 在 infra-biz：
- `domain=im` → 委托 im 模块已注册的 handler（或 im-biz 直接调 TransportApi，二选一；**推荐 im-biz 发消息后直接调 TransportApi**，Publisher 主要给 **非 im 模块** 用）
- `domain=notify|presence` → **NoOpHandler**（日志 debug，不报错）

#### im-api — 业务 API（供 BPM 等跨域）

```java
public interface ImConversationApi {
    /** 获取或创建两人单聊 */
    Long getOrCreateDirectConversation(Long userIdA, Long userIdB);
}

public interface ImMessageApi {
    /** 系统/Bot 向会话发消息（不走 WS 上行） */
    Long sendSystemMessage(Long conversationId, MessageContent content, PublisherRef publisher);
}
```

#### notify-api — 通知中心（V2 占位）

```java
public interface NotifyInboxApi {
    void push(NotifyItemCommand command);
}
```

**V1**：接口 + `NotifyInboxApiImpl` 抛出 `UnsupportedOperationException` 或空实现 + 文档说明。

---

### D7：模块与包结构

#### starter-websocket（framework）

```text
com.relayflow.framework.websocket
  ├── config/WebSocketProperties, WebSocketAutoConfiguration
  ├── core/RealtimeEnvelope, WebSocketSessionRegistry
  ├── sender/LocalWebSocketMessageSender, RedisWebSocketMessageSender
  └── security/JwtWebSocketHandshakeInterceptor
```

#### infra-biz

```text
com.relayflow.module.infra
  ├── api/realtime/RealtimeTransportApiImpl, RealtimeEventPublisherImpl
  ├── websocket/WebSocketEndpoint, PingPongHandler
  └── websocket/router/DomainMessageRouter   # domain → handler 列表
```

#### im-biz

```text
com.relayflow.module.im
  ├── websocket/ImWebSocketHandler           # 处理 domain=im 上下行
  ├── service/message/ImMessageService
  ├── service/conversation/ImConversationService
  └── dal/...
```

**DomainMessageRouter 注册**：

| domain | Handler | V1 |
|--------|---------|-----|
| im | `ImWebSocketHandler` | ✅ |
| system | `PingPongHandler` | ✅ |
| notify | `NoOpNotifyHandler` | 占位 |
| presence | `NoOpPresenceHandler` | 占位 |

---

### D8：Redis 约定

| Key | 用途 | TTL |
|-----|------|-----|
| `t:{tenantId}:online:{userId}` | 在线状态 | 60s（心跳续期） |
| `t:{tenantId}:ws:fanout` | Pub/Sub channel | — |
| `t:{tenantId}:conv:{convId}:seq` | seq INCR（可选） | — |

---

### D9：多实例 fanout

```text
实例 A：ImMessageService 提交 DB
  → RealtimeTransportApi.sendToUsers(...)
  → RedisWebSocketMessageSender.publish(
        channel: t:1:ws:fanout,
        body: { targetUserIds, envelope, sourceInstanceId }
     )

各实例订阅 → 若 targetUser 在本机 SessionRegistry → write WS
```

`sender-type=local` 时跳过 Redis，直接本地 Registry 投递。

---

### D10：前端架构（后续 `-web` 切片引用）

```text
web/src/
  ├── api/app/im.ts
  ├── stores/im/
  │     conversation.ts
  │     message.ts
  │     websocket.ts      # 单连接，按 domain 分发
  └── composables/useImWebSocket.ts
```

**发送队列**：乐观 UI → `client_msg_id` → WS send → `message.ack` 替换 / 失败重试（同 id 幂等）。

**重连**：指数退避；重连后 `GET /app-api/im/message/list?afterSeq=` 补 gap。

---

### D11：安全与权限

| 场景 | 规则 |
|------|------|
| WS 握手 | 有效 JWT；写入 `tenant_id` |
| 发消息 | 必须是 `im_conversation_member` |
| 频道发帖 | `role` + `im_channel` 发帖权限 |
| 工作台 `/app` | 有效组织成员即可；**不用** `sys_permission` 限制聊天 |
| 管理端 `/admin` | 频道管理、审计导出等走 RBAC（后续切片） |
| 跨 tenant | WS / API 均拒绝 |

---

## 事件分类（飞书式扩展真源）

| 类别 | domain | 持久化 | V1 |
|------|--------|--------|-----|
| 单聊 / 群聊 / 频道消息 | im | im_message | ✅ 逐步实现 |
| 系统会话内消息（「XXX 加入群」） | im | im_message, sender_type=system | 群聊切片 |
| 输入中 Typing | presence | 否 | 占位 |
| 在线状态 | presence | Redis | 占位 |
| 审批 / 任务 / @ 提醒 | notify | notify_inbox_item | 接口占位 |
| Bot / 应用卡片 | im | im_message, content blocks | 预留 |
| 组织变更广播 | system | 可选 | 占位 |

**跨模块示例（将来 BPM）**：

```text
bpm-biz → RealtimeEventPublisher.publish(notify 事件)
       → DeliveryOrchestrator（V2）
       → notify_inbox + WS(notify.inbox.item.new)
       → （可选）ImMessageApi.sendSystemMessage 插入卡片
```

---

## 纵向切片路线图

```text
Phase 0  [平台] im-realtime-platform
           starter-websocket + RealtimeTransportApi + DomainRouter + tenant WS

Phase 1  [平台] im-schema-v1
           Flyway im_* 表 + codegen

Phase 2  im-direct-chat-web → im-direct-chat-api → im-direct-chat-integrate
           第一个用户可见闭环（两人单聊）

Phase 3  im-group-chat-web → -api → -integrate

Phase 4  im-channel-web → -api → -integrate

Phase 5  im-notify-inbox-*（V1.1+）
Phase 6  im-presence-*、im-rich-card-*（按需）
```

每个业务切片 `-web` lane 须起草 `openspec/lanes/{slice}/contract.md`（REST + WS envelope 字段）。

---

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 抽象过度、V1 交付慢 | 接口先 stub；Transport + 单聊优先闭环 |
| notify 与 im 混淆 | domain 强制分离；通知不落 im_message |
| 大群 fanout 性能 | V1 小群同步；V1.1 仅推在线成员 |
| seq 热点 | V1 PG 行锁；规模上来再 Redis INCR |
| WS 断线丢推送 | ACK 后 fanout；客户端 afterSeq 补拉 |
| 非 im 模块绕过 Publisher | Code Review + 禁止 infra WS handler 写业务 |

---

## Migration Plan

1. **本 change**：审阅 design → `openspec validate` → archive 合并 spec delta
2. **im-realtime-platform**：实现 WS，无 Flyway
3. **im-schema-v1**：Flyway；与业务切片解耦
4. **业务切片**：按 frontend-first 推进

**回滚**：各 change 独立；平台层回滚不影响已持久化消息（schema change 除外）。

---

## Open Questions

- 单聊创建：首次发消息懒创建 vs 主动「发起会话」API？（建议 **懒创建**，`-web` contract 定夺）
- seq：纯 DB vs Redis INCR？（建议 **V1 纯 DB**）
- `im-direct-chat` 是否含图片消息？（建议 **V1 仅 text**，file 跟 `infra-file` 下一小切片）

---

## 验证（本 change）

```bash
openspec validate im-platform-foundation --strict
```

实现阶段各切片见对应 change 的 tasks.md。
