## Context

RelayFlow 已有工作台日历、IM、`/infra/ws` 文本实时通道，但**没有**音视频媒体面。业界协作产品普遍自建业务层 + 外接 SFU（本仓库选型：**自建 LiveKit Server**，不改其源码）。用户对标飞书：Hub（发起/加入/预约）+ 预进房窗 + 多人宫格会中 + 侧边聊天；并要求 RTC 对接可被后续 IM 1:1/群通话复用。

约束：模块化单体、跨域仅 `*-api`、表前缀分域、前端优先纵向切片、管理端路由 `/admin`、用户端 `/app/*`。

> **架构复查（2026-07-28）**：对照 `architecture.md` / `cross-domain-messaging.md` / 现有 calendar 代码后，已修正若干不合理点（见文末「架构复查结论」）；下文为修正后的目标设计。

## Goals / Non-Goals

**Goals:**

- 拆出 **`rtc` 媒体会话域**，封装 LiveKit（token / room / webhook），供 meeting 与未来 IM 复用
- 交付 **`meeting` 产品域** V1：Hub、预进房、多人会中、侧边聊天、预约与日历弱打通
- Compose 可起 LiveKit；前端用官方 `livekit-client`，特效走 TrackProcessor（可选）
- UI 流畅：预进房与会中布局对齐飞书简化态（宫格、名牌、静音标、说话者前置）

**Non-Goals:**

- 改 LiveKit / 自研 SFU；会议室投屏、妙记、直播、电话、云录制
- IM 内语音/视频通话产品 UI（仅 API `purpose` 预留）
- 美颜/AR（可选虚景非必达）；等候室、破冰房、PSTN
- calendar-biz 依赖 meeting-biz；meeting 写 `cal_*`；rtc 写 `mtg_*` / 解析会议业务
- 前端直调「裸」`/app-api/rtc/join-token`（V1 禁止，见 D2）
- V1 会中聊天落库（见 D5）

## Decisions

### D1. 双域拆分：`rtc`（平台支撑）+ `meeting`（产品）

| 域 | 定位 | 表前缀 | 谁依赖 |
|----|------|--------|--------|
| **rtc** | 平台支撑域（类似「媒体版 FileApi」）：LiveKit 适配、会话、token、webhook | `rtc_` | meeting-biz、**未来** im-biz |
| **meeting** | 官方产品域：Hub、会议生命周期、预约、会中产品行为 | `mtg_` | web 经 `/app-api/meeting` |

不放进 `infra`：媒体会话有独立生命周期与 webhook，避免与文件/WS 搅在一起；但**语义上是可被多产品复用的支撑能力**，不是「只有会议导航才存在的业务」。

```text
浏览器                    Java 单体                         LiveKit
───────                   ────────                         ───────
Hub/预进房/会中 UI
  │ REST /app-api/meeting/*
  ▼
meeting-biz ──CalendarApi──► calendar-api（须新建，见 D4）
  │ RtcSessionApi（仅 *-api）
  ▼
rtc-biz ──HTTPS API/JWT──► LiveKit（信令管理）
  │
  └── webhook ◄──────────── LiveKit 事件

音视频 RTP/WebRTC：浏览器 ◄──────────────────────────────► LiveKit
                   （不经 Java，Java 不在媒体面）
```

**备选（否决）**：仅 `meeting` 模块塞 LiveKit — IM 复用会绑死会议模型。  
**备选（否决）**：LiveKit 全塞 `infra` — webhook/会话模型过重，且与「文件/WS」职责不符。

### D2. 对外 API 边界（修正：禁止前端直打 rtc join）

| 入口 | 允许 | 说明 |
|------|------|------|
| `/app-api/meeting/**` | ✅ 前端 | 发起/加入/预约/join-credentials/结束/历史 |
| `RtcSessionApi`（Java） | ✅ 仅他域 biz | create / ensureSession / issueJoinToken / end / get |
| `/app-api/rtc/webhook`（或 `/infra/rtc/webhook`） | ✅ LiveKit 服务器 | **无用户 JWT**；共享密钥/签名校验 |
| `/app-api/rtc/join-token` | ❌ V1 禁止对前端开放 | 鉴权无法脱离产品域；IM/会议各自 Controller 聚合签发 |

