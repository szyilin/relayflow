## 1. 前端（生命周期 / 预检 / 续期 / 日历绑定）

- [x] 1.1 会中页：路由离开与卸载可靠上报 `leave`（keepalive/beacon）；主持展示「结束会议」调 `endMeeting`；与「离开」区分
- [x] 1.2 `MeetingPreJoinModal`：麦/摄设备选择器；扬声器音量（或输出设备）实际作用于远端音频；deviceId 传入入会发布
- [x] 1.3 `useRtcRoom` / 会中页：token 临近过期静默刷新 credentials 并续连；join-credentials 响应透出过期时间（若后端补充字段则对齐）
- [x] 1.4 `CalendarEventEditor`：RELAYFLOW 预约走 schedule 返回的 `calendarEventId` 绑定，禁止同一次操作双建 `cal_event`

## 2. 后端（rtc + meeting）

- [x] 2.1 Flyway（约 `V0.1.0.71`）：清理重复 ACTIVE 后加部分唯一索引；`ensureSession` 冲突幂等返回
- [x] 2.2 `endSession`：Room Service 删房/踢人；失败仍标 `ENDED` + 日志；可选 `room-service-enabled` 开关
- [x] 2.3 webhook：`participant_count` 原子增减；文档/注释可联调路径；保持 Bearer 或升级签名验签（择一落地）
- [x] 2.4 `MeetingService.schedule` 调 `CalEventApi.createLinkedEvent` 并回写 `calendar_event_id`；摘要/响应带出该 id；失败不留下孤儿策略按 design D4

## 3. 文档与验证

- [x] 3.1 更新 `docs/dev/rtc-livekit.md` 与 `deploy/compose.yml` 注释：浏览器可达 URL、`node-ip` 区分、webhook 联调示例（含容器网络 caveat）
- [x] 3.2 验证：`openspec validate meeting-rtc-lifecycle-hardening --strict`；`./mvnw -pl relayflow-server -am compile`；`cd web && pnpm build && pnpm typecheck`；浏览器冒烟：入会 → 路由离开 leave → 主持结束踢房 → 预约仅一条日历
