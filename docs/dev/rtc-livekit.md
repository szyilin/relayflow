# LiveKit 自建说明（RelayFlow RTC）

视频会议 / 未来 IM 通话的**媒体面**由自建 [LiveKit Server](https://github.com/livekit/livekit) 提供。业务会话元数据在 `rtc_*`；产品（会议）在 `mtg_*`。

## 职责边界

| 组件 | 职责 |
|------|------|
| 浏览器 `livekit-client` | 直连 LiveKit，推拉音视频（**不经 Java**） |
| `relayflow-module-rtc` | 签发 join JWT、webhook 更新 `rtc_*`、结束时 Room Service 删房 |
| `relayflow-module-meeting` | Hub / 预约 / 鉴权后调 `RtcSessionApi` |
| LiveKit 容器 | SFU 转发 |

**禁止**：前端持有 API Key/Secret；公开 `/app-api/rtc/join-token`（V1）。

## 预检 → 入会（前端约定）

对齐 LiveKit JS 推荐路径（`createLocalTracks` / `prepareConnection` / `publishTrack`）：

1. **预检**（`MeetingPreJoinModal`）：用 `createLocalTracks` 做本地预览；确认入会时把仍存活的 LocalTrack **handoff** 给会中页，**不要**先 `stop()` 再在会中二次 `getUserMedia`（深链直进会中无 handoff 时回退 `setMicrophoneEnabled` / `setCameraEnabled`）。
2. **凭证**：`POST /app-api/meeting/join-credentials`。
3. **预热**：`room.prepareConnection(url, token)`（失败不阻断，仅记诊断）。
4. **连接**：`room.connect` → 发布 handoff tracks 或设备 API。
5. **控件真源**：会中麦/摄图标以**用户意图（乐观状态）**为准；信令连上后即解锁工具栏。切换优先对已发布轨道 `mute` / `unmute`（避免反复 `set*Enabled` 重采集）。SFU `publishTrack` 在后台进行，不阻塞进会。

### Safari / Docker Desktop 注意

- JS SDK 默认 `singlePeerConnection: true`；Safari 上常伴随 `_data_track` datachannel 告警，ICE/发布卡约 **15s** 后全量重连并 `LocalTrackUnpublished`。入会 Room 显式 `singlePeerConnection: false`，并关闭 `red` / `backupCodec`。
- Compose 必须用 `deploy/livekit.yaml` 的 `rtc.udp_port: 7882`（见下）。
- 诊断里若 `publish*Ms ≈ 15000` 且紧跟 `Reconnecting` → 优先查 ICE/单 PC；若已有 `ui.unlocked` 而工具栏仍灰，查 `joining` / `toolbarLocked`。

### 诊断（给 AI / 排障）

会中页在开发环境或 `?rtcDebug=1` 时显示「诊断」按钮，复制 JSON 快照（分段耗时 + 事件环缓冲；**不含完整 JWT**）。控制台 DEV 下也会打 `[rtc-diag]` 行。关键 timing：`credentialsMs`、`prepareMs`、`connectMs`、`publishTotalMs` / `publishVideoMs`、`firstFrameMs`。

排障时对照：

| 现象 | 诊断里看什么 |
|------|----------------|
| 「连接中」很久 | `connectMs`；是否迟迟没有 `ui.unlocked` |
| 进会后画面/对方迟迟不可见 | `publish*Ms`、`publishTrack.failed`、期间是否有 `Reconnecting` |
| 画面闪一下消失 | `LocalTrackPublished` 后紧跟 `LocalTrackUnpublished` / `Disconnected`；是否有 `leave.report`；`ensureMediaPublished` 是否反复触发 |
| 麦摄按钮慢 / 卡住 | `toggleMic` / `toggleCam`（含 `.ok` / `.failed` / `.blocked`）；`ui.toolbarLocked`；是否误用长时间 `set*Enabled` |
| 瓦片麦图标不对 | `deviceFlags.livekit` 与 `uiMic`/`uiCam` 是否长期不一致；本地瓦片以 UI 意图为准 |
| 误结束会议 | 后端 leave 在 join 后 20s 内不会因「最后一人离开」自动 `DeleteRoom`；前端不再在 `onUnmounted` 上报 leave（改用 `onBeforeRouteLeave` / pagehide） |

## 本地 Compose

```bash
docker compose -f deploy/compose.yml up -d livekit
```

默认密钥与 `application.yml` 中 `relayflow.rtc.livekit` 一致：`devkey` / `secret`。

**Docker Desktop（尤其 macOS）**：`deploy/livekit.yaml` 必须用 `rtc.udp_port: 7882`（单端口复用），并与 Compose 映射的 UDP 端口一致。若只用默认端口段而不映射，ICE 会卡约 15s，`publishTrack` 等到引擎超时，表现为「正在开启设备…」后画面闪一下消失。改配置后请 `docker compose -f deploy/compose.yml up -d --force-recreate livekit`。

### 浏览器可达 URL（重要）

配置项 `relayflow.rtc.livekit.url` / 环境变量 `RELAYFLOW_LIVEKIT_URL` 是**参会浏览器**用来连信令的 WebSocket 地址，会经 `join-credentials` 下发给客户端。

| 场景 | 建议 |
|------|------|
| 浏览器与 LiveKit 同机 | `ws://127.0.0.1:7880`（默认）可用 |
| 手机 / 另一台电脑访问前端 | 必须改成该设备能访问的地址，例如 `ws://192.168.x.x:7880` |
| 仅改 `LIVEKIT_NODE_IP` | **不够**：那是 ICE/媒体广告地址，不等于浏览器 WebSocket URL |

`LIVEKIT_NODE_IP`（Compose `--node-ip`）控制媒体侧候选地址；与 `RELAYFLOW_LIVEKIT_URL` 职责不同，二者在多机联调时通常都要设为宿主机局域网 IP（或对客户端可达的主机名）。

可选 Room Service HTTP 基址：`RELAYFLOW_LIVEKIT_API_URL`（默认由 `url` 的 `ws→http` / `wss→https` 推导），用于会议结束时 `DeleteRoom`。可用 `RELAYFLOW_LIVEKIT_ROOM_SERVICE_ENABLED=false` 紧急关闭。

### Webhook 联调

应用端点：`POST /app-api/rtc/webhook`，请求头 `Authorization: Bearer <webhook-secret>`（空则回落 `api-secret`）。只更新 `rtc_*`（如 `participant_count`），**不**写 `mtg_*`。产品侧进出以 meeting join/leave 为准；webhook 用于媒体侧遥测。

默认 Compose 使用 LiveKit `--dev`，**不会**自动打 webhook，需额外配置。容器内 LiveKit 访问宿主机上的 Java 时，常用：

- macOS/Windows Docker：`http://host.docker.internal:8080/app-api/rtc/webhook`（端口以实际 API 为准）
- Linux：宿主机局域网 IP，或 `extra_hosts: ["host.docker.internal:host-gateway"]`

勿声称「默认 Compose 一定已接通 webhook」。

## 生产注意

1. **TURN（coturn）**：对称 NAT / 严格防火墙下几乎必需；勿声称「任意网络零 TURN 可通话」。本仓库不强制默认拉起 coturn，运维须按网络规划。
2. 公网部署需正确配置 LiveKit 的公网 IP、UDP/TCP 端口与 TLS（`wss://`）。
3. Webhook：LiveKit → `POST /app-api/rtc/webhook`，`Authorization: Bearer <webhook-secret>`。
4. 媒体流量大，与 API 节点带宽隔离更稳妥。

## 配置项

```yaml
relayflow:
  rtc:
    livekit:
      url: ws://127.0.0.1:7880          # 浏览器可达
      api-url: ""                       # 空则从 url 推导 http(s)
      api-key: devkey
      api-secret: secret
      webhook-secret: ""                # 空则回落 api-secret
      token-ttl-seconds: 7200
      room-service-enabled: true
```
