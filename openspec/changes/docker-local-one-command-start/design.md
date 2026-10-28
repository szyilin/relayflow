## Context

现有 `deploy/compose.yml` 只编排基础设施，`compose.prod.yml` 追加后端但仍要求宿主机 Vite。后端默认加载 `dev` profile，Redis / MinIO 默认地址指向宿主机视角；MinIO 无建桶步骤，XXL-JOB 种子重复执行会重设 Cron / 启停状态。前端的生产静态服务、WebSocket / SSE 代理和完整首次启动验收尚未提供。

本 change 为 **[平台]** 交付，维持根目录 Java Maven 父工程、`web/` Vue 工程和单一 `relayflow-server`。用户已授权按依赖顺序实现剩余 tasks；逐项以实际验证为准。

## Goals / Non-Goals

**Goals:**

- 新用户克隆仓库后，在安装并启动 Docker 的本机执行 `./scripts/start-local.sh`，无需安装 Java、Node、pnpm 或手工建桶即可打开 `http://localhost:18080/app/login`。
- 默认启动完整本地栈，使用现有体验账号 `19988888888 / 123456`；AI 模型凭证后续在产品中配置，不阻塞基础服务启动。
- 首次初始化、再次执行、重建应用镜像、停止后恢复都具有明确的数据与配置保留语义。
- 自动化验证覆盖真实登录、文件直传 / 下载和 WebSocket；浏览器走查覆盖 SPA 深链、SSE 代理与本机会议。

**Non-Goals:**

- 不交付生产 TLS / 域名 / 备份 / HA、发布镜像仓库或离线安装包，不修改 Docker daemon 或强制全局镜像源。
- 不自动启动宿主机 Java / Vite，不新增原生 PowerShell 启动器；首批入口为 Bash（Linux、macOS，Windows 通过 WSL2）。
- 不改变租户 / 权限模型，不解决本次审查中的文件 ACL、文档保存或 Agent 并发等独立问题。
- 不改业务表、Flyway 历史、默认业务数据策略，不新增 Maven / npm 依赖；不引入 Gateway、Nacos 或新业务服务。
- 默认承诺同机浏览器体验；手机、局域网 / 公网访问及 TURN 规划沿用专项文档，不自动猜测宿主机 IP。

## Decisions

### D1. 两个入口，新增完整本地栈

- **体验入口**：`scripts/start-local.sh` + `deploy/compose.local.yml`；**开发入口**：保留 `deploy/compose.yml` + 宿主机 Java / Vite。
- 新本地 Compose 根据现有服务定义扩展，复用 LiveKit 配置、XXL schema / seeds、Maven Wrapper；不直接 include 具有固定 `container_name` 与开发端口暴露的旧栈，避免隐式继承宿主机地址。
- 本地栈不使用固定 `container_name`；统一显式项目名，默认 `relayflow-local`，高级调用可通过 `COMPOSE_PROJECT_NAME` 覆盖。卷和网络随项目隔离，CI 使用独立项目名与临时配置。
- 保留旧 `compose.prod.yml` 兼容入口，本次不把它扩展为生产方案；README 将“前端始终在宿主机”收窄为开发模式说明。
- 默认完整栈包含 `web`、`server`、`postgres`、`redis`、`minio`、`minio-init`、`xxl-job-db-init`、`xxl-job-admin`、`livekit`；初始化服务成功退出属于正常状态。
- local 的 XXL 镜像入口显式设置 `XXL_JOB_DB_TYPE=postgres`，并关闭 Hibernate DDL / Spring SQL 自动初始化；schema 与 seeds 完全交给首次初始化服务，避免镜像入口的默认 MySQL profile 覆盖 Spring 环境配置。

选择独立 local 栈，以便先验收完整路径；把旧 Compose 全量重构成多层继承会扩大本轮回归面。共享定义的后续提炼另行评估，当前用配置检查防止版本 / 参数漂移。

### D2. 固定配置来源，首次生成后保留

