## 1. 诊断与入会路径

- [x] 1.1 新增 `RtcDiag`（timeline/timings/事件环缓冲/`exportSnapshot` 脱敏）；`useRtcRoom` 与会中页接入分段计时
- [x] 1.2 预检改用 LocalTrack 预览；确认入会时 handoff 不 stop；`connect` 前 `prepareConnection`，有 handoff 则 `publishTrack`，否则回退 `set*Enabled`
- [x] 1.3 会中页：`?rtcDebug=1` 或 DEV 下一键复制诊断；状态文案区分凭证/信令/开设备

## 2. UI 稳态与续期

- [x] 2.1 工具栏只读 LiveKit 真值；connecting/publishing 禁用麦摄；瓦片有 camera track 时避免 hasVideo 抖动闪头像
- [x] 2.2 Token 续期防抖（刷新中忽略重复；最小间隔）；避免进会后短时间全量重连风暴
- [x] 2.3 更新 `docs/dev/rtc-livekit.md` 预检→入会约定；`openspec validate meeting-rtc-join-stability --strict`；`cd web && pnpm build && pnpm typecheck`

## 3. Safari 发布挂起与控件响应（续）

- [x] 3.1 Room：`singlePeerConnection: false`，关闭 `red`/`backupCodec`；信令连上即 `ui.unlocked`，publish 后台进行
- [x] 3.2 麦摄切换：乐观 UI + 已发布轨道 `mute`/`unmute`；工具栏不再被 publishing/reconnecting 锁死；诊断记录 `toggleMic`/`toggleCam`
- [x] 3.3 瓦片麦克风旁实时 `audioLevel` 电平；静音时为 0 / mic-off
- [x] 3.4 `ensureMediaPublished` 限次 + 跳过 in-flight，避免重连风暴；`pnpm build` + `pnpm typecheck`
