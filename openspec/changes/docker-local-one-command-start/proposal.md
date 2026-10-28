## Why

首次克隆后的启动链路仍依赖宿主机 Java / Node / pnpm、手工配置存储及多个终端；现有 Docker 后端入口缺少完整的容器网络参数，且未包含前端。需要先交付可重复验证的本机完整体验入口，让使用者安装 Docker 后即可一条命令启动并登录。

## What Changes

- 新增 `./scripts/start-local.sh` 与 `deploy/compose.local.yml`：容器内构建并启动前端、唯一后端进程、PostgreSQL、Redis、MinIO、XXL-JOB 和 LiveKit；成功输出 `/app/login` 地址及本地体验账号。
- 本地配置首次自动生成，密钥持久保存，应用所需环境变量显式注入；自动创建存储桶并初始化调度库，业务 schema 继续使用已有 Flyway 迁移。
- 统一处理容器内部地址与浏览器地址，包括对象存储签名、LiveKit 管理连接、调度执行器回调，以及前端反向代理的 SPA / REST / WebSocket / SSE 行为。
- 启动入口执行前置检查、有界等待、业务连通检查和失败诊断；重复启动与停止保留配置、数据和用户修改的调度设置。
- 增加隔离的首次启动 / 再次启动 / 停止恢复冒烟与 CI，README 默认介绍完整体验，再介绍宿主机开发调试。
- 保留 `deploy/compose.yml` 的基础设施开发入口和既有宿主机 HMR / 断点方式；现有 `compose.prod.yml` 不再作为新用户默认入口，本 change 不扩展为生产部署改造。

## Capabilities

### New Capabilities

无；启动编排与存储双地址行为分别归入现有部署、基础设施规格。

### Modified Capabilities

- `deployment`：定义一键本地体验、配置及初始化生命周期、前端容器与代理、就绪和失败反馈、持久化与隔离验收；更正现有把基础设施 Compose 当作完整应用入口的场景。
- `infra`：定义受管对象存储的内部操作地址与浏览器签名地址，保证默认配置、匹配受管地址的租户配置及历史文件下载一致，未配置双地址时兼容原行为。

## Impact

- 类型：**[平台]**，无新增产品页面或业务 API，无需 `-web → -api → -integrate` 业务切片。
- Java：影响 `relayflow-spring-boot-starter-oss` 的运行时配置 / 签名适配及必要的 `infra-biz` 配置解析；`relayflow-server` 增加独立本地容器 profile。不改 Maven 模块布局、版本及业务依赖方向，不新增 Maven / npm 依赖。
- 前端：仅在 `web/` 增加容器构建及静态站点反向代理配置，沿用现有 Vue / Nuxt UI 工程、锁文件和路由。
- 部署与工具：`deploy/`、`scripts/`、忽略规则、构建上下文、README / 用户运维说明及 CI。镜像使用经验证的固定版本，不自动修改使用者的 Docker daemon 或镜像源配置。
- 数据与回滚：不新增或重写 Flyway 迁移，不改表、不灌业务偏好默认行；本地栈使用独立 Compose 项目和数据卷。停止保留数据，回退到既有开发入口前释放冲突端口；不得把新配置 / 密钥丢弃后继续挂载原数据卷。
- 本轮只创建 proposal / design / specs / tasks；实施任务待用户明确要求开始后逐步执行，不因规划产物齐备自动进入实现。
