## Why

工作台缺少飞书式「视频会议」产品面：无法发起/加入/预约多人会议，也无法在进房前预检音视频设备。日历规格已明确 V1 不做视频会议预订 UI，但预约会议天然应与日历弱打通。更关键的是：LiveKit 媒体能力后续还要支撑 IM 群语音/视频与 1:1 通话，若把流媒体对接写死在「会议板块」内，后续必然耦合爆炸。需要在第一版就拆出可复用的 RTC 会话层，并交付精简可用的会议 Hub + 会中体验。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。第一版对标飞书视频会议 **精简子集**（用户附图一 Hub + 附图二预进房窗）。

### A. 可复用 RTC 媒体会话层（平台能力 · 重点）

- 新增 **`rtc` 域**：对接自建 **LiveKit Server**（不改 LiveKit 源码）
- 对内暴露 `RtcSessionApi`（仅他域 biz）；对前端 **不**开放裸 join-token；webhook 独立验签入口
- 会话带 `purpose` / `owner_type`（`MEETING`；预留 `IM_DIRECT` / `IM_GROUP`），禁止会议业务字段进入 RTC 核心模型
- Compose / 配置增加 LiveKit（及 TURN 说明）；Java 只签 token / 管元数据，**不**在媒体面；`rtc-biz` 用官方 Java/Kotlin Server SDK

### B. 视频会议产品面 V1（工作台）

对齐飞书 Hub 精简三项 + 会中基础体验：

1. **发起会议** — 即时开会；预进房窗（附图二）：麦克风/摄像头开关与设备选择、扬声器音量、本地预览、虚景入口可占位；确认后进房
2. **加入会议** — 会议号 / 链接进入；同样经预进房窗
3. **预约会议** — 创建预约会议并通过新建的 **CalEventApi**（calendar-api）写入日历事件；日历只存可空 `meeting_id`，**calendar-biz 不依赖 meeting**
4. **多人会中** — 宫格展示参会者小窗；左下角显示显示名 + 静音标识；说话中参与者排序前置；底部基础控制（麦/摄像头/挂断等）
5. **会中侧边聊天** — 简单文本聊天（LiveKit DataChannel；V1 不落库、不做文件/表情）

非本期（对齐飞书 Hub 其余入口，明确不做）：会议室投屏、妙记/智能纪要、直播、电话、录制回放、等候室、美颜特效（虚景可用官方 TrackProcessor 作可选增强，非必达）、IM 内 1:1/群通话 UI（仅预留 RTC API）、会中聊天持久化。

### C. 工作台导航与路由

- Rail / 主导航增加「视频会议」入口 → `/app/meeting`（或等价）
- 会中可为独立全屏路由（如 `/app/meeting/:id/room`），保证布局流畅

## Capabilities

### New Capabilities

- `rtc-media`：LiveKit 对接、通用媒体会话、join token、webhook、配置与部署约定；供 meeting 及后续 IM 通话复用
- `meeting`：会议实体与生命周期、发起/加入/预约、参会人、会中侧边聊天、工作台 Hub 与预进房/会中 UI 行为

### Modified Capabilities

- `calendar`：解除与预约会议冲突的「禁止 video-meeting booking UI」表述；新增跨域 **CalEventApi**（现状无此 API，必须补）；`cal_event.meeting_id` 可空；日历 UI「加入会议」只调 meeting API
- `deployment`：Compose / 运维增加自建 LiveKit（及 TURN 说明）

> 会中聊天 V1 = LiveKit DataChannel；**不**改 `infra` WS domain。架构细则与复查结论见 `design.md`。

## Impact

- **领域 / Maven**：新增 `relayflow-module-rtc`、`relayflow-module-meeting`；**扩展** `calendar-api`（新建 CalEventApi）；server 加载 + Flyway；禁止 `meeting-biz→calendar-biz`、`rtc-biz→meeting-*`、`im-biz→meeting-*`（IM 未来只依赖 `rtc-api`）
- **前端**：`/app/meeting` Hub、预进房、会中；`livekit-client`；共享 `composables/rtc/`；浏览器**直连** LiveKit（不经 Java 代理媒体）
- **API**：前端只走 `/app-api/meeting/*`（含 join-credentials）；`RtcSessionApi` 仅 Java 跨域；webhook 独立验签入口；**禁止** V1 对前端开放裸 `/app-api/rtc/join-token`；禁止前端持有 LiveKit Secret
- **Java 依赖**：`rtc-biz` 使用官方 `io.livekit:livekit-server`（版本进 BOM）
- **DB**：`rtc_*`、`mtg_*`；`cal_event.meeting_id`；更新 `docs/dev/database.md`
- **部署**：Compose LiveKit（+ TURN 说明）；`relayflow.rtc.livekit.*`
- **回滚**：停 LiveKit + 隐藏入口；无 `meeting_id` 日程行为不变
- **不做**：改 LiveKit 源码、自研 SFU、会议室/PSTN/录制/妙记、IM 通话 UI、会中聊天落库
