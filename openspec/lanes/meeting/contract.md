# API 契约：meeting

> **状态**：ready / integrate done（看板 `workspace-meeting`）  
> **起草**：`workspace-meeting-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **母 change**：[`openspec/changes/archive/2026-07-29-workspace-meeting-v1`](../../changes/archive/2026-07-29-workspace-meeting-v1/proposal.md)

## 背景

工作台视频会议：Hub（发起 / 加入 / 预约）、预进房、多人会中、侧边 DataChannel 聊天。媒体面为自建 LiveKit；业务在 `meeting`，通用会话在 `rtc`（仅 Java `RtcSessionApi`）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 当前租户有效成员；**不用** `sys_permission` |
| LiveKit | 浏览器直连；凭证仅来自本契约 join-credentials；**禁止**前端持有 API Secret |
| **禁止** | 公开 `GET/POST /app-api/rtc/join-token`（V1） |

## 前缀

`/app-api/meeting`

## REST

### POST /instant

发起即时会议。

**Request**：

```json
{ "title": "可选标题" }
```

**Response `data`**：

```json
{
  "id": "190001",
  "meetingCode": "A1B2C3D4",
  "title": "即时会议",
  "status": "LIVE",
  "hostUserId": "1001"
}
```

### POST /schedule

预约会议（服务端经 CalEventApi 写日历）。

**Request**：

```json
{
  "title": "周会",
  "scheduledStart": "2026-07-29T10:00:00+08:00",
  "scheduledEnd": "2026-07-29T11:00:00+08:00"
}
```

**Response `data`**：同会议摘要，另含 `calendarEventId`、`status": "SCHEDULED"`。

### GET /get?id=

### GET /get-by-code?code=

**Response `data`**：会议摘要（含 `status`、`hostUserId`、`meetingCode`、时间窗、`calendarEventId`）。

### POST /join-credentials

懒创建 rtc session + 签发 LiveKit token + 记录参会人。

**Request**：

```json
{ "meetingId": "190001" }
```

或 `{ "meetingCode": "A1B2C3D4" }`。

**Response `data`**：

```json
{
  "meetingId": "190001",
  "meetingCode": "A1B2C3D4",
  "title": "周会",
  "livekitUrl": "ws://localhost:7880",
  "token": "<jwt>",
  "roomName": "mtg_190001",
  "identity": "1001"
}
```

### POST /leave

```json
{ "meetingId": "190001" }
```

### POST /end

主持人结束会议。

```json
{ "meetingId": "190001" }
```

### GET /history

我主持或参加过的会议列表（简史）。

**Response `data`**：`MeetingSummary[]`

## 跨域（非浏览器）

| API | 调用方 | 说明 |
|-----|--------|------|
| `RtcSessionApi` | meeting-biz（未来 im-biz） | ensure / issueJoinToken / end |
| `CalEventApi`（新建） | meeting-biz | 预约写 `cal_event.meeting_id` |

## 前端路径

| 路径 | 说明 |
|------|------|
| `/app/meeting` | Hub |
| `/app/meeting/:meetingId/room` | 全屏会中 |
| `web/src/composables/rtc/` | 可复用 LiveKit 连接 |

## Mock（-web）

Store 内临时数据撑 UI；join-credentials 可返回假 token（不连真 LiveKit）或本地 LiveKit；**integrate 已删除 Mock**。

## 人工 E2E 验收清单（双浏览器 + LiveKit）

前置：`docker compose -f deploy/compose.yml up -d`（含 LiveKit）；`relayflow-server` 与 `pnpm dev` 已起。

| # | 步骤 | 期望 |
|---|------|------|
| 1 | 用户 A：`/app/meeting` → 发起即时会议 → 预进房开麦/摄像头 → 进房 | 会中宫格见本地面 |
| 2 | 用户 B：同租户另一浏览器 → 用会议号加入 → 进房 | 双方互见对方画面；说话者前置排序随发言变化 |
| 3 | 侧边 DataChannel 聊天互发 | 对方实时可见；刷新后不要求落库 |
| 4 | 用户 A：Hub 预约会议 → 打开日历 | 事件含会议入口；「加入会议」进预进房 |
| 5 | Host 结束会议 | 双方退出；历史列表可见 |

> 联调门禁（去 Mock / 日历入口 / DataChannel / build·typecheck）已在 `workspace-meeting-v1` §5.1–5.3、5.5 完成；上表为人工双人验收项。