- 默认本地配置为 `.relayflow/local.env`；脚本根据自身位置定位仓库，始终传入绝对 `--env-file`、Compose 文件与项目名，不依赖当前目录的隐式 `.env` 搜索。
- `RELAYFLOW_LOCAL_ENV_FILE` 可指定另一份配置文件，供隔离验收使用；启动 / 停止 / 日志说明均使用同一配置源。脚本不 `source` 未经验证的 env 为 shell 代码，也不执行文件内容。
- 自定义配置在仓库内时必须位于已忽略的 `.relayflow/`，或使用仓库外路径；按规范化后的真实父目录检查，避免密钥文件落入 Git / Docker 构建上下文。项目内记录项目名与配置路径的绑定；同项目不同路径不得另生成一套密钥。跨克隆目录 / 远程多客户端的全局互斥不在配置工具的保证范围。
- 模板保存非敏感默认值。首次生成 PostgreSQL / MinIO 凭证、JWT secret、32 字节 AES key（Base64）、XXL access token、LiveKit key / secret，使用固定版本工具容器的密码学随机源，无宿主机 OpenSSL / Node / Python 依赖。
- 初始化先用同目录临时文件完整写入，再原子落盘并限制权限；已有文件只校验，不覆盖 / 不补写旋转密钥。相同项目的并行启动必须串行化生成过程。临时文件、env、token 和运行产物均被 Git / Docker 构建上下文忽略。
- 缺失、损坏配置或已存在项目数据卷而配置丢失时，明确报错并给出恢复说明；禁止自动新建密钥后挂载旧卷。用户可从备份恢复原配置。
- Compose 只显式转发各服务需要的变量。`server` 使用 `local-docker` profile，继承基线而不加载 `dev` 中的宿主机回调与 V2 默认值；本地默认仍为单租户，支持现有 `RELAYFLOW_TENANT_*` 覆盖。

配置契约（凭证未列具体值，均来自持久配置）：

| 变量 / 属性 | 默认 / 用途 |
|---|---|
| `LOCAL_WEB_PORT` | `18080`，绑定 `127.0.0.1`，浏览器唯一应用入口 |
| `MINIO_API_PORT` | `19000`，绑定 `127.0.0.1`，浏览器直传及下载 |
| `LIVEKIT_PORT` / `LIVEKIT_RTC_PORT` / `LIVEKIT_UDP_PORT` | `7880` / `7881` / `7882`，本机信令与媒体；UDP 映射与 `rtc.udp_port` 一致 |
| `SPRING_PROFILES_ACTIVE` | `local-docker`（由本地 Compose 固定注入） |
| `SPRING_DATASOURCE_URL` / `SPRING_REDIS_HOST` | `jdbc:postgresql://postgres:5432/relayflow` / `redis`，内部端口不随宿主机发布端口改变 |
| `RELAYFLOW_MINIO_ENDPOINT` | `http://minio:9000`，内部对象操作 |
| `RELAYFLOW_MINIO_PUBLIC_ENDPOINT` | `http://localhost:${MINIO_API_PORT}`，浏览器预签名 URL |
| `RELAYFLOW_MINIO_REGION` | 受管本地 MinIO 的固定 region，与服务初始化配置对齐 |
| `RELAYFLOW_LIVEKIT_URL` / `RELAYFLOW_LIVEKIT_API_URL` | `ws://localhost:${LIVEKIT_PORT}` / `http://livekit:7880` |
| `RELAYFLOW_JOB_ADMIN_ADDRESSES` / `RELAYFLOW_JOB_EXECUTOR_ADDRESS` | `http://xxl-job-admin:8080/xxl-job-admin` / `http://server:9999/` |
| `RELAYFLOW_LOCAL_START_TIMEOUT_SECONDS` | `900`，服务启动与就绪阶段的有界等待；镜像拉取 / 编译单独显示进度 |

