# XXL-JOB（RelayFlow 官方调度平面）

调度中心与业务进程分离：Admin 跑在 Compose 容器里，`relayflow-server` 内嵌执行器。  
**官方任务定义以 `seed/` 为真源**（PostgreSQL 方言），clone 后 `compose up` 即可看到默认任务。

存储：**与业务共用同一 Postgres 实例**，独立 database `xxl_job`（不写进业务库 `relayflow`）。

## 布局

| 路径 | 说明 |
|------|------|
| `schema/01_tables_xxl_job.sql` | XXL-JOB 3.3.x 表结构（PG）+ 默认 Admin 账号 |
| `seed/01_executor.sql` | 执行器组 `appname=relayflow-server` |
| `seed/02_jobs.sql` | 全部官方定时任务（handler / Cron / 描述） |
| `init-db.sh` | Compose oneshot：建库 + 灌 schema/seed（幂等） |
| `init-local.sh` / `init-local.sql` | 完整本地栈专用：仅首次建表 / 种子，事务提交后记录完成状态 |

## 访问

| 项 | 开发默认 |
|----|----------|
| Admin UI | http://localhost:8090/xxl-job-admin |
| 账号 / 密码 | `admin` / `123456`（生产务必修改） |
| 执行器 AppName | `relayflow-server` |
| accessToken | 与 `.env` 中 `XXL_JOB_ACCESS_TOKEN` 一致（Compose 通过 `command --xxl.job.accessToken` + `XXL_JOB_ACCESSTOKEN` 注入；**勿依赖 PARAMS**，znle 入口未消费该变量） |
| Admin 镜像 | `znle/xxl-job-admin:3.3.2`（**linux/amd64 + linux/arm64**，原生 PG） |

## 新增官方 Job（必走仓库）

1. 在 `seed/02_jobs.sql` 增加幂等 INSERT/UPDATE（按 `executor_handler`）。
2. 在对应 `*-biz` 编写同名 `@XxlJob("handlerName")` Handler。
3. 提交 PR。控制台可临时改 Cron / 启停，但升级重装以种子为准。

**禁止**把「仅在 Admin 手建任务」作为唯一交付方式。

别人若自备 Admin（甚至用 MySQL），仍可对照 `seed/02_jobs.sql` 里的 **handler 名、Cron、描述** 自行建任务——种子是契约，不强制对方必须用本 Compose 的 Admin。

## 宿主机跑 server + Docker 跑 Admin

Admin 回调执行器时，用 **address**（不要填宿主机无法解析的 hostname 到 `ip`）：

```bash
RELAYFLOW_JOB_EXECUTOR_ADDRESS=http://host.docker.internal:9999/
RELAYFLOW_JOB_EXECUTOR_IP=
```

（Linux Compose 已通过 `extra_hosts: host.docker.internal:host-gateway` 让 Admin 容器解析该主机名。）

## 重新灌种子

### 完整本地栈的首次初始化

`init-local.sh` 供 `compose.local.yml` 一次性服务调用，挂载本目录为 `/xxl-job`，由 PostgreSQL 16 客户端运行。必填 `POSTGRES_USER` / `POSTGRES_PASSWORD`，使用 `POSTGRES_HOST`（默认 `postgres`）、`POSTGRES_PORT`（默认 `5432`）、`XXL_JOB_DB_NAME`（默认 `xxl_job`），可设置 `LOCAL_INIT_TIMEOUT_SECONDS`（默认 `60`）。调度库不得与业务库或系统模板库同名。

local Admin 同时设置 `XXL_JOB_DB_TYPE=postgres`，因为镜像入口根据该变量传入命令行 profile，优先于 `SPRING_PROFILES_ACTIVE`；并关闭 Hibernate DDL 与 Spring SQL 自动初始化，避免启动时运行另一套 schema / seeds。首次初始化的所有权只属于 `xxl-job-db-init`。

同库首次初始化使用数据库 advisory lock 串行执行；schema、官方 seeds 与 `public.relayflow_local_bootstrap` 完成标记同事务提交。失败会回滚，本次创建的空数据库可在修复后直接重试。已完成时不再执行 seeds，保留用户修改的 Cron、启停、任务参数、执行器地址、账号和自定义任务；停止 / 重建容器不改变这一行为。

遇到没有 local 标记的既有调度表、未知版本标记或标记存在但必需表丢失时，明确失败，不自动接管、补表或重新灌种子。不要删除标记来强迫重灌；应先备份并由运维明确选择恢复或维护方案。

### 开发入口 / 显式维护

原 `init-db.sh` 与以下命令仍会应用官方 seeds，其中 UPDATE 会重设官方任务的 Cron / 启停及执行器设置。它是开发或显式维护入口，不是完整本地栈的日常启动行为；需要重灌时先备份并评估这些覆盖。

```bash
docker compose -f deploy/compose.yml run --rm xxl-job-db-init
```

## 版本

| 组件 | 版本 |
|------|------|
| `xxl-job-core`（执行器） | 3.3.2 |
| Admin 镜像 | znle/xxl-job-admin:3.3.2 |
| 存储 | 同机 Postgres / database `xxl_job` |
