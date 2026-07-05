# 任务：scaffold-system-codegen

## 1. CLI 工具

- [x] 1.1 `RelayflowCodegenCli`：解析 `--module`、`--tables`、`--output`、`--migrate`
- [x] 1.2 外置 `codegen.yml`（模块包名、枚举列映射；**不含表清单**）
- [x] 1.3 按表查询 `tenant_id` 自动选择 `BaseDO` / `TenantBaseDO`
- [x] 1.4 输出至临时目录（默认 `.relayflow/codegen-out/`）
- [x] 1.5 `maven-shade-plugin` 打包 fat JAR；根 `pom.xml` 增加 `-Pcodegen` profile
- [x] 1.6 `scripts/codegen.sh`：`java -jar` 入口（JAR 缺失时自动 `-Pcodegen package`）
- [x] 1.7 支持仓库根 `codegen.local.yml`（`codegen.local.yml.example` + gitignore）

## 2. 清理

- [x] 2.1 删除 `SystemSchemaCodegen` 及硬编码表常量
- [x] 2.2 移除 `system-biz` 的 `generate-sources` exec / build-helper / lombok（合并 DO 后再加回）
- [x] 2.3 回滚 `src/` 下手写 `*DO`、基础 `*Mapper`

## 3. 文档

- [x] 3.1 更新 `docs/dev/codegen.md`（fat JAR、`-Pcodegen`、配置优先级、合并后 pom）
- [x] 3.2 更新 `AGENTS.md` / `implementation-workflow.mdc` / `deploy/.env.example`

## 4. 验证

- [x] 4.1 `./mvnw -Pcodegen package -DskipTests`
- [x] 4.2 `./scripts/codegen.sh --help`
- [x] 4.3 （可选）对 `sys_user` 试跑并检查临时目录产物（需本地 PostgreSQL）

## 5. 衔接 system-schema-v1

- [x] 5.1 用 CLI 生成 system 域表 DO/Mapper 到临时目录
- [x] 5.2 diff 后合并至 `system-biz/target/generated-sources/mybatis/`
- [x] 5.3 `./mvnw -pl relayflow-server -am compile`