PostgreSQL、Redis、后端 API / 执行器、MinIO Console、XXL 控制台默认只在项目网络内使用，不额外抢占开发端口。诊断文档提供按需访问控制台的方法；本地体验账号只用于这套回环接口。

### D3. 可复现的容器构建与同源前端

- 后端保留 Java 21 多阶段构建，Maven Wrapper 与仓库 BOM 不变；构建缓存保留，但不复制宿主机 `target`、`.env` 或 `.relayflow`。不要求使用者配置第三方镜像源。
- 构建输入包括根 `lombok.config`，确保源码已有的 `@Lazy` / `@Qualifier` 能复制到生成构造器参数；缺失该文件会导致镜像构建成功但业务上下文出现依赖循环。
- 后端只复制 Maven 构建输入；自定义仓库 / 代理 settings 可通过 BuildKit secret 临时挂载。保留 Maven 阶段日志并允许重试失败依赖；运行镜像使用非 root 用户与可写日志目录，健康检查同时检查 HTTP 成功和业务 `code=0`。本地 profile 由调用方显式指定，旧开发 profile 保持原行为；注入契约见 `deploy/local-server.md`。
- `web/Dockerfile` 用与现有 pnpm 版本兼容的固定 Node 镜像，按 `packageManager` 固定 pnpm，`--frozen-lockfile` 安装，构建后交给固定版本 Nginx 运行；无需生产 Vite 进程。
- 前端资源和相关 Nginx 配置放在 `web/`；SPA `/app/*` / `/admin/*` 刷新回退 `index.html`，`/admin-api`、`/app-api`、`/infra/ws` 优先匹配并保留原路径，API 错误不得回退 HTML。
- WebSocket 转发 Upgrade / Connection；Agent SSE 禁缓冲 / 缓存并设置适合长连接的读取超时；浏览器继续使用相对 API / WS 地址，不把 `server` 主机名写进静态资源。
- 镜像锁定经验证的具体版本（含 MinIO 服务与客户端配对版本），验证 `linux/amd64` 与 `linux/arm64` 可用性。版本清单随实现记录于 local 部署说明；不得沿用 `latest` 作为可复现承诺。
- 远程拉取核对发现旧 MinIO / mc 仓库已不可取，因此 local 用固定 Go / Alpine 容器构建相同官方源码版本。源码为不可变 Go 模块版本并由官方 sumdb 校验，不依赖宿主机旧镜像缓存，也不采用第三方不明二进制；首次网络依赖增加 Go 官方模块代理 / 校验和服务。源码、许可证链接及 revision 随本地镜像保留，不新增应用 Maven / npm 依赖。
- local LiveKit 固定为 1.13.11；原 1.8.4 与现有 JS 2.21.0 客户端在真实媒体测试中出现协商超时。新版本保持本机回环地址约定，并以双浏览器上下文实际 RTP 收包 / 视频解码验证，不仅检查信令端口。

选择生产静态构建便于无本机工具体验；Vite HMR 仍由原开发入口承担。首次构建需要网络，不把首次拉取 / 编译时间误称为已就绪时间。

### D4. 对象存储内部操作与签名地址分离

当前一个 endpoint 同时承担服务端 I/O 与浏览器签名；改成 `minio:9000` 会使浏览器不可达，改成 `localhost` 又会让容器内 I/O 失败。

