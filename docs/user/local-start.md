# 完整本地启动

在仓库根目录执行 `./scripts/start-local.sh`。脚本定位仓库，不要求固定当前目录；首次生成配置、准备镜像、从源码构建，随后等待所有服务健康并检查页面、业务接口和 RTC 信令。成功后访问 `http://localhost:18080/app/login`，体验账号 `19988888888 / 123456`。

## 环境与入口

- Linux / macOS：Bash 3.2+、Docker daemon、Compose 2.24+；Windows 使用 WSL2 里的 Bash 和仓库。宿主机无需 Java、Node、pnpm、Python 或 OpenSSL。
- 建议给 Docker 分配至少 4 CPU / 8 GiB 内存并预留约 15 GiB 磁盘；这是资源建议，首次构建时间随机器与网络变化。
- 使用同机 `localhost` 浏览器；摄像头 / 麦克风需要浏览器许可。本地栈发布回环端口，不支持直接拿手机、局域网地址或公网域名访问。TURN / 远程部署另行规划。
- `deploy/compose.local.yml` 是完整体验编排。`deploy/compose.yml` + 宿主机 Java / Vite 仍为开发入口，两者项目卷独立；本地栈不接管旧开发容器或数据。

```bash
./scripts/start-local.sh
./scripts/local-status.sh
./scripts/local-status.sh --logs
./scripts/stop-local.sh
./scripts/start-local.sh --no-build
```

普通停止会移除本项目容器 / 网络，保留 env、密钥、PostgreSQL、Redis、MinIO 和日志卷。再次启动恢复这些卷；重新构建应用镜像也保留数据。脚本没有清空数据选项。

## 配置、密钥与端口

非敏感模板为 `deploy/local.env.example`。首次由固定工具容器生成随机凭证，原子写入权限 0600 的 `.relayflow/local.env`。已有文件仅校验与复用，不覆盖、不自动补齐或旋转密钥；文件损坏或旧卷存在而配置丢失时拒绝启动。应恢复原配置备份，避免为旧数据生成新密钥。配置和所有数据卷需作为一套备份；本入口不提供生产备份调度。

| 发布端口 | 配置键 | 用途 |
|---|---|---|
| 18080/TCP | `LOCAL_WEB_PORT` | 前端、REST、WebSocket、SSE 同源入口 |
| 19000/TCP | `MINIO_API_PORT` | 浏览器原始签名 URL 直传 / 下载 |
| 7880/TCP | `LIVEKIT_PORT` | RTC 信令 |
| 7881/TCP | `LIVEKIT_RTC_PORT` | RTC TCP 媒体 |
| 7882/UDP | `LIVEKIT_UDP_PORT` | RTC UDP 媒体 |

全部绑定 `127.0.0.1`。PostgreSQL / Redis / 后端 / XXL 管理端 / MinIO Console 仅在项目网络内。端口冲突会在启动前报错；本项目已发布的端口允许再次使用。改端口时先停止，再编辑本地 env 中的数字后启动，MinIO CORS 与 RTC 媒体端口同步更新。不要改动已有数据对应的数据库名、项目名或密钥。

高级调用可在首次生成时指定项目和配置位置：

```bash
COMPOSE_PROJECT_NAME=relayflow-demo \
RELAYFLOW_LOCAL_ENV_FILE=.relayflow/demo/local.env ./scripts/local-config.sh
# 按需编辑上述文件的端口；后续所有操作固定使用同一文件：
RELAYFLOW_LOCAL_ENV_FILE=.relayflow/demo/local.env ./scripts/start-local.sh
RELAYFLOW_LOCAL_ENV_FILE=.relayflow/demo/local.env ./scripts/stop-local.sh
```

仓库内自定义文件必须位于被忽略的 `.relayflow/`，也可使用仓库外绝对路径。同一项目名绑定一个配置位置；多项目并存需分别修改发布端口。已生成 env 中的值优先于外部同名环境变量，避免误用 shell 中另一套密码或端口。自定义配置的启动、状态和停止必须始终指定 `RELAYFLOW_LOCAL_ENV_FILE`。

默认单租户；首次启动前可在持久文件中设置现有 `RELAYFLOW_TENANT_*` 开关。不要用切换模式来迁移已有租户数据。

## 首次联网、失败和离线复用

首次需要访问 Docker Registry、Maven Central、Maven Wrapper 下载源、npm / pnpm 仓库和 Go 官方模块代理 / 校验和服务。脚本显示 pull / build / startup 阶段，不把编译下载时间计入服务就绪超时。服务就绪默认最多 900 秒，可在本地 env 调整 `RELAYFLOW_LOCAL_START_TIMEOUT_SECONDS`。

网络暂时失败时直接重试，配置与已有数据保留。本机网络需要自定义 Maven settings 时，使用构建 secret：

```bash
./scripts/start-local.sh --maven-settings /absolute/path/settings.xml
# 也支持 RELAYFLOW_MAVEN_SETTINGS_FILE=/absolute/path/settings.xml
```

