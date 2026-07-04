# DO / Mapper 代码生成约定

PostgreSQL 表结构以 **Flyway 迁移为唯一真源**。DO 与基础 Mapper **禁止 AI 或人工在 `src/` 手写字段列表**，须通过 **CLI 按需连库生成**。

> 行为规格见 `openspec/specs/` 与 active change；本文描述 **如何构建、配置、生成、diff、合并**。

## 原则

| 项 | 规则 |
|----|------|
| 表结构真源 | `relayflow-server/src/main/resources/db/migration/*.sql` |
| 生成工具 | MyBatis-Plus `FastAutoGenerator`（`relayflow-tools/relayflow-codegen`） |
| 运行形态 | **独立 fat JAR**，不启动 `relayflow-server`、不解析 `application.yml` / Nacos |
| 触发方式 | **命令行指定表名**（`--tables`）；禁止在 Java 或 YAML 中硬编码表清单 |
| 生成物首站 | **临时目录**（默认 `.relayflow/codegen-out/<timestamp>/`），不进 Git |
| 合并目标 | `relayflow-module-*-biz/target/generated-sources/mybatis/`（diff 后复制） |
| 禁止 | `mvn compile` 自动全库生成；禁止直接改临时/生成目录后当最终真源 |
| 允许手写 | 枚举、`ExtMapper`、ExtMapper XML、Service、Controller |

## 构建 CLI

仅构建 codegen 相关模块（跳过 server / *-biz，较快）：

```bash
./mvnw -Pcodegen package -DskipTests
# 产物：relayflow-tools/relayflow-codegen/target/relayflow-codegen.jar
```

日常入口 `./scripts/codegen.sh` 会在 JAR 不存在时自动执行上述命令。

## CLI 用法

```bash
# 帮助
./scripts/codegen.sh --help

# 只生成指定表（可多表，逗号分隔）
./scripts/codegen.sh --module system --tables sys_user,sys_dept

# 指定输出目录 + 生成前 Flyway migrate
./scripts/codegen.sh -m system -t sys_role,sys_permission --migrate -o /tmp/rf-codegen

# 等价：直接 java -jar
java -jar relayflow-tools/relayflow-codegen/target/relayflow-codegen.jar \
  --repo-root "$PWD" \
  --module system --tables sys_user -o /tmp/rf-out
```

### 参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `--module` / `-m` | 是 | `system` / `infra` / `im`（包名见 `codegen.yml`） |
| `--tables` / `-t` | 是 | 逗号分隔表名，**仅生成这些表** |
| `--output` / `-o` | 否 | 输出目录；默认 `.relayflow/codegen-out/<timestamp>/` |
| `--migrate` | 否 | 生成前对本地库执行 Flyway |
| `--config` | 否 | 自定义 YAML 路径（见下方配置优先级） |
| `--repo-root` | 否 | 仓库根目录（脚本自动传入） |

## 配置（与 Spring 解耦）

CLI **只读** 以下配置，**不** 加载 `application.yml`、不连 Nacos。

### 文件优先级

1. `--config <file>`（若指定）
2. 仓库根目录 `codegen.local.yml`（gitignore，可复制 `codegen.local.yml.example`）
3. classpath `relayflow-tools/relayflow-codegen/src/main/resources/codegen.yml`

### JDBC 优先级

| 用途 | 环境变量（优先） | 备选环境变量 | YAML 键 |
|------|------------------|--------------|---------|
| URL | `RELAYFLOW_CODEGEN_JDBC_URL` | `SPRING_DATASOURCE_URL` | `jdbc.url` |
| 用户名 | `RELAYFLOW_CODEGEN_JDBC_USER` | `POSTGRES_USER` | `jdbc.username` |
| 密码 | `RELAYFLOW_CODEGEN_JDBC_PASSWORD` | `POSTGRES_PASSWORD` | `jdbc.password` |

与 [deploy/.env.example](../../deploy/.env.example) 对齐：本地 `docker compose up -d` 后，导出 `POSTGRES_*` / `SPRING_DATASOURCE_URL` 即可供 CLI 与 server 共用，**变量名相同、读取方式独立**。

### YAML 内容（非表清单）

`codegen.yml` 仅包含：

- 各 **module** 的 `package-parent`、枚举包
- **enum-columns**：`表.列 → 枚举类名`（可选）
- **flyway.locations**：相对仓库根的迁移目录
- **不含** 要生成的表名列表

`BaseDO` / `TenantBaseDO` 由工具查询 `information_schema`：表含 `tenant_id` 列 → `TenantBaseDO`，否则 → `BaseDO`。

## 目录布局

```text
relayflow-module-system-biz/
├── src/main/java/.../dal/mysql/     # 手写 SysUserExtMapper.java
├── src/main/resources/mapper/       # 手写 Ext XML
└── target/generated-sources/mybatis/  # CLI diff 合并后的 DO/Mapper（不提交 Git）

.relayflow/codegen-out/<ts>/         # CLI 临时输出（不提交 Git）
```

## 推荐工作流（人 / AI 相同）

```text
1. 修改 Flyway 脚本
2. （可选）docker compose -f deploy/compose.yml --env-file deploy/.env up -d
3. ./scripts/codegen.sh -m system -t <变更涉及的表> [--migrate]
4. diff 临时目录 vs target/generated-sources/mybatis/
     diff -ru relayflow-module-system-biz/target/generated-sources/mybatis \
              .relayflow/codegen-out/<ts> || true
5. 确认后合并：
     rsync -av .relayflow/codegen-out/<ts>/ \
       relayflow-module-system-biz/target/generated-sources/mybatis/
6. 首次合并后，在 *-biz/pom.xml 增加 compile 支持（见下节）
7. ./mvnw -pl relayflow-server -am compile
8. 提交：Flyway + 手写代码；不提交 target/ 与 .relayflow/
```

### 合并后 *-biz 编译配置

生成 DO 使用 Lombok；合并至 `target/generated-sources/mybatis/` 后，需在对应 `*-biz/pom.xml` 增加：

- `lombok` 依赖（`provided`）
- `build-helper-maven-plugin` 将 `target/generated-sources/mybatis` 加入 compile source root

在首次 CLI 合并前，`*-biz` 保持精简 pom（无上述插件），避免空目录编译挂钩。

## 生成 vs 手写

| 生成（CLI） | 手写 |
|-------------|------|
| `*DO.java`（Lombok） | `{Entity}ExtMapper` |
| `*Mapper.java`（BaseMapper） | `*ExtMapper.xml` |
| | `enums/*` |

## AI 编码代理规则

1. 表结构变更 → **只改 Flyway**，再 **CLI 指定表** 生成到临时目录。
2. **不得**在 `src/` 创建 `*DO` 或基础 `*Mapper`。
3. 合并前必须 **diff** 临时输出与 `target/generated-sources/mybatis/`。
4. 自定义 SQL → 仅 `ExtMapper` + XML。

## OpenSpec change

| Change | 内容 |
|--------|------|
| `scaffold-system-codegen` | CLI fat JAR、`-Pcodegen`、`scripts/codegen.sh`、`codegen.yml` |
| `system-schema-v1` | Flyway + 枚举；DO/Mapper 经 CLI 合并 |

## 后续（非 V1）

- 管理端「选表生成」UI（infra 域，类似芋道/JeecgBoot）
- CI：migrate + 对指定表 codegen + diff 检查

## 参考

- [MyBatis-Plus 代码生成器](https://baomidou.com/guides/new-code-generator/)
- [database.md](database.md)、[code-style.md](code-style.md)
