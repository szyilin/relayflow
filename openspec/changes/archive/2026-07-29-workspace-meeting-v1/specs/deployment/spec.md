## MODIFIED Requirements

### Requirement: 本地基础设施 Compose 编排

`deploy/compose.yml` SHALL 定义 PostgreSQL、Redis、MinIO 服务，供本地开发与自部署使用。当启用视频会议 / RTC 能力时，Compose SHALL 另提供可自建的 **LiveKit** 服务定义（可用 profile 或文档说明为可选服务），并在文档中说明生产环境通常还需 TURN（如 coturn）与 UDP/TCP 端口规划。V1 MUST NOT 要求付费云 RTC。

#### Scenario: Compose 语法有效

- **WHEN** 开发者执行 `docker compose -f deploy/compose.yml config`
- **THEN** 配置解析成功且无语法错误
- **AND** 包含 postgres、redis、minio 三个服务定义

#### Scenario: LiveKit 服务可编排

- **WHEN** 开发者按文档启用 LiveKit 相关 Compose 服务（或默认已包含）
- **THEN** 配置中存在 LiveKit 服务定义且可与应用配置中的 URL/密钥对齐
- **AND** 不依赖外部 SaaS 媒体云即可在自托管环境联调

## ADDED Requirements

### Requirement: RTC 离线与内网说明

部署文档 SHALL 说明：在镜像预加载的前提下，LiveKit 与 RelayFlow 可在无公网环境下于内网互通；若客户端位于对称 NAT / 严格防火墙后，自托管 MUST 规划 TURN，否则可能无法建立媒体连接。

#### Scenario: 内网无 TURN 的限制可知

- **WHEN** 运维阅读部署/RTC 文档
- **THEN** 文档明确区分「信令可达」与「媒体经 TURN」的差异
- **AND** 不声称在任意 NAT 下零 TURN 一定可通话
