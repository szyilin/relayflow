# infra-job Specification

## Purpose

XXL-JOB 调度平面：Compose Admin、进程内执行器与仓库种子真源。

## Requirements

### Requirement: XXL-JOB Admin 随 Compose 提供

系统 SHALL 在开发用 Docker Compose 中提供可启动的 XXL-JOB Admin 服务，使本地与自托管环境无需另找调度控制台即可使用定时任务平面。

#### Scenario: Compose 包含 Admin

- **WHEN** 开发者执行 `docker compose -f deploy/compose.yml config`
- **THEN** 配置中包含 `xxl-job-admin`（或文档约定的等价服务名）及其依赖的数据存储
- **AND** `.env.example` 列出 Admin 端口、账号、与执行器通信所需变量

#### Scenario: Admin 可访问

- **WHEN** 基础设施 Compose 已 `up` 且 Admin 健康
- **THEN** 运维可通过浏览器打开 Admin 登录页并使用文档给出的默认凭据登录（仅开发默认；生产须改密）

### Requirement: 业务进程内嵌执行器

`relayflow-server` SHALL 以内嵌 XXL-JOB Executor 方式接入调度中心；执行器应用名（appname）MUST 与仓库种子中的执行器组一致。

#### Scenario: 执行器注册

- **WHEN** `relayflow.job.enabled=true`（或等价开关）且 Admin 可达、server 已启动
- **THEN** Admin 执行器管理中可见约定 appname 的在线机器
- **AND** 业务 Handler 可通过 Admin「执行一次」被调度到该进程

### Requirement: 官方 Job 定义以仓库为真源

系统 MUST 将官方定时任务的定义（至少含：executor handler 名、Cron、任务描述、所属执行器组）版本化保存在仓库（如 `deploy/xxl-job/seed/`），并在部署初始化时写入 Admin 所用库，达到幂等可复现。

#### Scenario: 克隆后开箱有官方任务

- **WHEN** 新环境按 README 完成 Compose 初始化（含 XXL schema 与 seed）
- **THEN** Admin 任务列表中出现仓库种子声明的全部官方任务（含中国区划月同步任务）
- **AND** 无需阅读业务源码即可得知 handler 名与默认 Cron

#### Scenario: 新增官方任务走仓库变更

- **WHEN** 贡献者要新增一个官方定时任务
- **THEN** 必须同时提交：种子定义变更 + 业务侧同名 Handler
- **AND** 文档禁止将「仅在控制台手建任务」作为唯一交付方式

### Requirement: Job 与领域消息职责分离

定时调度 MUST 使用 XXL-JOB（或本规格所述 Job 平面）；跨域业务事实广播仍走领域消息约定。禁止用领域消息总线替代 Cron 触发平台维护类任务。

#### Scenario: 区划同步走 Job

- **WHEN** 需要按月同步中国行政区划
- **THEN** 由 XXL Job 触发 infra 同步逻辑
- **AND** 不依赖领域消息作为该月更的主触发器
