## Context

`workspace-meeting-v1` 已归档：浏览器直连 LiveKit，meeting 发 credentials，rtc 签 JWT。审查发现生命周期与规格对齐缺口（见 `proposal.md`）。本设计只覆盖**应改**项，不重做媒体架构。

约束：跨域仅 `*-api`；媒体不经 Java；webhook 只写 `rtc_*`；Flyway 下一序号约 `V0.1.0.71`；包名 `com.relayflow`；禁止在规格/文案中点名第三方商业产品品牌（实现可继续用 LiveKit 技术名，文档沿用 `docs/dev/rtc-livekit.md`）。

## Goals / Non-Goals

**Goals:**

1. 业务结束 ↔ 媒体房关闭一致
2. leave / 自动 end 与真实在房状态一致
3. schedule API 满足既有「经 CalEventApi 建日历」规格，且不与日历编辑器双建事件
4. webhook 可联调；计数原子；ensureSession 幂等
5. 会前设备选择与扬声器控件生效；长会 token 可续

**Non-Goals:**

- 等候室 / 邀请 ACL、投屏、IM 通话、会中聊天落库、默认 Compose 必启 coturn、码率自适应策略大改

## Decisions

### D1. endSession 调用 LiveKit Room Service

- **选择**：`rtc-biz` 用已有 BOM 依赖 `io.livekit:livekit-server` 的 `RoomServiceClient`（或等价）：按 `room_name` **DeleteRoom**（或 ListParticipants + RemoveParticipant 全踢后删房）。先尽力调媒体侧，再将 `rtc_session.status=ENDED`。
- **备选**：仅改库 — 否决（现状问题）。
- **失败策略**：Room API 失败记 warn，**仍**标 ENDED（避免业务卡死）；客户端断线后无法靠凭证再入已 ENDED 的 meeting。
- **配置**：LiveKit HTTP API 基址可与 `relayflow.rtc.livekit.url` 同源推导（`ws`→`http`），或新增可选 `api-url`；密钥复用 `api-key`/`api-secret`。

### D2. 可靠 leave：路由离开也上报

- **选择**：会中页在 `onUnmounted` **以及** `pagehide`/`beforeunload` 上报 leave；用 `leftReported` + 单调 `joinGen` 防重。卸载路径用 `fetch(..., { keepalive: true })` / `sendBeacon` 兼容关页；正常挂断仍走现有 `leaveMeeting`。
- **HMR**：开发态可能误 leave — 可接受；或仅在 `import.meta.env.PROD` 的 unmounted 上报（实现时二选一，默认 **一律上报**，开发注意即可）。
- **备选**：只靠 pagehide — 否决（SPA 路由不触发）。

### D3. 主持人「结束会议」

- **选择**：`room.vue` 若当前用户为 host（credentials 响应或 getMeeting 的 `hostUserId`），展示「结束会议」→ `endMeeting` → disconnect → 回 Hub。非主持人不展示。API 已存在，无需新 endpoint。

### D4. schedule → CalEventApi，避免双日历

- **选择**：`MeetingService.schedule` 在插入 `mtg_meeting` 后调用 `CalEventApi.createLinkedEvent`，回写 `calendar_event_id`；失败则整笔事务回滚（或补偿删 mtg — **优先同事务外：日历失败则抛错且不提交 meeting**，用 `@Transactional` + 日历在事务内调用若日历自管事务则需明确：日历 API 若独立提交，则失败时删 mtg 补偿）。
- **前端**：`CalendarEventEditor` 在 `videoMeeting=RELAYFLOW` 且尚无 `meetingId` 时：调用 schedule 后若响应带 `calendarEventId`，**加载/绑定该事件**，禁止再 `createEvent` 一条带同一 `meetingId` 的新事件。Hub「预约」仍可进日历创建流。
- **备选**：schedule 加 `createCalendar=false` — 否决（规格要求 schedule 必建日历）。

### D5. Webhook 联调与鉴权

- **选择**：保留 `Authorization: Bearer <webhook-secret>`（与现网一致）；文档 + compose 注释给出 LiveKit 配置示例（`--dev` 可用独立 yaml / 环境说明）。**不强制**本 change 改成 JWT 签名验签（可列为 follow-up）；若实现成本低可用 SDK `WebhookReceiver` 升级。
- **计数**：`UPDATE rtc_session SET participant_count = GREATEST(0, participant_count + :delta) WHERE room_name=:r AND status='ACTIVE'`。
- **Compose**：默认 `--dev` 可不自动打 webhook（本机 Java 地址因容器网络难配）；文档写清 `host.docker.internal` / 宿主机 IP 示例。可选 profile `rtc-webhook` 仅文档级即可。

### D6. ensureSession 幂等

- **选择**：Flyway 部分唯一索引：`UNIQUE (tenant_id, owner_type, owner_id, purpose) WHERE deleted=0 AND status='ACTIVE'`（PostgreSQL）。插入冲突时再查 ACTIVE 返回。清理历史重复 ACTIVE（若有）在迁移中保留最新一条。

### D7. 会前设备与扬声器

- **选择**：`MeetingPreJoinModal` 用 `navigator.mediaDevices.enumerateDevices` + 选择器；确认时把 `deviceId` 经 query/store 传给 `useRtcRoom.connect` / `setMicrophoneEnabled`/`setCameraEnabled` 的 track options。扬声器：对远端 `<audio>` 设 `volume`（0–1），或 `setSinkId`（若浏览器支持）；控件必须改变可感知输出。

### D8. Token 续期

- **选择**：客户端在 `expiresAt` 前（若响应无该字段则按配置默认 TTL 的 80%）静默再调 `join-credentials`，用新 token `room.prepareConnection` / disconnect+reconnect **或** LiveKit 推荐的 token refresh 路径（以当前 `livekit-client` API 为准）。后端已可重复 issueJoinToken；meeting 侧重复 join 应 upsert participant、不重复刷坏状态。
- **备选**：仅把默认 TTL 提到 24h — 作补充配置，不能替代续期（规格：相对日历日仍短期）。

### D9. 浏览器可达 URL

- **选择**：强化 `docs/dev/rtc-livekit.md` + `application.yml` 注释：`RELAYFLOW_LIVEKIT_URL` = **浏览器** WebSocket 地址；多机联调示例；与 `LIVEKIT_NODE_IP` 区分职责。deployment 规格 ADDED 场景可测「文档可定位」。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Room Service 不可达导致「库已 ENDED、房仍在」短暂窗口 | 仍标 ENDED；凭证拒发；定时/下次 end 重试可选（V1 不做定时） |
| unmounted leave 与 HMR 竞态 | joinGen；开发文档提示 |
| schedule 建日历与编辑器双建 | D4 前端改绑已有 `calendarEventId` |
| 容器内 webhook 打不到宿主机 Java | 文档示例，不假装默认已通 |
| setSinkId 浏览器差异 | 音量必做；sink 能力探测后降级 |

## Migration Plan

1. Flyway `V0.1.0.71__rtc_session_active_owner_unique.sql`（去重 + 部分唯一索引）
2. 发布 rtc/meeting/web/docs；无需数据回填日历（仅新 schedule）
3. 回滚：保留索引无害；Room Service 调用可配置开关 `relayflow.rtc.livekit.room-service-enabled`（默认 true）以便紧急关闭

## Open Questions

- （实现时确认）当前 `livekit-client` 版本是否提供官方 token refresh 而不必整房重连；选文档推荐路径即可。
- Hub「预约」是否改为先 schedule 再跳日历编辑已有事件 — 倾向是，tasks 写明。
