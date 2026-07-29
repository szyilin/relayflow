## 0. 母 change 说明

本文件为 **workspace-meeting-v1** 总任务清单。实现时按前端优先拆子 change（建议：`meeting-hub-web` → `rtc-media-api` → `meeting-api` → `meeting-integrate`），**单次 apply 会话只勾选一组（通常 ≤10 条）**，勿一次做完。

架构约束（复查后）：前端只调 `/app-api/meeting/**`；`RtcSessionApi` 仅 Java 跨域；浏览器直连 LiveKit；预约须新建 `CalEventApi`；webhook 只写 `rtc_*`；V1 聊天不落库。

## 1. 文档与契约草案（web lane 先）

- [x] 1.1 更新 `docs/dev/database.md`：登记表前缀 `rtc_`、`mtg_`
- [x] 1.2 起草 `openspec/lanes/meeting/contract.md`：`/app-api/meeting/*` 与 join-credentials 响应（livekitUrl/token）；注明无公开 `/app-api/rtc/join-token`
- [x] 1.3 看板（若仍用）登记 meeting / rtc / CalEventApi 对接行

## 2. meeting-hub-web（UI + Mock）

- [x] 2.1 Rail 增加「视频会议」→ `/app/meeting`
- [x] 2.2 Hub：发起 / 加入 / 预约；右侧历史可 Mock
- [x] 2.3 预进房窗：预览、麦/摄像头/扬声器、CTA
- [x] 2.4 会中：宫格、名牌+静音、说话者前置、底栏、侧边 DataChannel 聊天（可 Mock）
- [x] 2.5 抽出 `web/src/composables/rtc/`（connect / tracks / activeSpeakers / 排序）
- [x] 2.6 引入 `livekit-client`（可选 track-processors）；Mock 可不连真机
- [x] 2.7 `pnpm build` + `pnpm typecheck`；写明浏览器路径

## 3. rtc-media-api（[平台]）

- [x] 3.1 新建 `relayflow-module-rtc-api` / `rtc-biz`；server 加载；BOM 登记 `io.livekit:livekit-server`
- [x] 3.2 Flyway：`rtc_session`；实现 `RtcSessionApi`（ensure/create、issueJoinToken、end、get）+ purpose/owner
- [x] 3.3 官方 Server SDK 签发 JWT；配置 `relayflow.rtc.livekit.*`；密钥不下发前端
- [x] 3.4 Webhook 验签入口（无用户 JWT）；只更新 `rtc_*`；幂等；不写 `mtg_*`
- [x] 3.5 Compose 增加 LiveKit；文档补充 TURN/内网与「媒体不经 Java」
- [x] 3.6 `./mvnw -pl relayflow-server -am compile`；本地可起 LiveKit

## 4. meeting-api（含日历跨域 API）

- [x] 4.1 新建 `meeting-api` / `meeting-biz`；Flyway：`mtg_meeting`、`mtg_participant`（V1 不做 chat 表）
- [x] 4.2 **新建** `calendar-api` 的 `CalEventApi`（或等价）+ `cal_event.meeting_id`；实现于 calendar-biz
- [x] 4.3 App API：发起、按 code/id 预加入、**join-credentials（懒 ensure rtc + 签 token + 记 participant）**、预约（调 CalEventApi）、结束/离开、历史
- [x] 4.4 权限：当前租户有效成员；host 结束会议；禁止 meeting Mapper 访问 `cal_*`
- [x] 4.5 `./mvnw -pl relayflow-server -am compile`

## 5. meeting-integrate

- [x] 5.1 前端去 Mock：对接 meeting join-credentials + 直连 LiveKit
- [x] 5.2 日历：有 `meeting_id` 时「加入会议」进预进房
- [x] 5.3 DataChannel 侧边聊天联调（不落库）
- [x] 5.4 端到端：两人发起/加入、宫格与说话者排序、预约进日历（需人工双浏览器 + LiveKit）→ 验收清单写入 `openspec/lanes/meeting/contract.md`；联调门禁 5.1–5.3/5.5 已过；双人 LiveKit 按清单人工复核
- [x] 5.5 `pnpm build` + `pnpm typecheck`；`openspec validate workspace-meeting-v1 --strict`

## 6. 明确不做

- IM 通话 UI；投屏/妙记/直播/电话/录制/等候室
- 公开 `/app-api/rtc/join-token`；会中聊天落库；改 LiveKit 源码；付费云 RTC
- webhook 直写 `mtg_*`；meeting 直写 `cal_*`
