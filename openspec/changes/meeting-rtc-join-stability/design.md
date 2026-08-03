## Context

媒体面由浏览器直连自建 LiveKit；Java 只签发 join JWT。现状：预检用原生 `getUserMedia` 预览后 `stop()`，会中再 `Room.connect` + `setMicrophoneEnabled` / `setCameraEnabled`；未调用 `prepareConnection`；工具栏乐观写 `mic/camEnabled` 又被 `bump→syncDeviceFlags` 覆盖；`hasVideo` 随 mute/live 抖动导致画面 opacity 闪烁。

参考：[LiveKit JS connect](https://docs.livekit.io/home/client/connect/)、client-sdk-js README（`prepareConnection`、`createLocalTracks`、`publishTrack`）。

## Goals / Non-Goals

**Goals**

- 分段可观测：credentials / prepare / connect / publish / firstFrame
- 预检开摄时会中尽快出本地画面，工具栏状态稳定
- 对齐 LiveKit 推荐入会路径；续期不制造 reconnect 风暴

**Non-Goals**

- 不改 token API 形状、不引入 Gateway/TURN 产品化
- 不接 Sentry/OpenTelemetry 平台（本 change 仅客户端诊断包）
- 不做 Playwright CI 门禁（可后续加；本 change 以手工 diag + build/typecheck 为准）

## Decisions

### D1. 诊断总线 `RtcDiag`

- 内存环缓冲（事件 + timings）；`exportSnapshot()` 脱敏（token 仅长度/前缀）
- `?rtcDebug=1` 或 `import.meta.env.DEV` 时暴露复制入口；生产默认不打扰

### D2. 预检 track 交接

- 预检用 `createLocalTracks`（或等价 LocalTrack）；确认入会时 **不 stop**，经模块级 handoff 交给 `useRtcRoom`
- `connect` 后 `publishTrack`；无 handoff 时回退 `set*Enabled`（兼容深链直进会中）

### D3. `prepareConnection`

- 拿到 credentials 后、`connect` 前调用；失败不阻断 connect（记 diag）

### D4. UI 真源

- 工具栏只读 LiveKit `isMicrophoneEnabled` / `isCameraEnabled`
- `phase === 'publishing' | 'connecting'` 禁用麦摄
- 瓦片：存在 camera track 且用户未关摄则显示 video；短暂 mute 启动不盖头像闪烁

### D5. Token 续期

- `remaining <= 60` 仅调度一次刷新；刷新中忽略重复定时器
- 仍用 disconnect+reconnect（SDK 无更简静默换票时），但加最小间隔防抖（例如 30s）

## Risks / Trade-offs

- [预检→路由 handoff 泄漏未 publish 的 track] → 会中页 unmount / 失败路径强制 stop
- [publishTrack 与 setCameraEnabled 语义不一致] → 统一用 publication / `isCameraEnabled` 驱动 UI
- [diag 含隐私] → 不导出完整 JWT；仅 url host、roomName、identity

## Migration Plan

纯前端发布；回滚即还原 composable/页面。无 Flyway。

## Open Questions

- 无阻塞项；静默换票若后续 SDK 支持可再收紧 D5
