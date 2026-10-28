# 分步实施验证

## 1.1 本地配置生成与密钥持久化（2026-10-28）

入口：`./scripts/local-config.sh`。默认配置为 `.relayflow/local.env`，`--check` 只校验，`--no-pull` 禁止下载缺失的工具镜像。`RELAYFLOW_LOCAL_ENV_FILE` 可选择仓库内 `.relayflow/` 下或仓库外的配置；`COMPOSE_PROJECT_NAME` 选择新项目，已有项目不得静默切换配置路径。

该工具只准备配置，不启动应用。现有配置逐键校验后复用，缺项 / 损坏不自动补写密钥。首次生成需 Docker 可用且不存在对应项目旧卷 / 容器；使用摘要固定的 Alpine 3.23.4 无网络、只读、无宿主挂载容器读取随机源。生成文件权限为 `0600`，通过项目锁和不覆盖目标的原子发布保护。

已运行并通过：

- `bash -n scripts/local-config.sh scripts/tests/local-config.test.sh`（本机 Bash 3.2）。
- `./scripts/tests/local-config.test.sh`：21 项隔离检查，无真实 Docker 调用；覆盖首次生成、复用、损坏 / 缺项 / 重复键 / 注入拒绝、Docker 不可用、旧卷 / 容器、查询失败、生成部分失败、缺镜像、符号链接、并发、同项目不同路径、普通文件 / 目录 / 目录符号链接发布竞态、未忽略路径拒绝和信号清理。
- 真实 Docker：独立临时项目首次生成、逐字节复用、AES 解码为 32 字节、不同凭证独立生成、真实带项目标签的旧卷触发拒绝；仅清理本次临时目录、一次性容器和专用空卷，未触碰已有项目数据。
- 工具镜像清单确认为 OCI 多架构索引，包含 `linux/amd64`、`linux/arm64`；当前平台真实运行通过，未将镜像清单可用性等同于其他平台实测。
- `git check-ignore --no-index .relayflow/local.env`、`git diff --check`、`openspec validate docker-local-one-command-start --strict`。

本步未改 Java / 前端源码、Compose 服务或 Flyway，未运行无关的 Maven / 前端构建。

## 1.2 对象存储内部地址与浏览器签名地址（2026-10-28）

新增可选 `RELAYFLOW_MINIO_PUBLIC_ENDPOINT` / `RELAYFLOW_MINIO_REGION` 属性绑定及运行时地址解析。配置 public endpoint 时必须显式设置 region，避免 SDK 向浏览器地址发起 region 查询；原单地址、未指定 region 的配置保留原自动发现行为。

Bootstrap、租户有效上传、历史下载和 inline / 已保存配置连通测试统一处理受管地址匹配；匹配时保留租户凭证、bucket、前缀，不回写 JSON。独立 endpoint 不重定向。PUT / GET 在 SDK 签名前使用 public endpoint，启动校验 / HEAD / 删除使用内部 endpoint。

已运行并通过：

- `MinioAddressingTest` 6 项：属性绑定、地址规范化 / 独立地址、无 region 拒绝、双地址 PUT / GET 的独立 SigV4 校验、内部地址真实 HTTP I/O，以及旧单地址 region 自动发现。
- `StorageProviderServiceImplTest` 4 项：Bootstrap 上传 / 历史下载、受管租户内部与公开两种入口、独立租户配置保留、inline / 已保存配置连通测试。
- 定向 Maven 测试命令：`./mvnw -o -pl relayflow-module-infra/relayflow-module-infra-biz -am test -Dtest=MinioAddressingTest,StorageProviderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`。本机显式预加载缓存 Mockito agent，避免动态 attach 限制；本地 HTTP 夹具仅监听 `127.0.0.1`，在允许监听端口的执行环境运行。最终 10 项全部通过，未忽略失败。
- `./mvnw -o -pl relayflow-server -am compile`、`openspec validate docker-local-one-command-start --strict`、`git diff --check`。

测试放在已具备测试依赖的 `infra-biz`，未新增 Maven / npm 依赖、持久字段、API 字段或 Flyway 迁移。未改前端、未连接真实 MinIO；浏览器实际直传 / CORS / 完整容器网络仍在 task 2.1 / 2.4 / 3.2 验收。

## 2.1 MinIO 与 XXL-JOB 首次初始化（2026-10-28）

新增 `deploy/minio/init-local.sh`：幂等建桶、配置并核对同机 CORS。当前社区版 MinIO 通过服务级 `api.cors_allow_origin` 动态配置允许源，不调用桶级 CORS API；真实响应验证 PUT / GET / HEAD、ETag 暴露及非允许源拒绝。脚本不修改匿名策略、对象、桶标签及其他 API 设置。

