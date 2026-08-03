## Why

预检阶段麦/摄预览正常，进入会中后连接耗时长、视频迟迟不出、底部麦摄按钮状态与真实设备不一致并来回闪烁。根因集中在客户端入会路径未对齐 LiveKit 推荐的预检→预热→发布流程，以及 UI 状态与 LiveKit 真值竞态；需要可导出的分段诊断数据，才能稳定复现与修复。

## What Changes

- 会中入会路径：`prepareConnection` 预热；预检本地 track 交接后 `publishTrack`（避免 stop 后再二次 `getUserMedia`）
- 工具栏与画面：麦/摄图标绑定 LiveKit 实际状态；publishing 期间禁用切换；有 track 时稳定显示 `<video>`，消除 `hasVideo` 抖动导致的闪烁
- Token 临近过期续期：防抖，避免进会后短时间全量 disconnect/reconnect 风暴
- 开发态 RTC 诊断时间线：分段耗时 + 关键事件环缓冲，支持 `?rtcDebug=1` 一键复制（不含完整 token）
- 文档补充预检→入会约定（`docs/dev/rtc-livekit.md`）

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `meeting`: 明确会前预检到会中媒体发布的稳定性要求（连接阶段可观测、控件与媒体状态一致、预检设备选择在会中生效且不因二次采集失败）

## Impact

- **前端**：`web/src/composables/rtc/*`、`MeetingPreJoinModal`、会中 `room.vue`、`MeetingParticipantTile`
- **后端 / API / Flyway**：无变更（凭证仍走既有 `/app-api/meeting/join-credentials`）
- **部署**：无 Compose 必改项；诊断仅客户端
- **回滚**：回退前端 composable/页面即可，无数据迁移