- 在 OSS 运行时配置中增加可选 `publicEndpoint` / `region`，部署属性绑定 `relayflow.storage.minio.public-endpoint` / `region`；未设置双地址时按现有单 endpoint 行为处理。
- 受管地址对来源于 Bootstrap：内部 endpoint 用于启动校验、测试连接、HEAD、删除和 region 查询；公开 endpoint 在生成 presigned PUT / GET **之前**参与 SDK 签名，不在生成后替换 URL 字符串。
- 签名客户端使用明确的受管 region（或经内部客户端取得的 region），不为查询桶 region 回连浏览器的 `localhost`。本地 MinIO 与签名客户端的 region 必须一致。
- task 1.2 采用显式 region：配置 public endpoint 时必须同时配置 region，缺失则拒绝配置；不配置 public endpoint 的旧部署保留 SDK 原有 region 自动发现。
- 对 `infra-biz` 上传有效配置、历史下载配置及 inline 连通测试统一解析：租户 endpoint 若规范化后精确匹配受管内部或公开地址，则应用这对地址，同时保留租户 credentials / bucket / pathPrefix；其他独立存储 endpoint 原样保留。不能无条件把所有租户配置指向默认 MinIO。
- 不回写租户 `config_json`，不新增 DO / 表 / 管理端字段；已有部署未设置 public endpoint 时兼容原行为。local fresh install 使用 Bootstrap，无需预插 `infra_storage_provider`。
- MinIO init 设置仅供本地应用源使用的必要 CORS 规则（上传、下载、读取 ETag），不创建匿名公开桶策略；验收使用浏览器原始返回的签名 URL 完成 PUT / confirm / GET。

### D5. 首次初始化与日常启动分开

- MinIO 服务健康后，由 `minio-init` 幂等创建 `relayflow` 桶及所需 CORS；已有桶 / 对象保留，错误退出阻止后端启动。
- 当前社区版 MinIO 使用服务级动态 `api.cors_allow_origin`，不调用桶级 CORS API；重复执行只收敛同机应用的两个允许源，保留其他 API 设置，不改变桶策略。PUT / GET / HEAD 预检和 ETag 暴露以真实 HTTP 响应验收。
- XXL 使用已有 schema / 官方种子；local 初始化在调度库内记录成功标记（基础设施元数据，不是业务 Flyway）。未初始化才应用 schema / seeds，标记在全部成功后写入；失败重试不把半初始化视为完成。
- 建库用 session advisory lock；建表 / seeds / 标记同事务并用 transaction advisory lock 串行。存在未标记的旧调度表、未知标记版本或已初始化必需表缺失时明确失败，不自动接管 / 重灌；已有业务库不得作为调度初始化目标。
- 已初始化时，日常 `up` / 应用重建不重灌会覆盖 Cron、启停或执行器地址的 UPDATE。显式更新官方种子的维护命令沿用现有文档，本 change 不建立自动升级机制，也不改变旧开发入口的种子策略。
- 业务 schema / 默认体验账号沿用 Flyway。A 类基础设施资源 eager ensure；B / C 类业务默认不额外预插。禁止增加跨域“统一灌表”服务。
- 依赖链：PostgreSQL / Redis 健康、MinIO 建桶成功、XXL 初始化成功与 Admin 就绪 → server 完成 Flyway / 存储校验 → web / 登录路径可用。LiveKit 信令也纳入就绪检查。

### D6. 启动器只做编排与诊断

- 前置检查：Docker CLI、可连接 daemon、Compose 版本与所需选项、配置完整性、实际发布端口冲突；最低支持 Compose `2.24`，更高主版本按能力检查兼容。端口已由本项目占用属于再次执行，不误报为外部冲突。
- 默认在线执行镜像准备 / 构建，再 `up --wait --wait-timeout`；不使用固定 sleep 代替就绪检查，不在 Docker 未启动时擅自启动 Docker Desktop。
- `--no-build` 为已有应用镜像的复用入口，配合禁止拉取策略；缺镜像时直接说明缺项。无网络时不会偷偷重建或下载依赖。
- 成功前检查前端页面、经前端代理的应用接口（业务 `code=0`）和 LiveKit 信令；`scripts/smoke-local.sh` 补充真实登录、文件与 WebSocket 检查，使用现有协议及隔离测试数据。
- 失败返回非零码，输出失败阶段、服务名、同项目状态与有限日志；脱敏密码、完整 JWT、签名 URL 查询串。成功只输出访问地址、本地体验账号和日志 / 停止方法，不打印基础设施密钥。
- `scripts/stop-local.sh` 复用相同配置 / 项目解析，停止并移除本项目容器网络，默认不传 `-v`。普通启动 / 停止不清卷；用户手工清空数据需另行显式操作，不提供自动重置捷径。

