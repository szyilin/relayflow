# DO / Mapper 代码生成约定

PostgreSQL 表结构以 **Flyway 迁移为唯一真源**。`relayflow-codegen` 的职责是：按需连库生成 **标准、未修剪** 的 MyBatis-Plus 三件套参照物，供人 / AI **与 `src/` 中已管理文件做 diff 后增量合并**。

> 行为规格见 `openspec/specs/`；本文描述 **工具怎么用、文件放哪、AI 怎么合**。

## 原则

| 项 | 规则 |
|----|------|
| 表结构真源 | `relayflow-server/src/main/resources/db/migration/*.sql` |
| 生成工具 | MyBatis-Plus `FastAutoGenerator`（`relayflow-tools/relayflow-codegen`） |
| 运行形态 | **独立 fat JAR**，不启动 `relayflow-server`、不解析 `application.yml` / Nacos |
| 触发方式 | **命令行指定表名**（`--tables`）；禁止在 Java 或 YAML 中硬编码表清单 |
| 生成物首站 | **临时目录**（默认 `.relayflow/codegen-out/<timestamp>/`），**仅作 diff 参照，不进 Git** |
| 仓库真源（提交） | `*-biz/src/` 下的 `*DO`、`*Mapper.java`、`*Mapper.xml` |
| 禁止 | `mvn compile` 自动全库生成；禁止把临时目录当最终真源；禁止整文件覆盖而丢掉手写 SQL |
| 允许手写 | 枚举、`*ExtMapper` / `*ExtMapper.xml`、等价的跨租户 PublicMapper、Service、Controller |

## 三件套（生成 · 进 Git）与自定义 SQL（手写 · 进 Git）

采用 **做法 2：拆分** —— 生成物与自定义 SQL **分文件**，互不覆盖。

| 类别 | 文件 | 位置（`*-biz`） | 来源 |
|------|------|-----------------|------|
| DO | `SysUserDO.java` | `src/main/java/.../dal/dataobject/` | codegen → diff 合并进 `src/` |
| 基础 Mapper | `SysUserMapper.java` | `src/main/java/.../dal/mapper/` | 同上（`BaseMapper<T>`） |
| 基础 XML | `SysUserMapper.xml` | `src/main/resources/mapper/` | 同上（resultMap / 列清单 / 基础 CRUD） |
| 自定义接口 | `SysUserExtMapper.java` | `src/main/java/.../dal/mapper/` | **手写**，进 Git |
| 自定义 XML | `SysUserExtMapper.xml` | `src/main/resources/mapper/` | **手写**，进 Git |

等价手写：跨租户等场景可用 `*PublicMapper.java`（注解 SQL 或独立 XML），规则与 ExtMapper 相同——**codegen diff 永不修改这些文件**。

### 目录布局

```text
relayflow-module-system-biz/
├── src/main/java/.../dal/
│   ├── dataobject/          # SysUserDO.java（Git 管理；由 codegen 参照合并）
│   └── mapper/
│       ├── SysUserMapper.java      # 基础 Mapper（Git）
│       └── SysUserExtMapper.java   # 自定义 SQL 接口（手写，Git）
├── src/main/resources/mapper/
│   ├── SysUserMapper.xml           # 基础 XML（Git；跟表结构）
│   └── SysUserExtMapper.xml        # 自定义 SQL（手写，Git；diff 不碰）

.relayflow/codegen-out/<ts>/         # CLI 临时参照物（不提交）
├── com/relayflow/.../dal/dataobject/
├── com/relayflow/.../dal/mapper/
└── mapper/                          # *Mapper.xml 参照
```

## 构建 CLI

```bash
./mvnw -Pcodegen package -DskipTests
# 产物：relayflow-tools/relayflow-codegen/target/relayflow-codegen.jar
```

日常入口 `./scripts/codegen.sh` 会在 JAR 不存在时自动构建。

## CLI 用法

```bash
./scripts/codegen.sh --help

# 只生成指定表（可多表，逗号分隔）→ 临时目录，供 diff
./scripts/codegen.sh --module system --tables sys_user,sys_dept

# 指定输出目录 + 生成前 Flyway migrate
./scripts/codegen.sh -m system -t sys_role,sys_permission --migrate -o /tmp/rf-codegen
```

### 参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `--module` / `-m` | 是 | `system` / `infra` / `im` / `task` / `calendar`（见 `codegen.yml`） |
| `--tables` / `-t` | 是 | 逗号分隔表名，**仅生成这些表** |
| `--output` / `-o` | 否 | 输出目录；默认 `.relayflow/codegen-out/<timestamp>/` |
| `--migrate` | 否 | 生成前对本地库执行 Flyway |
| `--config` | 否 | 自定义 YAML |
| `--repo-root` | 否 | 仓库根目录（脚本自动传入） |

### 临时目录产出（未修剪）

对每张表输出：

1. `*DO.java`（Lombok，继承 `BaseDO` / `TenantBaseDO`）
2. `*Mapper.java`（`@Mapper` + `BaseMapper`）
3. `*Mapper.xml`（标准 MP XML：resultMap、列清单等）

**不**生成 Service / Controller；**不**生成 ExtMapper。

## 推荐工作流（人 / AI 相同）

