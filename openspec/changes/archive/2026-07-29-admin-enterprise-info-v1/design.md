## Context

管理端尚无「企业信息 / 企业地址」；`sys_tenant` 仅有 `code/name/status/owner_user_id`（注册已生成 `code`）。仓库无定时任务平台；领域消息与 Cron 已约定分离。产品对标飞书企业管理后台截图，但 V1 砍掉域名、注销企业、国外 geo、地图选点。开源自托管要求：**默认 Job 必须能从仓库复现**，不能只活在 XXL 控制台里。

约束：V1 单 `relayflow-server`；Compose 已有 PG/Redis/MinIO；Spring Boot 3.4；前端优先纵向切片；跨域禁止 `*-biz → *-biz`。

## Goals / Non-Goals

**Goals:**

1. Compose 天然带上 XXL-JOB Admin；业务进程内嵌执行器。
2. 官方 Job（含中国区划月更）以仓库种子为真源，clone + compose 后无需翻代码猜配置。
3. 中国四级行政区划入库 + 级联 API + 月同步。
4. 管理端企业信息双 Tab（信息 + 地址），注册可回填字段自动带出；企业编号只读。

**Non-Goals:**

- PowerJob / 自研扫表调度中台 / 用领域消息替代 Cron
- 企业域名、解散企业、企业认证闭环、数据驻留多地
- 全球 geo、乡镇街道、地图 POI
- Phase 2 多执行器分组细化（预留 appname 即可）

## Decisions

### D1. 调度框架选用 XXL-JOB（Admin 独立 + Executor 内嵌）

- **选择**：XXL-JOB；Admin 独立容器；`relayflow-server` 嵌 Executor。
- **相对**：PowerJob（能力更强但 V1 过重）；Quartz 纯内嵌（无统一控制台、不符合「平台约定」）；自研扫表（重复造轮子）。
- **落点**：BOM 管版本；`relayflow-spring-boot-starter-job` 自动配置；业务只写 `@XxlJob` Handler（或薄接口），禁止散落硬编码 Admin URL 业务逻辑。

### D2. Job 定义「仓库真源」而非「只在控制台手配」

```text
deploy/xxl-job/
  schema/          # 官方表结构（适配 PG 或沿用官方 MySQL 镜像策略，见 D3）
  seed/
    01_executor.sql    # 执行器组 appname = relayflow-server
    02_jobs.sql        # 所有官方任务：handler、cron、描述、路由、报警邮件空
  README.md            # 如何新增 Job：先改种子 + 写 Handler，再可选在 UI 调参
```

- **约定**：新增官方任务 = **PR 改 seed SQL + 代码 Handler 同名**；控制台可改 Cron/启停，但升级/重装以种子为准（幂等 upsert：按 `executor_handler` 或约定 job 标识合并）。
- **禁止**：README 写「请到 XXL 控制台手动新建 xxx」作为唯一交付路径。
- **手动触发**：Admin UI「执行一次」+（可选）管理端「立即同步」调同一 Service，避免双逻辑。

### D3. Admin 数据存储

- **选定**：与业务 **共用同一 Postgres 实例**，独立 database `xxl_job`（不混入业务库 `relayflow`）。
- Admin 镜像：`znle/xxl-job-admin:3.3.2`（原生 PG + **linux/amd64 与 linux/arm64**），替代官方仅 amd64 / MySQL 镜像。
- 初始化：Compose oneshot `xxl-job-db-init` 执行 `deploy/xxl-job/init-db.sh`（建库 + schema + 官方 job 种子，幂等）。
- **不做**：旁路 MySQL 容器。

### D4. 中国 Geo：平台表 + 公开源导入 + 月 Job

| 项 | 决定 |
|----|------|
| 表 | `infra_geo_region`（无 `tenant_id`；平台公共数据） |
| 层级 | V1：`country_region` → `province` → `city` → `district`（中国大陆） |
| 源 | 民政部地名库/公开数据集 release（如 province-city-china / dmfw 同步包）；导入脚本记 `source` + `data_version` |
| 同步 | XXL Job `infraGeoSyncChina`，Cron **每月 1 日**（如 `0 0 3 1 * ?`）；幂等 upsert by `code` |
| 首次 | 应用启动或首次调用级联时若空表则 bootstrap（或 compose 后手动/Job 执行一次） |
| API | `GET /admin-api/infra/geo/children?parentId=`（懒加载）；可选按 code 反查路径 |

