## MODIFIED Requirements

### Requirement: 自托管单节点部署

系统必须（MUST）支持在单机通过 Docker Compose 运行完整的本地体验栈，包括前端、唯一 `relayflow-server`、PostgreSQL、Redis、MinIO、XXL-JOB 和 LiveKit。完整本地入口必须为 `./scripts/start-local.sh`，使用 `deploy/compose.local.yml`；`deploy/compose.yml` 继续作为宿主机开发的基础设施入口。

#### Scenario: 全新安装

- **GIVEN** 使用者已克隆仓库，Docker 已运行，具备 Bash 和所支持的 Compose，首次构建可联网
- **WHEN** 在仓库执行 `./scripts/start-local.sh`
- **THEN** 前后端在容器内构建并启动，依赖资源自动初始化，浏览器可打开输出的 `/app/login` 地址并登录
- **AND** 不要求宿主机安装 Java、Node、pnpm 或手工建桶，不依赖外部 SaaS 才能启动基础功能

#### Scenario: 保留宿主机开发

- **WHEN** 开发者使用 `deploy/compose.yml` 并在宿主机运行后端及 Vite
- **THEN** 原基础设施与 HMR / 断点开发方式仍受支持
- **AND** 文档明确该入口与完整本地体验入口的区别

### Requirement: 默认租户环境变量

自部署安装必须（MUST）支持通过环境变量覆盖租户模式。完整本地栈必须显式把 `RELAYFLOW_TENANT_ENABLED`、`RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER` 与 `RELAYFLOW_TENANT_DEFAULT_ID` 传入应用，不能因为沿用宿主机 `dev` profile 改变默认模式。

#### Scenario: Docker Compose 部署

- **WHEN** 运维通过 `start-local.sh` / `deploy/compose.local.yml` 启动应用
- **THEN** 可通过持久本地配置中的租户环境变量控制租户行为
- **AND** 未覆盖时使用单租户、默认租户 `1`，与既有自部署默认值一致

## ADDED Requirements

### Requirement: 本地配置与密钥生命周期

本地启动器必须（MUST）首次生成持久配置，显式选择 env 文件并向各容器注入所需变量；必须兼容从任意工作目录定位同一仓库配置。已有配置不得被普通启动、重建或停止覆盖；密钥不得提交到仓库或进入应用镜像。

#### Scenario: 首次生成

- **GIVEN** 不存在本项目配置和数据卷
- **WHEN** 使用者运行启动入口
- **THEN** 自动生成所需凭证、JWT / AES / 调度 / RTC 密钥并安全保存，且无需额外安装宿主机生成工具

#### Scenario: 保留配置

- **GIVEN** 已成功启动过且存在本地配置
- **WHEN** 再次启动或重建应用镜像
- **THEN** 已有配置及密钥保持不变，应用使用相同配置访问数据卷

#### Scenario: 防止遗失配置后误接旧卷

- **GIVEN** 本项目数据卷存在但本地配置遗失或不完整
- **WHEN** 运行启动入口
- **THEN** 以非零码退出并说明如何恢复原配置，不自动生成新密钥挂载旧数据

### Requirement: 本地初始化顺序与非破坏性重复启动

本地栈必须（MUST）在依赖健康、MinIO 桶初始化及调度库初始化成功后启动应用；业务表必须继续由现有 Flyway 管理。日常启动必须保留已有对象、业务数据以及用户修改的调度设置。

#### Scenario: 依赖尚未就绪

- **WHEN** PostgreSQL / Redis 未健康，或建桶 / 调度库初始化失败
- **THEN** 后端不得被报告为启动成功，启动入口等待或明确失败

#### Scenario: 首次与再次初始化

- **GIVEN** 首次启动需创建存储桶、调度表和官方种子
- **WHEN** 初始化完成后再次运行同一入口
- **THEN** 不重复创建资源、不覆盖已有文件，不重置用户修改的 Cron、启停状态或执行器设置
- **AND** 半初始化失败后允许安全重试，仅在完整成功后记录完成状态

#### Scenario: 并发首次初始化

- **WHEN** 同一调度库同时收到多个本地初始化请求
- **THEN** 建库与种子初始化串行完成，默认任务和执行器不得重复创建

#### Scenario: 既有调度数据不满足本地初始化契约

- **GIVEN** 存在未标记的既有调度表、未知版本的完成标记，或已初始化但必需表丢失
- **WHEN** 执行本地初始化
- **THEN** 非零退出，不自动接管旧表、重灌种子或修复覆盖数据