新增 `deploy/xxl-job/init-local.sh` / `init-local.sql`：建库使用 session advisory lock；建表、官方 seeds 与 `relayflow_local_bootstrap` 标记同事务执行，并使用 transaction advisory lock 处理并发。首次失败整体回滚，可直接重试；完成后不再执行覆盖 Cron / 启停 / 执行器设置的 seeds。拒绝未标记旧调度表、未知标记版本、已初始化缺表及使用业务库作为目标。PostgreSQL 16 拒绝路径使用数据库异常确保非零退出，不依赖 `psql` 不支持的 `\quit` 退出码参数。

已运行并通过：

- `./scripts/tests/local-init.test.sh`：14 项真实 Docker 检查，覆盖 MinIO 建桶后 CORS 阶段故障再重试、普通重复执行、对象 / 标签 / 其他 API 设置保留、允许与拒绝源预检、保持匿名拒绝、签名下载字节及 ETag、认证失败重试；XXL 修改后的 Cron / 禁用 / 参数 / 执行器 / 账号 / 自定义任务保留、种子事务故障回滚与重试、并发、旧库 / 异常标记 / 缺表 / 业务库拒绝，以及删除容器保留卷再创建后所有既有对象与调度设置不变。
- 测试使用摘要固定的 PostgreSQL 16.13、MinIO `RELEASE.2025-04-22T22-12-26Z` 与 mc `RELEASE.2025-08-13T08-35-41Z`；只创建并清理本次独立网络、容器、卷及临时配置，不使用已有项目资源。
- `./scripts/tests/local-config.test.sh`：21 项配置回归检查全部通过。
- Shell 语法检查、`git diff --check`、`openspec validate docker-local-one-command-start --strict`。
- 核对原 `deploy/compose.yml`、`init-db.sh`、官方 schema 和 seeds 均未修改；旧开发 / 显式维护入口保留原重灌策略。

本步未改业务 Flyway、Java 或前端代码，未重复运行无关构建。初始化服务接入完整 Compose 属于 task 2.4，尚未完成；本轮不声称完整应用可一键启动。

## 2.2 后端 local-docker profile 与镜像（2026-10-28）

新增 `application-local-docker.yml`，显式选择 profile 后使用 PostgreSQL / Redis / MinIO / LiveKit / XXL 的容器地址，区分浏览器发布端口与内部端口，回调默认 `http://server:9999/`；必需凭证来自持久 env，不沿用开发密钥。原 `application-dev.yml` 和两份既有 Compose 未修改。变量契约与手工构建方法见 `deploy/local-server.md`。

后端 Dockerfile 改为按摘要固定 Java 21 构建 / 运行镜像，只复制 Maven 输入，不强制安装 Bash / 修改软件源 / COPY Maven mirror settings。仓库与 Wrapper 缓存保留，自定义 settings 用可选 BuildKit secret。Maven 保留阶段日志并允许重新检查失败依赖；运行用户 UID 10001，日志可写；健康检查必须请求成功且业务 `code=0`。

已运行并通过：

- `LocalDockerProfileTest` 7 项：加载 server 真实配置，验证不启用 dev、默认内部地址和持久密钥、发布端口隔离、显式 URL / 环境变量覆盖、租户覆盖、缺密钥拒绝、调度 token 两种注入方式以及旧 dev 行为。使用现有 infra-biz 测试依赖，无新增 Maven 依赖。
- `./mvnw -o -pl relayflow-server -am compile`。
- 镜像源码构建：`docker build --progress=plain --secret id=maven_settings,src=deploy/maven/settings.xml -f relayflow-server/Dockerfile -t relayflow-server:local .` 成功，Maven package 约 29 秒（依赖缓存已部分填充，非纯冷构建耗时）。默认官方仓库构建先后出现 Wrapper TLS 重置和 `jakarta.validation-api:3.0.2` TLS 握手失败，因此本机通过已有可选 settings 完成验证；未修改默认仓库或宿主机全局网络配置，也不宣称默认纯冷构建在此网络下已通过。
- `./scripts/tests/server-image.test.sh relayflow-server:local` 4 项：最终镜像的非 root Java 21、日志目录权限、真实打包的 local profile，及实际 HEALTHCHECK 对业务成功 / 业务错误 / HTML / 带成功正文的 HTTP 失败的判断。
- 固定构建镜像实测 Java 21.0.11，固定运行镜像实测 Java 21.0.12.1；两份官方索引均含 amd64 / arm64，实际构建和运行验证为当前 arm64 平台。
- Shell 语法检查、`git diff --check`、`openspec validate docker-local-one-command-start --strict`。

