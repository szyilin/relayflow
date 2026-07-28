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

```bash
docker compose -f deploy/compose.yml run --rm xxl-job-db-init
```

## 版本

| 组件 | 版本 |
|------|------|
| `xxl-job-core`（执行器） | 3.3.2 |
| Admin 镜像 | znle/xxl-job-admin:3.3.2 |
| 存储 | 同机 Postgres / database `xxl_job` |