system 地址表只存 `region_id` + 路径快照文本 + `detail`，不跨域写 geo。

### D5. 企业信息数据模型（system）

扩展 `sys_tenant`（或 `sys_tenant_profile` 1:1——若列过多再拆；V1 优先扩列以简单）：

| 字段 | 语义 |
|------|------|
| `code` | 企业编号，创建时生成，**不可更新** |
| `name` | 可编辑 |
| `industry` / `scale` | 枚举或字典 code |
| `contact_name` / `contact_email` / `contact_mobile` / `contact_landline` | 联系人 |
| （可选）`logo_file_id` | 后置可做 |
| 不做 | `domain`、注销状态机 |

`sys_tenant_address`（租户级）：

- `types`：多选，存 JSON 数组或关联表；枚举 `JOB` / `INTERVIEW` / `STORE`
- `region_id`、`region_path`（展示快照）、`detail`、`status`（启用/停用）

注册回填：创建人昵称/手机/邮箱能填的写入联系人默认值；行业规模空着待编辑。

### D6. 前端与权限

- 路由：`/admin/system/enterprise`；侧栏「企业设置」或挂在系统管理下「企业信息」。
- UI：对标截图——基础信息卡 + 更多信息（只读/编辑切换）+ 地址 Tab 列表/筛选/弹窗级联。
- Permission：`system:enterprise:query` / `system:enterprise:update`；地址可共用或拆 `system:enterprise-address:*`。超级管理员模板种子补齐。
- 顺序：**[平台] Job+geo 可与 -web 并行起步**；企业信息严格 `-web`（Mock/store）→ `-api` → `-integrate`。contract 落 `openspec/lanes/admin-enterprise-info/contract.md`。

### D7. 模块边界

```text
xxl-job-admin (compose)
       │ 调度
relayflow-server (Executor)
       ├─ starter-job
       ├─ infra-biz: GeoSyncJob → GeoSyncService → infra_geo_region
       └─ system-biz: Enterprise* API；读 geo 经 infra-api（GeoRegionApi）
```

禁止 system-biz 直接 Mapper geo 表。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| XXL 官方偏 MySQL，PG 脚本不全 | D3 选定方案；种子用幂等 upsert；CI/`compose config` 验证 |
| 控制台改 Cron 与种子漂移 | 文档：官方任务以种子为准；重大变更走 PR；可选启动校验告警 |
| 区划数据包体积 / 首次导入慢 | 懒加载 API；同步异步跑 Job；不塞前端包 |
| 源站不可达（内网） | 仓库可 vendoring 一版 JSON/SQL 快照作离线 bootstrap |
| 单 change 范围大 | tasks 分阶段；实现会话按阶段勾选，不一次做完 |

## Migration Plan

1. Compose 增加 Admin + DB；`.env.example` 增加 `XXL_JOB_*`、执行器 `appname`。
2. 业务 Flyway：geo 表、tenant 扩列、address 表、permission 种子。
3. 部署 XXL schema + seed；启动 server 注册执行器；手动跑一次 geo sync。
4. 前端上线企业信息页。
5. **回滚**：停 Admin 容器、关 `relayflow.job.enabled`；业务迁移按 Flyway 逆向（开发期可破坏性，须征得同意）。

## Open Questions

1. ~~XXL Admin 用同 PG 新 database 还是旁路 MySQL？~~ → **已选定同 Postgres 实例 + 独立库 `xxl_job` + arm64 友好 Admin 镜像**（见 D3）。
2. 企业 Logo / 认证入口是否在本 change 只做展示占位？（默认：占位或不做，不挡主路径。）
3. 「数据驻留地」是否只读展示「中国大陆」？（默认：只读展示，不可改。）