```text
1. 修改 Flyway 脚本
2. （如需）docker compose -f deploy/compose.yml up -d
3. ./scripts/codegen.sh -m <module> -t <变更涉及的表> [--migrate]
4. diff 临时目录 vs src/ 中对应三件套：
     # DO / Mapper.java
     diff -u .../src/main/java/.../dal/dataobject/SysUserDO.java \
              .relayflow/codegen-out/<ts>/.../SysUserDO.java || true
     # Mapper.xml
     diff -u .../src/main/resources/mapper/SysUserMapper.xml \
              .relayflow/codegen-out/<ts>/mapper/SysUserMapper.xml || true
5. 按「合并规则」增量更新 src/（见下节）
6. ./mvnw -pl relayflow-server -am compile
7. 提交：Flyway + src/ 下三件套变更 + 手写 Ext（如有）
   不提交：.relayflow/、target/
```

### 合并规则（强制）

| 场景 | 做法 |
|------|------|
| **已有表 · 增删改列** | 对照临时 DO / Mapper.java / Mapper.xml，在 `src/` 对应文件中同步字段、resultMap、列清单等与表结构相关部分 |
| **已有表 · ExtMapper** | **整文件不动**（含 `*ExtMapper.java` / `*ExtMapper.xml` / `*PublicMapper*`） |
| **新表** | 将临时目录三件套改好 package / namespace 后放入约定 `src/` 路径；无自定义 SQL 时可不建 Ext |
| **禁止** | `rsync` / 整文件覆盖临时 XML 到 `src/` 而丢掉任何手写内容；在生成的 `*Mapper.xml` 里堆业务 SQL |

自定义复杂 SQL → **只**写 ExtMapper（或等价 PublicMapper），**不要**写进生成的 `*Mapper.xml`。

## 配置（与 Spring 解耦）

CLI **只读** 下列配置，**不**加载 `application.yml`、不连 Nacos。

### 文件优先级

1. `--config <file>`
2. 仓库根 `codegen.local.yml`（gitignore，可复制 `codegen.local.yml.example`）
3. classpath `relayflow-tools/relayflow-codegen/src/main/resources/codegen.yml`

### JDBC 优先级

| 用途 | 环境变量（优先） | 备选 | YAML 键 |
|------|------------------|------|---------|
| URL | `RELAYFLOW_CODEGEN_JDBC_URL` | `SPRING_DATASOURCE_URL` | `jdbc.url` |
| 用户名 | `RELAYFLOW_CODEGEN_JDBC_USER` | `POSTGRES_USER` | `jdbc.username` |
| 密码 | `RELAYFLOW_CODEGEN_JDBC_PASSWORD` | `POSTGRES_PASSWORD` | `jdbc.password` |

### YAML 内容（非表清单）

- 各 module 的 `package-parent`、枚举包
- **enum-columns**：`表.列 → 枚举类名`（可选）
- **flyway.locations**
- **不含** 要生成的表名列表

含 `tenant_id` 列 → `TenantBaseDO`，否则 → `BaseDO`。

### PostgreSQL 类型映射（codegen 内置）

| DB 类型 | Java 类型 | 备注 |
|---------|-----------|------|
| `timestamptz` | `OffsetDateTime` | |
| `int2` / `smallint` | `Integer` | |
| `jsonb` | `String` | 自动加 `@TableField(..., typeHandler = JsonbTypeHandler.class)` |
| `enum-columns` | 枚举类 | |

## 生成 vs 手写（对照）

| 生成（CLI 参照 → 合入 `src/`） | 手写（仅 `src/`） |
|-------------------------------|-------------------|
| `*DO.java` | `{Entity}ExtMapper.java` |
| `*Mapper.java`（BaseMapper） | `{Entity}ExtMapper.xml` |
| `*Mapper.xml`（基础 CRUD / resultMap） | `*PublicMapper`（跨租户等） |
| | `enums/*`、Service、Controller |

## AI 编码代理规则

1. 表结构变更 → **只改 Flyway**，再 CLI 指定表生成到临时目录。
2. **禁止**脱离 codegen、在 `src/` 从零手写整份 DO 字段列表；**必须**以临时参照为准做增量合并。
3. 合并前必须 **diff** 临时输出与 `src/` 已有三件套（新表则复制落位）。
4. 自定义 SQL → **仅** ExtMapper（或等价 PublicMapper）；**禁止**写入生成的 `*Mapper.xml`。
5. **禁止**修改、覆盖或删除 `*ExtMapper*` / 业务 PublicMapper 来「对齐」生成物。
6. 手写 DTO / VO / Properties → Lombok（见 [code-style.md](code-style.md)）。

## 与工程迁移的关系

- **文档与工具约定（当前真源）**：三件套位于 `src/`，进 Git；codegen 只产出临时参照。
- **若仓库仍见 `target/generated-sources/mybatis/`**：属过渡残留，以本文为准，勿再往 target 合并；工程目录迁入 `src/` 为独立步骤。

## OpenSpec change（历史）

| Change | 内容 |
|--------|------|
| `scaffold-system-codegen` | CLI fat JAR、`-Pcodegen`、`scripts/codegen.sh` |
| `system-schema-v1` 等 | Flyway + 枚举；早期曾合并至 `target/`，已由本文纠正 |

## 参考

- [MyBatis-Plus 代码生成器](https://baomidou.com/guides/new-code-generator/)
- [database.md](database.md)、[code-style.md](code-style.md)