镜像验证未连接真实业务基础设施；完整应用启动、调度注册和音视频联调仍属于后续完整 Compose / 冒烟验收。Docker 打包跳过执行测试，测试通过证据以上述独立定向测试为准，不代表既有全量测试已全部通过。

下一步为 task 2.3：前端多阶段镜像与 Nginx 协议代理。

## 2.3 前端镜像与协议（2026-10-28）

`web/Dockerfile` 使用固定 Node 24.22.1 / Nginx 1.30.6，按现有 packageManager 激活 pnpm，复制 pnpm-workspace.yaml 后执行 frozen 安装、`pnpm build` 与 `pnpm typecheck`。真实构建通过，锁文件未修改；既有大 chunk 提示仍存在，不把提示视为构建失败。

`web/docker/default.conf` 提供 SPA 深链、REST 原路径 / 查询参数、WS Upgrade 和 SSE 无缓冲代理；Docker DNS 动态解析支持后端重建；访问日志只记录 URI 路径，避免 WS 查询 token 进入日志。

`scripts/tests/web-image.test.sh` 真实容器验证五项通过：SPA 深链、API 错误 / query 与缺失资源不回退 HTML、WS 双向帧、SSE 分段、日志不含测试 token。后端最终镜像的四项 `server-image.test.sh` 也已复验通过。

## 完整编排与启动器联调记录（2026-10-28）

完整栈使用独立项目卷与回环端口，初始化成功后才启动后端 / 前端。真实启动暴露并修正两个问题：后端 Docker COPY 必须包含根 `lombok.config`，否则已有的 @Lazy 无法进入生成构造器；XXL 镜像入口通过 `XXL_JOB_DB_TYPE` 选择 profile，不能仅设置 Spring 环境变量。local 现已显式使用 postgres，并关闭 Hibernate DDL 与 Spring SQL 自动初始化。真实调度库注册记录为 `relayflow-server|http://server:9999/`。

`local-launcher.test.sh` 的 10 项无 daemon 故障检查通过：daemon 不可用、Compose 版本、首次就绪命令、外部端口冲突、本项目端口复用、缺镜像、初始化失败、就绪超时、继承环境变量隔离、停止保留 env / 卷。错误日志脱敏检查覆盖持久密码、Bearer/JWT、签名 query 和常见 JSON 密钥。测试还发现并修正 macOS Bash 3.2 下中文标点紧随 `$image` 被识别成变量名的问题，改为 `${image}`。

真实隔离 `smoke-local.sh --no-build` 已完成一轮首次启动 → 登录 / 文件 / WebSocket → 再次启动 → 停止 → 重建容器 → 恢复检查 → 停止。项目 `relayflow-smoke-1793178518-3097` 逐字节配置比较通过，原文件和消息可读；调度任务 1 修改后的 Cron `0 17 3 * * ?` / 禁用状态以及手动执行器地址保留。地址断言显式设置 `address_type=1`，避免把 XXL 正常刷新自动注册地址误判成初始化覆盖。该轮采用原 MinIO 缓存产物；最终源码构建替换的回归记录追加于下文，不能以这一轮替代远程镜像可获取性验证。

## 浏览器验收（2026-10-28，macOS / Chrome 156 / Docker Linux arm64）

- `/app/login` 真实表单登录进入 `/app/messages`；已登录 `/app/messages` 刷新保持工作台。另一个隔离浏览器上下文直接访问受保护 `/app/meeting/123/room`，跳到 `/app/login?redirect=...`。
- 浏览器使用上传接口原样返回的 `http://localhost:29000` 签名 URL 完成 PUT 200，读取暴露的 ETag，确认上传，跟随授权下载跳转 GET 200 并比对字节；未替换签名 Host 或使用内部传输。
- 确定性 SSE 上游经实际 Nginx 转发，浏览器 fetch 首段 22 ms、末段 1520 ms，两段内容分别为 first / second。没有外部模型凭证，不声称已验证外部模型推理。
- 使用两个不同账号的隔离标签页，在实际会议页面加入同一会议。通过 `browser-media.js` 注入合成 canvas / oscillator 音视频轨道，不读取宿主机摄像头或麦克风；媒体经过真实 LiveKit / WebRTC。原 1.8.4 出现发布 / 协商超时，local 更新到固定 1.13.11 后通过。
- 主持人端 inbound audio 243693 bytes / 2013 packets，inbound video 901138 bytes / 1671 decoded frames；另一端 inbound audio 288463 bytes / 2383 packets，inbound video 948594 bytes / 1730 decoded frames。两个页面的本地 / 远端视频均为 320×180。这是实际 RTP 传输与解码证据，不等同于物理设备或任意 NAT / 局域网平台测试。
- 点击主持人“结束会议”，列表显示已结束，`/app-api/meeting/get` 重新读取状态 `ENDED` / code 0。

