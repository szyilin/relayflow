## Why

工作台视频会议（LiveKit）已可联调，但生命周期与运维闭环仍有硬缺口：结束会议不关媒体房、SPA 路由离开不报 leave、预约未走日历跨域 API、webhook/参会计数形同虚设。若不先收口，长会与多端联调会持续出现「业务已结束但媒体仍在」「会议永远 LIVE」等状态漂移。

## What Changes

基于代码审查结论，**本 change 只修应改项**；明确搁置项见文末。

### 纳入（应改）

- **媒体会话收口**：`RtcSessionApi.endSession`（及 meeting end 路径）MUST 通过 LiveKit Room Service 关闭房间 / 踢出参与者，再（或同时）将 `rtc_session` 标为 `ENDED`；禁止仅改库。
- **可靠 leave**：会中页路由离开、挂断、标签关闭均须上报 meeting `leave`（含 `sendBeacon` / 等价 best-effort）；修复「媒体已断但 `left_at` 为空 → 会议永不自动结束」。
- **主持人结束**：会中 UI 为主持人提供「结束会议」（调已有 `POST /meeting/end`），与「离开」区分。
- **预约对齐规格**：`MeetingService.schedule` MUST 经 `CalEventApi` 创建关联日历事件并回写 `calendar_event_id`（允许前端日历编辑器继续编排，但 API-only schedule 不得再产生孤儿预约）。
- **Webhook / 计数**：开发 Compose 或文档给出可启用的 LiveKit → `/app-api/rtc/webhook` 配置；webhook 处理幂等；`participant_count` 用原子更新；鉴权保持共享密钥或升级为 LiveKit 签名验签（择一写清）。
- **ensureSession 并发**：同一 owner 的 ACTIVE 会话不得并发插入多行（DB 约束或等价幂等 upsert）。
- **会前预检补齐**：麦克风/摄像头设备选择器；扬声器音量（或输出设备）实际作用于远端回放，不再是死控件。
- **入会凭证时效**：长会场景下客户端在 token 临近过期前可重新拉取 credentials 并无感续连，或文档化可配置更长 TTL + 明确断线行为（实现择优，行为须可测）。
- **部署说明强化**：`RELAYFLOW_LIVEKIT_URL` MUST 对**浏览器客户端**可达（禁止默认把仅容器内/127.0.0.1 地址当成多机联调真值而不提示）；TURN 仍以文档 + 可选 Compose 说明为主，本 change **不强制**默认拉起 coturn。

### 明确不改（本 change 搁置）

| 项 | 理由 |
|----|------|
| 等候室 / 邀请名单 ACL | V1 产品决策「同租户成员凭会议号可入」；另立项 |
| 屏幕共享 / Hub 投屏 | 原母 change 非目标 |
| IM 1:1 / 群通话 | `RtcPurpose` 预留即可；另切片 |
| 管理端会议产品设置 | 「即将开放」占位保留 |
| 会中聊天落库 | V1 DataChannel-only 不变 |
| adaptiveStream / dynacast / 分辨率策略调优 | 运维调参，非正确性缺口 |
| 默认 Compose 必带 coturn | 资源重；文档已要求生产规划 TURN |

无 **BREAKING** API 字段删除；会中 UI 增加主持人结束控件；schedule 行为从「仅 mtg」变为「mtg + 日历」——对只调 schedule API 的客户端是行为增强。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `meeting`：可靠 leave、主持人结束 UI、schedule→日历、会前设备选择与扬声器生效、token 续期/续连行为
- `rtc-media`：endSession 关闭 LiveKit 房、ensureSession 幂等、webhook 验签/幂等与原子计数
- `deployment`：LiveKit URL 对浏览器可达说明；webhook 联调配置；TURN 仍为文档要求（可补可选 profile 说明，不强制默认启 coturn）

## Impact

- **Java**：`relayflow-module-rtc-biz`（Room Service、webhook、ensure 约束）、`relayflow-module-meeting-biz`（schedule→`CalEventApi`、end 路径）、可能 Flyway（唯一索引）
- **web/**：`room.vue` leave/end、`MeetingPreJoinModal` 设备选择、`useRtcRoom` 续连（若做）
- **deploy/** + `docs/dev/rtc-livekit.md`：webhook 示例、URL/TURN 说明
- **回滚**：Flyway 唯一索引可保留；Room Service 调用失败应有降级日志但仍标 ENDED（design 定细节）；无密钥类迁移