#### Scenario: 本地对象存储跨域配置

- **WHEN** 初始化本地受管 MinIO
- **THEN** 同机应用允许源的 PUT / GET / HEAD 预检及 ETag 暴露可用，非允许源不获得跨域授权，匿名对象读取仍被拒绝
- **AND** 重复初始化仅收敛受管 CORS 源，不更改其他 API 设置、桶策略或已有对象

### Requirement: 前端静态服务与协议代理

本地体验必须（MUST）通过前端容器提供页面，沿用相对 API / WebSocket 路径，并正确代理 REST、WebSocket 及 SSE。

#### Scenario: 页面深链与 API 分流

- **WHEN** 浏览器刷新 `/app/*` / `/admin/*` 深链或请求 `/app-api` / `/admin-api`
- **THEN** 页面请求正确回退 SPA，API 保留原始路径与状态，不回退为 HTML 页面

#### Scenario: 实时协议

- **WHEN** 已认证客户端连接 `/infra/ws` 或接收 Agent SSE 分段响应
- **THEN** WebSocket 握手及双向消息可用，SSE 分段及时到达且不被代理缓存或缓冲

### Requirement: 本地网络地址与能力配置

本地栈必须（MUST）区分容器间地址与浏览器地址，显式注入数据库、Redis、存储、RTC 与调度配置。默认只承诺同机浏览器访问，模型凭证缺失不得阻止基础服务启动。

#### Scenario: 容器与浏览器分别连通

- **WHEN** 本地后端访问基础设施，浏览器上传下载、连接 LiveKit，调度中心调用执行器
- **THEN** 分别使用对应可达地址和一致凭证；不得把容器 `localhost` 当作其他服务，也不得把内部服务名下发为默认浏览器地址

#### Scenario: 独立本地容器 profile

- **WHEN** 调用方显式选择 `local-docker` profile 并注入持久本地配置
- **THEN** 不加载 `dev` 的多租户或宿主机回调默认值，容器间服务使用内部端口，宿主机发布端口只影响对应浏览器地址
- **AND** 必需凭证缺失时明确报错，不回退开发密钥；原 `dev` 入口保留既有行为

### Requirement: 就绪反馈与失败诊断

启动器必须（MUST）检查运行前提并有界等待服务就绪；成功之前必须验证页面及经前端代理的业务接口，不能仅以容器运行或 HTTP 200 判定成功。失败必须返回非零码并提供脱敏诊断。

#### Scenario: 成功反馈

- **WHEN** 初始化、服务健康与基础接口检查全部通过
- **THEN** 输出登录地址、本地体验账号、查看日志与保留数据的停止方法

#### Scenario: 启动失败

- **WHEN** daemon 不可用、版本不支持、端口被其他项目占用、配置无效、初始化失败或等待超时
- **THEN** 明确报告失败阶段 / 服务和相关诊断，不输出基础设施密钥、完整 JWT 或完整签名 URL
- **AND** 不自动删除卷、旋转密钥、启动 Docker Desktop 或修改全局镜像配置

### Requirement: 停止与隔离验证

本地栈必须（MUST）按项目隔离容器、网络和数据卷，普通停止必须保留数据。自动化冷启动验证必须使用独立项目与临时配置，不接管使用者的开发环境。

#### Scenario: 停止后恢复

- **GIVEN** 本地栈已有文件与业务数据
- **WHEN** 执行文档提供的停止入口后再次启动
- **THEN** 配置、密钥、文件与业务数据保留，能够再次登录并读取原数据

#### Scenario: 自动化冷启动验收

- **WHEN** CI 或开发者执行隔离冒烟
- **THEN** 验证首次启动、真实登录、文件上传确认与下载、WebSocket、再次启动及停止恢复
- **AND** 失败保存脱敏诊断，仅清理本次独立项目资源

### Requirement: 首次构建与离线复用边界

文档必须（MUST）区分首次在线构建与已有完整应用镜像的复用，提供禁止构建 / 拉取的本地启动方式；不得把只有基础镜像的环境宣称为可离线首次构建，也不得将核心服务可离线运行等同于浏览器零外联。

#### Scenario: 已有镜像复用

- **GIVEN** 所需完整应用和基础设施镜像均已存在，配置有效
- **WHEN** 执行禁止构建 / 拉取的入口
- **THEN** 不下载构建依赖或镜像；缺镜像时报告缺项，核心服务不依赖外部模型才能启动