### D7. 验收与 CI 以真实冷启动为准

| 验收 | 最低证据 |
|---|---|
| 空环境 | 独立项目、无既有卷、无本地配置，从配置生成到 `/app/login` 可用 |
| 功能连通 | 种子账号登录并取权限；文件上传会话 → 浏览器直传 → confirm → 下载校验字节；WebSocket 握手与心跳 / 实时投递 |
| 前端协议 | 刷新 `/app/login` 与受保护深链；API 错误为 API 响应；SSE 分段响应及时到达且不断流 |
| 可重复执行 | 再次启动、重建应用、停止后恢复，配置文件 / 密钥不变，测试文件 / 消息仍在，修改后的调度 Cron / 启停不被重置 |
| 错误反馈 | daemon 不可用、端口冲突、无效配置、init 失败、就绪超时均非零退出且可定位 |
| RTC | 同机两标签页入会验证真实音视频及结束会议；仅签发 token / 端口打开不算媒体验证 |

- 冒烟工具放在脚本或测试容器中，不新增应用 Maven / npm 依赖。CI 建立独立项目，失败归档脱敏状态和日志，只清理本次专属资源；测试数据不混入用户已有卷。
- CI 自动覆盖 Compose 配置、配置生成 / 复用、首次启动、登录、文件、WebSocket 及停止恢复；无模型凭证的 SSE 代理用确定性的本地上游夹具验证，不要求收费外部模型。RTC 真实媒体保留浏览器验收记录。
- 源码验证执行 `./mvnw -pl relayflow-server -am compile`、受影响测试、`pnpm build` / `pnpm typecheck` 和本 change strict validate。既有全量测试 / lint 问题单独记录，不通过“忽略失败”宣称门禁通过。

## Risks / Trade-offs

- 首次网络拉取 / Maven 与 pnpm 依赖耗时 → 分阶段输出进度、锁定版本、利用 Docker 缓存；预构建发布镜像作为后续独立工作。
- 旧开发 Compose 与新体验栈有服务定义重复 → 优先保留旧入口，测试 local 的完整依赖与镜像版本；不在本轮顺手重排所有部署文件。
- Docker Desktop / WSL 媒体网络差异 → 保留已有 LiveKit UDP 单端口约定，记录实际验证平台，不宣称任意 NAT 可用。
- `.relayflow/local.env` 丢失会影响数据库认证、加密数据和后续登录 → 不自动旋转，文档要求配置与卷一同保存。
- MinIO 公开签名地址 / region / CORS 不一致 → 覆盖双地址、租户匹配 / 独立配置和真实浏览器直传测试。
- 当前 HTML 引用外部字体，图标也需核对运行时请求 → 区分“预构建后核心服务可离线运行”与“浏览器零外联”；字体不可达应有回退，不在本轮承诺完整离线前端资源包。

## Migration Plan

1. 按 tasks 先落配置与存储适配，再完成初始化 / 镜像 / Compose，随后启动器、验收与文档 / CI；每步单独验证后再继续。
2. 新入口默认创建独立项目和新卷，不自动接管既有 `relayflow-*` 容器或导入旧数据；无需 Flyway 新迁移、产品版本升级或用户数据转换。
3. README 快速体验切换到新入口，同时保留开发章节及旧入口的兼容说明；更改过的部署规格通过本 change 归档合入真源。
4. 回滚时用匹配配置执行停止，保留 env 和卷；释放端口后可继续旧开发路径。任何已有 schema 版本不支持的 JAR 回退按原数据库规则处理，不执行自动降级或删卷。

## Open Questions

无阻塞规划的产品决策。具体固定镜像 tag、平台实测结果和构建耗时在对应实施任务中记录；某平台 / 镜像不可用时先报告并收窄兼容声明，不以未经验证的 tag 代替验收。