**鉴权归属**：产品域（meeting / 未来 im）负责「此人能否进此会话」；`RtcSessionApi.issueJoinToken` **信任内部调用方**（与现有跨域 `*Api` 一致），不做会议成员语义校验。

**Java SDK**：`rtc-biz` 引入官方 `io.livekit:livekit-server`（版本进 BOM）；只用于签 JWT / Room API / webhook 验签。前端 `livekit-client` 负责进房推拉流。

### D3. 统一懒创建媒体会话（修正）

所有会议（即时 / 预约）均：

1. 先落 `mtg_meeting`（即时可直接 `LIVE` 或 `SCHEDULED→进房变 LIVE`）
2. **首次**请求 join-credentials 时：`ensureRtcSession(meetingId)` → `RtcSessionApi.create/ensure` → 回写 `mtg_meeting.rtc_session_id`
3. 再 `issueJoinToken` → 返回 `{ livekitUrl, token, roomName, meetingId, … }`

避免「创建会议即占 LiveKit 空房间」；即时与预约路径一致，实现更简单。

- `meeting_code`：人读加入码（实现期定 8～10 位字母数字 + 碰撞重试）
- V1 权限：当前租户**有效成员**可加入未结束会议（不做外链匿名入会）

### D4. 日历弱耦合 + **必须新建 CalendarApi**（修正）

现状：`calendar` 只有 `/app-api/calendar/**` Controller，**尚无**供他域调用的 `CalEventApi` / `CalendarApi`。  
若 meeting 直接调 Controller 或写 `cal_*` → **违规**。

因此本 change **必须**在 `calendar-api` 增加跨域 API，例如：

- `CalEventApi.createLinkedEvent(...)` / `updateMeetingLink(...)` / `cancelByMeetingId(...)`（命名以实现为准）
- DTO 含可选 `meetingId`；calendar-biz 只存 `cal_event.meeting_id`（Long），**不** import meeting 类型、**不**依赖 meeting-api/biz

依赖方向：

```text
meeting-biz → calendar-api   ✅
meeting-biz → rtc-api        ✅
calendar-biz → meeting-*     ❌
rtc-biz → meeting-*          ❌
im-biz → meeting-*           ❌（未来 im → rtc-api）
```

取消/结束会议：meeting **best-effort** 调 CalendarApi 取消或清 `meeting_id`；允许最终一致（日历残留链接时加入会议应返回已结束错误）。

### D5. 会中侧边聊天：V1 仅 DataChannel（收紧）

- 实时：LiveKit DataChannel（不新增 `/infra/ws` `domain=meeting`）
- **V1 不建** `mtg_chat_message`（刷新丢历史可接受）；需要持久化时另立 change
- 不做 IM 会话复用（避免会议聊天绑死 `im_*`）

### D6. 参会人真源在 meeting，不靠 webhook 写 mtg（修正）

| 数据 | 真源 | 说明 |
|------|------|------|
| 谁拿过凭证 / 业务参会 | `mtg_participant`（可选表）或等价 | 在 **join-credentials / leave / end** 同步路径写入 |
| LiveKit 房间存活、媒体侧进出 | `rtc_session` + webhook | rtc **只**更新 `rtc_*`；**禁止** webhook 里写 `mtg_*` |

若日后要用 webhook 驱动会议侧统计：rtc 在验签入库后发**领域消息**（如 `rtc.participant.joined`），meeting 异步消费——符合 `cross-domain-messaging.md`。**V1 可不实现该消费者**，以同步 join 路径为准。

### D7. 前端结构

| 路径 | 说明 |
|------|------|
| `/app/meeting` | Hub：发起 / 加入 / 预约；右侧简史 |
| 预进房 Modal/页 | 麦/摄像头/扬声器、本地预览、CTA |
| `/app/meeting/:meetingId/room` | 全屏会中：宫格 + 底栏 + 侧边聊天 |

- `livekit-client`；可选 `@livekit/track-processors`
- **必须**抽 `web/src/composables/rtc/`（connect / tracks / activeSpeakers / 布局排序），供日后 IM 通话复用
- 浏览器 **直连** LiveKit WSS（响应里的 url）；应用 API 与媒体平面分离
- 说话者前置：纯客户端排序

### D8. 部署与配置

