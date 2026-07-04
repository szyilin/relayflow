# 设计：CLI 按需 DO/Mapper 生成

## 工作流

```text
Flyway 迁移（真源）
  → （可选）--migrate 同步到本地 PG
  → CLI：--module system --tables sys_user,sys_dept -o .relayflow/codegen-out/...
  → diff 对比 target/generated-sources/mybatis/ 或上次产物
  → 确认后复制到 module 的 target/generated-sources/mybatis/
  → mvn compile
```

## CLI 参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `--module` / `-m` | 是 | 模块键：`system` / `infra` / `im`（见 `codegen.yml`） |
| `--tables` / `-t` | 是 | 逗号分隔表名，**仅生成所列的表** |
| `--output` / `-o` | 否 | 输出目录；默认 `.relayflow/codegen-out/<timestamp>/` |
| `--migrate` | 否 | 生成前对本地库执行 Flyway |
| `--config` | 否 | 自定义 `codegen.yml` 路径 |

## 超类选择（无硬编码表名单）

连接数据库查询 `information_schema`：若表含 `tenant_id` 列 → `TenantBaseDO`，否则 → `BaseDO`。

## 配置外置（codegen.yml / codegen.local.yml）

- JDBC 默认值（可被 `RELAYFLOW_CODEGEN_*` 或 `POSTGRES_*` / `SPRING_DATASOURCE_URL` 环境变量覆盖）
- **不**读取 `application.yml`、不连 Nacos
- 配置优先级：`--config` → 仓库根 `codegen.local.yml` → classpath `codegen.yml`
- `modules.*.package-parent` 等包路径
- `enum-columns`：`表.列 → 枚举简单类名`（可选）

## 构建

- Maven profile `-Pcodegen`：仅 `relayflow-dependencies` + `relayflow-tools`
- `maven-shade-plugin` 产出 `relayflow-codegen.jar`，`scripts/codegen.sh` 以 `java -jar` 调用

## 与 system-schema-v1 分工

| 项 | system-schema-v1 | scaffold-system-codegen |
|----|------------------|-------------------------|
| Flyway | ✓ | |
| 手写枚举 | ✓ | |
| DO/Mapper | 用 CLI 生成后合并 | ✓ 提供 CLI |
