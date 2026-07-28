# LiveKit 自建说明（RelayFlow RTC）

视频会议 / 未来 IM 通话的**媒体面**由自建 [LiveKit Server](https://github.com/livekit/livekit) 提供。业务会话元数据在 `rtc_*`；产品（会议）在 `mtg_*`。

## 职责边界

| 组件 | 职责 |
|------|------|
| 浏览器 `livekit-client` | 直连 LiveKit，推拉音视频（**不经 Java**） |
| `relayflow-module-rtc` | 签发 join JWT、webhook 更新 `rtc_*` |
| `relayflow-module-meeting` | Hub / 预约 / 鉴权后调 `RtcSessionApi` |
| LiveKit 容器 | SFU 转发 |

**禁止**：前端持有 API Key/Secret；公开 `/app-api/rtc/join-token`（V1）。

## 本地 Compose

```bash
docker compose -f deploy/compose.yml up -d livekit
```

默认密钥与 `application.yml` 中 `relayflow.rtc.livekit` 一致：`devkey` / `secret`。  
浏览器 URL：`ws://127.0.0.1:7880`（`RELAYFLOW_LIVEKIT_URL`）。

`--dev` 模式适合本机；`LIVEKIT_NODE_IP` 在局域网联调时可改为宿主机局域网 IP。

## 生产注意

1. **TURN（coturn）**：对称 NAT / 严格防火墙下几乎必需；勿声称「任意网络零 TURN 可通话」。
2. 公网部署需正确配置 LiveKit 的公网 IP、UDP/TCP 端口与 TLS（`wss://`）。
3. Webhook：LiveKit → `POST /app-api/rtc/webhook`，`Authorization: Bearer <webhook-secret>`（开发期可用与 api-secret 相同的共享密钥）。
4. 媒体流量大，与 API 节点带宽隔离更稳妥。

## 配置项

```yaml
relayflow:
  rtc:
    livekit:
      url: ws://127.0.0.1:7880
      api-key: devkey
      api-secret: secret
      webhook-secret: ""   # 空则回落 api-secret
      token-ttl-seconds: 7200
```