settings 只在构建阶段挂载，不进入运行镜像；项目不会修改 Docker daemon、宿主机代理或全局镜像源。仓库已有 `deploy/maven/settings.xml` 可按网络情况自选，默认使用官方仓库。本次 macOS 验收遇到 Maven Central TLS 握手失败，选择该 settings 后通过；不保证所有网络首次下载均成功。

`--no-build` 要求本平台已有 server、web、MinIO、mc 四份本地构建镜像、其余运行时镜像和 Alpine 配置 / 端口工具镜像，缺少会明确报错；它禁止拉取和构建。构建缓存不是运行镜像的替代品。预先联网启动通过后，核心本地栈可复用这些镜像；HTML 仍引用外部字体，字体不可达时使用回退字体，因此不承诺浏览器零外联或离线安装包。恢复备份仍需匹配的 env 和数据卷。

失败返回非零，输出阶段、服务状态及有限脱敏日志；日志保存在 `.relayflow/logs/<project>.log`。用 `local-status.sh --logs` 重新收集。脚本隐藏已知凭证、JWT、签名查询参数和常见 JSON 密钥；手工执行 `docker inspect` / `compose config` 可能显示原始配置，分享前自行核对。已有锁时报错表示同项目有启动 / 停止在执行；仅确认原进程退出后，才处理 `.relayflow/operation-locks/<project>.lock` 遗留目录。

## 初始化与固定镜像

MinIO 初始化只确保 `relayflow` 桶和两个同机允许源，不修改匿名访问策略或已有对象；当前社区版使用服务级动态 CORS，详见 `deploy/minio/README.md`。XXL 的首次 schema / 官方 seeds / 完成标记在同一事务，日常启动跳过 seeds；手动 Cron、启停、账号和执行器设置保留。自动注册的执行器地址会由 XXL 正常刷新，与手动地址设置不同。调度 token 来自持久文件；Admin 按 PostgreSQL 模式启动，自动建表 / 自动 SQL 初始化关闭。

| 组件 | 本次固定版本 |
|---|---|
| PostgreSQL / Redis | 16.13 / 7.4.9 |
| MinIO / mc | RELEASE.2025-04-22T22-12-26Z / RELEASE.2025-08-13T08-35-41Z |
| XXL-JOB Admin | znle 3.3.2 |
| LiveKit Server | 1.13.11 |
| Node / Nginx | 24.22.1 / 1.30.6 |
| Java 构建 / 运行 | Temurin 21.0.11 / 21.0.12.1 |
| 配置工具 | Alpine 3.23.4 |
| MinIO / mc 构建工具 | Go 1.24.13 |

实际不可变摘要以 `deploy/compose.local.yml`、各 Dockerfile 和 `scripts/lib/local-compose.sh` 为准。LiveKit 本地栈升级后已验证与现有 JS 2.21.0 客户端的双端媒体；原开发 Compose 版本保持不变。[官方发布记录](https://github.com/livekit/livekit/releases/tag/v1.13.11)。基础镜像索引包含 Linux amd64 / arm64，当前实际构建与浏览器验证为 macOS + Docker Desktop Linux arm64；amd64 冷启动由 CI 配置覆盖，远程 CI 结果以运行记录为准。

本次远程冷拉取核对发现 MinIO / mc 旧 Docker Hub 摘要不可用，官方 Quay tag 也返回未授权，官方二进制下载返回 410。local 通过 `deploy/minio/Dockerfile` 从相同版本的官方源码提交构建两份本地镜像，使用 Go 不可变模块版本、官方校验和数据库及固定 Go / Alpine 镜像，保留许可证与源码链接；不依赖宿主机缓存里的旧镜像，也不引入来源不明的替代二进制。首次多出源码依赖下载和编译时间。原开发入口的镜像拉取问题仍需开发者按需处理，本 change 不接管原开发数据或改其初始化策略。

## 自动验收

```bash
./scripts/tests/local-config.test.sh       # 无 daemon 的配置故障检查
./scripts/tests/local-launcher.test.sh     # 无 daemon 的启动器故障检查
./scripts/smoke-local.sh                  # 新项目，从源码构建并验收
./scripts/smoke-local.sh --no-build       # 本地已有完整镜像时复用
./scripts/tests/server-image.test.sh
./scripts/tests/web-image.test.sh
./scripts/tests/local-init.test.sh
```

smoke 固定使用独立随机项目和 38080 / 39000 / 37880 / 37881 / 37882 端口，冲突时明确失败。验证登录、私有文件 PUT / confirm / GET、WebSocket ping / message.send / ack、再次启动及停止恢复，并比较原始 env 与手动调度设置。成功后停止测试容器，保留其 `.relayflow/smoke/<project>/local.env` 和专属卷；失败也保留证据。Node 验收容器使用内部传输但保留签名的 Host / path / query，浏览器直传另行验证原始 URL。

`.github/workflows/docker-local.yml` 在独立 runner 上执行相同检查，归档只选脱敏日志，不上传 env、token 或存储签名。真实双标签页媒体与浏览器 SSE 的操作和本次证据见 `openspec/changes/docker-local-one-command-start/verification.md`；无需模型凭证的 SSE 夹具验证代理分段，并不代表已验证外部模型服务。
