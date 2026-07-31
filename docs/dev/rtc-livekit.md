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

## 本地 Compose

```bash
docker compose -f deploy/compose.yml up -d livekit
```

默认密钥与 `application.yml` 中 `relayflow.rtc.livekit` 一致：`devkey` / `secret`。

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
