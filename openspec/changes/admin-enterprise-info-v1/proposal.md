## Why

管理后台缺少飞书式「企业信息 / 企业地址」能力；同时仓库尚无统一的定时任务平面。开源自托管场景下，若 Job 只存在于 XXL-JOB 控制台手工配置，克隆仓库的人无法复现「该跑哪些任务」。需要把 **XXL-JOB 调度平台** 与 **企业信息纵向切片** 一次规划清楚，并约定 Job 定义以仓库为真源。

## What Changes

- **[平台] 引入 XXL-JOB**：`deploy/compose.yml` 增加独立 `xxl-job-admin`；`relayflow-server` 内嵌执行器；framework 增加 `relayflow-spring-boot-starter-job` 收口配置。
- **Job 定义入库可复现**：官方任务的 Cron / handler / 描述以仓库内版本化 SQL（或等价种子）维护；`compose up` 后开箱具备默认任务，**禁止**要求使用者翻业务代码猜要在控制台配什么。控制台仍可用于运维启停与手动触发。
- **中国行政区划平台数据**：`infra_geo_region`（平台表，非租户）；V1 仅中国大陆四级（国家/地区节点 → 省 → 市 → 区县）；月更由 XXL Job 触发；首次空库 bootstrap。
- **管理端「企业信息」**：路由 `/admin/system/enterprise`（或等价），两 Tab——企业信息管理、企业地址管理。
  - **不做**：企业域名、注销/解散企业、地图选点/POI、国外行政区深度。
  - **企业编号**：沿用创建时生成的 `sys_tenant.code`，只读不可改。
  - **可编辑**：企业名、行业、规模、联系人等；注册已有字段自动回填。
  - **地址**：类型多选（职位地址 / 面试地址 / 门店地址）、中国级联、详细地址文本；启停与编辑。
- 交付顺序遵循前端优先：平台 Job/geo 可先行；企业信息 UI → API → 联调。

## Capabilities

### New Capabilities

- `infra-job`：XXL-JOB 嵌入、Compose 编排、执行器接入、**仓库内 Job 种子真源**与 Handler 约定
- `infra-geo`：中国行政区划入库、级联查询 API、月同步 Job 与手动触发
- `admin-enterprise-info`：管理端企业信息 / 企业地址的产品行为与 API 契约（含权限码）

### Modified Capabilities

- `deployment`：开发 Compose 增加 `xxl-job-admin` 及关联库/卷/健康检查；文档说明 Job 种子路径
- `system`：扩展租户主数据字段（行业、规模、联系人等）；企业编号只读语义与注册回填
- `web-admin`：侧栏增加企业信息入口；页面与权限门禁
- `infra`：平台级 geo 表前缀与跨域只读 API 边界（若与现有 infra 需求交叉则 delta）

## Impact

| 区域 | 影响 |
|------|------|
| `deploy/` | Compose、`.env.example`、`deploy/xxl-job/`  schema + job 种子 |
| `relayflow-dependencies` / `relayflow-framework` | XXL-JOB 版本入 BOM；新增 job starter |
| `relayflow-module-infra` | geo 表、同步 Service、级联 API、XXL Handler |
| `relayflow-module-system` | `sys_tenant` 扩列、企业信息/地址 CRUD、权限种子 |
| `relayflow-server` | 执行器启用、配置项 |
| `web/` | 管理端页面、Store、`api/admin`、导航与 permission |
| 自部署 | 多一个 Admin 容器与（建议）独立 DB/schema；回滚：停用容器 + 关闭执行器配置即可；业务 Flyway 按序回滚 |
| 依赖 | 新增 `xxl-job-core`（须兼容 Spring Boot 3.4）；**不**引入 Gateway/Nacos |

**非目标（本 change）**：PowerJob、自研扫表调度中台、企业认证流程、数据驻留多地选择、全球 geo、领域消息替代 Cron。