- Compose：`livekit` 服务；文档写清生产 **TURN（coturn）**、UDP/TCP
- 配置：`relayflow.rtc.livekit.url` / `api-key` / `api-secret` / `webhook-secret`
- LiveKit 单节点 V1 可不强制 Redis；多节点再按官方文档加
- V1 不部署 Egress/Ingress
- BOM 登记 `livekit-server` 版本（新增依赖须按仓库约定确认）

### D9. 实现切片顺序

1. `meeting-hub-web` — UI + Mock（可先假 token）
2. `rtc-media-api` — 模块 + LiveKit + Compose + webhook（只写 rtc_*）
3. `meeting-api` — mtg + **新建 CalEventApi** + join-credentials 聚合
4. `meeting-integrate` — 去 Mock、日历「加入会议」

### D10. 数据模型（示意）

```text
rtc_session (
  id, tenant_id, purpose, owner_type, owner_id,
  room_name, status, create_time, ...
)

mtg_meeting (
  id, tenant_id, meeting_code, title, host_user_id,
  status, scheduled_start, scheduled_end,
  rtc_session_id NULL, calendar_event_id NULL, ...
)

mtg_participant (meeting_id, user_id, role, joined_at, left_at, ...)
  -- V1 由 meeting 在 join/leave 同步维护；非 webhook 直写

-- V1 不做 mtg_chat_message

cal_event.meeting_id NULL
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| NAT 连不上 | 生产必配 TURN；文档写明 |
| 前端 LiveKit 逻辑写死在 meeting 页 | `composables/rtc/` 约定 |
| 空房间占用 | 统一懒创建 session |
| DataChannel 刷新丢聊天 | V1 接受；持久化另立 change |
| `meeting_id` 残留在日历 | 结束会议 best-effort 清链接；加入已结束会议返回明确错误 |
| 缺 CalendarApi 导致违规耦合 | D4 强制新建 calendar-api 跨域接口 |
| 多人宫格性能 | 软策略（订阅/分页）；产品不设组织硬顶 |

## Migration Plan

1. Flyway：`rtc_*`、`mtg_*`、`cal_event.meeting_id`
2. Compose LiveKit + 环境变量
3. Rail 挂「视频会议」
4. 回滚：停 LiveKit + 隐藏入口；无 `meeting_id` 的日程行为不变

## Open Questions（已收敛默认）

| 项 | 默认 |
|----|------|
| 加入码 | 8～10 位字母数字 + 碰撞重试 |
| Hub 历史 | 我主持或参加过的 |
| 虚景 | 可选增强，非进房阻塞项 |
| webhook → meeting | V1 不做；仅 rtc 本地状态 |

---

## 架构复查结论

| # | 原设计问题 | 结论 / 修正 |
|---|------------|-------------|
| 1 | 提案写「前端也可调 `/app-api/rtc/join-token`」 | **不合理**：鉴权离开产品域。改为 **仅** `/app-api/meeting/**` 聚合签发；rtc 只对内 `RtcSessionApi` |
| 2 | 假定已有 `CalendarApi` | **现状没有**跨域日历 API。预约打通 **必须先** 在 `calendar-api` 增加创建/关联事件 API，禁止 meeting 写 `cal_*` |
| 3 | webhook「回写」mtg_participant | **跨域写违规风险**。参会真源改 join/leave 同步；webhook 只动 `rtc_*`；跨域用领域消息（V1 可暂缓消费者） |
| 4 | 即时开会即 createSession、预约懒创建 | 路径不一致。**统一懒创建** |
| 5 | 可选 `mtg_chat_message` | V1 范围膨胀。**收紧为 DataChannel only** |
| 6 | Java 是否在媒体面 | 明确：**不在**；浏览器直连 LiveKit，Java 只发 token / 管元数据 |
| 7 | rtc 放独立模块 vs infra | **保留独立 rtc 模块**合理；定位为平台支撑域，供 IM 复用 |
| 8 | meeting ↔ calendar 双向 | 保持 **meeting → calendar-api 单向**；calendar 只存 opaque `meeting_id` |

**总评**：双域拆分 + LiveKit 外置方向正确；主要不合理点在「对外 token 路径」「日历跨域 API 缺失」「webhook 写会议表」。按上表修正后，与仓库耦合铁律一致，可进入实现切片。