## 文档、CI 与源码门禁（2026-10-28）

README 默认快速体验切换到 `start-local.sh`，保留宿主开发 / 原 Docker 后端入口；详细配置、备份、端口、网络、停止 / 恢复、离线边界和版本清单见 `docs/user/local-start.md`。CI 配置 `.github/workflows/docker-local.yml` 使用独立 Ubuntu runner，运行配置 / 故障检查、源码 compile / 受影响测试、源码镜像构建、冷启动 / 恢复、协议 / 初始化检查，只归档脱敏日志。YAML 已解析验证；未 push，也未触发或声称远程 GitHub Actions 成功。

最终已复验 `./mvnw -o -pl relayflow-server -am compile`；受影响 `LocalDockerProfileTest` 7、`MinioAddressingTest` 6、`StorageProviderServiceImplTest` 4 共 17 项通过（0 failure / error / skip），本机使用缓存 Mockito javaagent 与可监听回环端口的环境。21 项配置检查、10 项启动器检查、原 / prod Compose config、Shell 语法与 git diff --check 通过。未新增 Maven / npm 依赖或 Flyway；不把定向测试结果宣称为全量测试 / lint 通过。早期审查已发现的无关 `TenantBootstrapServiceTest` UnnecessaryStubbing 和前端 lint 基线问题不属于本 change，未通过忽略失败掩盖。

## 最终 MinIO 冷构建补齐与回归（2026-10-28）

远程索引检查发现本机旧 MinIO / mc 缓存不能代表新用户可拉取：Docker Hub 返回 insufficient_scope，官方 Quay tag 返回 401，官方二进制校验和地址返回 410。因此完整 local 栈改为构建 `relayflow-minio:local` / `relayflow-mc:local`，不使用旧缓存镜像作为构建输入。

`deploy/minio/Dockerfile` 使用固定 Go 1.24.13 / Alpine 3.23.4 摘要，源码不可变版本分别为 `v0.0.0-20250422221226-0d7408fc9969` 和 `v0.0.0-20250813083541-7394ce0dd2a8`；官方 Go 模块代理返回的 Origin 对应完整提交，与原已验证版本相同，由官方 Go sumdb 校验。源码未修改，构建保留 `-mod=readonly`、许可证和源码元数据。最终二进制的 --version 真实输出同一 RELEASE / commit，运行工具链为 Go 1.24.13 arm64。

本机 Go 代理曾有 EOF / 下载停滞，重试复用校验过的缓存后，两份源码镜像均成功构建；没有强制第三方镜像源、复制缓存中的旧二进制或修改全局网络。最后一轮 MinIO 构建阶段约 136 秒（已部分缓存，不是完整纯冷耗时）。Go 工具链与全部外部基础镜像已核对 amd64 / arm64 索引；两份本地源码镜像当前实际执行验证为 arm64，amd64 原生构建 / 冷启动留给已配置 CI 的运行结果，未宣称本机验证了另一架构。

最终结果：

- `local-init.test.sh` 已在两份新源码镜像上重跑，14 项真实检查全部通过，包括已有对象、API 设置、Cron / 启停 / 手动地址、账号、失败事务重试、并发和容器重建保留。
- 新隔离项目 `relayflow-smoke-1793180178-20755` 使用新源码镜像与空卷，完成首次启动、登录权限、签名文件 PUT / ETag confirm / private GET、WebSocket ping / message.send / ack；再次启动、停止重建恢复均通过，env 逐字节一致，原始文件 / 消息及手动调度设置保持。最后停止测试容器并保留本次配置与卷。
- 旧验收项目替换到两份新镜像后全栈健康；浏览器再次用原始签名 URL PUT 200、暴露 ETag、下载 200、字节一致。
- 最终入口 `RELAYFLOW_LOCAL_ENV_FILE=.relayflow/verification/local.env ./scripts/start-local.sh --maven-settings deploy/maven/settings.xml` 实际执行四个 build target、初始化及全栈有界就绪，通过。应用重建后的旧文件和消息读取另有验证；自定义配置下成功输出带完整配置来源的状态 / 停止命令。

首次构建依然依赖可访问的官方下载源，`--no-build` 只要求已有四份完整本地镜像、其余运行时镜像和 Alpine 工具，不要求宿主机 Go。文档和 CI 已同步这一构建路径。所有任务按本机实际证据勾选，远程 CI 未运行、默认 Maven Central 在本机的 TLS 限制及物理设备 / 其他平台未实测边界保持明确。
