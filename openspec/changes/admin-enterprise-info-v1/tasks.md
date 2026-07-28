# Tasks: admin-enterprise-info-v1

> 实现时按阶段推进；单次 apply 会话建议只勾选 **一个阶段**（约 ≤10 条），勿一次做完整 change。

## 1. [平台] XXL-JOB Compose 与 Job 种子

- [x] 1.1 在 BOM 增加兼容 Spring Boot 3.4 的 `xxl-job-core` 版本；新增 `relayflow-spring-boot-starter-job`（执行器自动配置、开关、appname）
- [x] 1.2 `deploy/compose.yml` / `.env.example` 增加 `xxl-job-admin` 及依赖库（同 PG 新 database 或旁路 MySQL，按 design D3 选定一条并写死）
- [x] 1.3 落盘 `deploy/xxl-job/`：schema + seed（执行器组 appname=`relayflow-server`）+ README（如何新增官方 Job）
- [x] 1.4 seed 声明至少一条占位或真实官方任务；幂等可重复执行；`docker compose … config` 通过
- [x] 1.5 `relayflow-server` 启用 starter；本地联调：Admin 可见执行器在线；文档补充默认账号与端口

## 2. [平台] 中国区划入库与月同步 Job

- [x] 2.1 Flyway 创建 `infra_geo_region`（及同步元数据表若需要）；标明平台共享、无 `tenant_id`；codegen → diff 合入
- [x] 2.2 实现 `GeoSyncService`（公开源/vendored 快照导入 + 按 code 幂等 upsert）与空库 bootstrap
- [x] 2.3 注册 XXL Handler `infraGeoSyncChina`；在 `deploy/xxl-job/seed` 增加月 Cron 任务定义
- [x] 2.4 暴露 `GET /admin-api/infra/geo/children`；`infra-api` 增加只读 `GeoRegionApi` 供 system 使用
- [x] 2.5 手动/Job 跑通一次同步；验证四级级联数据可用

## 3. 企业信息 — 前端（-web）

- [x] 3.1 `useAdminNav` 增加企业信息入口与 permission；页面 `/admin/system/enterprise`（双 Tab：信息 / 地址）
- [x] 3.2 企业信息 Tab UI：基础信息、更多信息（无域名）、联系人；企业编号只读；无注销企业；编辑/保存交互
- [x] 3.3 地址 Tab UI：筛选、列表、新增/编辑弹窗（类型多选三选项、中国级联、详细地址）、启停
- [x] 3.4 Pinia store + `api/admin` 草案；API 未就绪时 store 内临时数据撑 UI
- [x] 3.5 起草 `openspec/lanes/admin-enterprise-info/contract.md`；更新 `api-integration-board.md`；`pnpm build` + `pnpm typecheck`

## 4. 企业信息 — 后端（-api）

- [ ] 4.1 Flyway：`sys_tenant` 扩档案字段；`sys_tenant_address`；permission 种子；codegen → diff 合入
- [ ] 4.2 注册创建企业时回填可获得的联系人字段；保证 `code` 更新路径拒绝修改
- [ ] 4.3 按 contract 实现企业信息 GET/PUT；地址 CRUD/启停；经 `GeoRegionApi` 校验 region
- [ ] 4.4 `@PreAuthorize` 与超级管理员模板对齐；curl / `.relayflow/api-tests` 验收

## 5. 联调与收尾（-integrate）

- [ ] 5.1 前端去掉该切片临时 Mock；对接真实 API 与 geo 级联
- [ ] 5.2 端到端：Compose 起 Admin → server 执行器 → 月更 Job 可手动触发；企业信息/地址页可读写
- [ ] 5.3 `openspec validate admin-enterprise-info-v1 --strict`；看板标记 done；README/AGENTS 如需补充 Job 约定则短更
