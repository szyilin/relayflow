---
name: flyway-squash-migrations
description: >-
  Squash RelayFlow Flyway SQL migrations in a user-specified version range into
  one kept script (DDL first, then required DML), delete absorbed files, and
  realign flyway_schema_history (delete absorbed rows + repair kept checksum).
  Explicit-only: invoke via /flyway-squash-migrations. Never auto-apply.
disable-model-invocation: true
---

# Flyway 迁移局部合并（RelayFlow）

**仅当用户通过 `/flyway-squash-migrations`（或等价显式点名）调用本 skill 时执行。**  
日常对话、改业务代码、写新迁移时 **禁止** 主动合并或改写历史 SQL。

**禁止**：改 `docs/dev/database.md`、自动 git commit、清空业务库、扩大用户未指定的版本范围。

## 何时使用

用户明确要求：把若干已存在的 `V0.x.y.z__*.sql` **局部合并**到某一个保留版本，并同步本地已执行过的 `flyway_schema_history`。

## 硬前置

1. 用户必须给出合并范围，例如：`把 V0.1.0.1～V0.1.0.3 收到 V0.1.0.3`（可点名文件列表）。
2. **禁止**自选「合到现在最新」或擅自纳入范围外脚本。
3. 迁移目录固定：
   `relayflow-server/src/main/resources/db/migration/`
4. 文件名纪律仍遵守：`V{major}.{minor}.{patch}.{seq}__{snake_desc}.sql`（见仓库约定；本 skill 不改产品 SemVer）。

## 工作流（必须按序）

复制并维护清单：

```text
Squash Progress:
- [ ] 1. 解析用户指定的吸收版本 + 保留版本
- [ ] 2. 阅读范围内全部 SQL，推导终态（DDL）与仍需 DML
- [ ] 3. 起草合并后保留文件内容（先不写盘也可先展示）
- [ ] 4. 解析将连接的数据库，列出拟删文件 / 拟删 history 行 / 拟 repair 版本
- [ ] 5. **停下来等用户确认**（未确认不得改文件、不得动库）
- [ ] 6. 写保留文件、删除被吸收文件
- [ ] 7. 用户确认后的 DB：删吸收版本 history + repair 保留版本 checksum
- [ ] 8. 校验（不做空库全量 migrate）
```

### 1. 范围解析

- **吸收版本**：合并后从磁盘删除的脚本（除保留版外）。
- **保留版本**：唯一留下的脚本；内容替换为合并结果；版本号默认用用户指定的保留号，**不要擅自改号**。
- 范围外文件（更早或更晚）**一字不动**。

### 2. 合并内容规则（硬规则 + 建议）

**硬规则**

- 保留文件结构：**先 DDL，后仍需执行的 DML**；用简短中文注释说明块用途（为何建表/种子仍需要）。
- 中间态收敛：可并入终态 `CREATE TABLE` 的 `ALTER`（加列/改类型/加约束/加索引）直接写进建表；已被后续废弃的列/表/索引 **不要** 出现在合并结果。
- 已被后续脚本删掉且不再使用的对象：合并结果中 **不创建**（净效果 = 从未存在），不要先 `CREATE` 再 `DROP`。
- 已废弃且不再需要的 seed / `INSERT`：**删除**，不要留「历史曾插入过」的死数据。
- 仍被当前产品依赖的种子数据（如默认租户、管理员引导数据）：保留在文件 **后部 DML**；能写幂等则写幂等（如 `ON CONFLICT DO NOTHING`），但以现有项目写法为准，不强行改语义。
- 注释：让后人看懂「这块在干什么」即可，勿写长文 changelog。

**建议（合理则做）**

- 同一保留文件内按依赖排序：被引用表先建；索引/唯一约束跟表走。
- 多表时按域前缀分段（`sys_` / `infra_` / `im_` …），段首一行注释。
- 保留文件 `description`：若原名已不准确，可改成更贴切的 snake_case（版本号不变）。
- 若范围内存在「先建后删」的整表噪音，合并后净空则整段省略，并在向用户确认的摘要里写明「已省略：…」。
- **不要**为了好看重排范围外版本号；**不要**跨用户指定区间「顺手」再合下一批。

### 3. 确认清单（改文件 / 动库前必须展示）

向用户列出并等待明确同意：

| 项 | 内容 |
|----|------|
| 吸收并删除的文件 | 完整路径列表 |
| 保留并重写的文件 | 路径 + 新旧职责一句话 |
| 省略的中间对象/DML | 简述 |
| 数据源 | 见下一节解析结果（URL/库名/用户；**不要**把密码写进聊天记录时可打码） |
| History | 将 `DELETE` 的 `version` 列表；将 `repair` 的保留 `version` |
| 策略 | **A**：老库表已在 → 不重跑 SQL；只删吸收行 + 修保留 checksum |

用户未回复确认前：**停止**。

### 4. 数据源解析

读取并合并（后者覆盖同名键的常规 Spring 语义；并应用环境变量）：

- 目录：`relayflow-server/src/main/resources/`
- 文件：所有 `application*.yml` / `application*.yaml` / `application*.properties`
- 关注：`spring.datasource.url` / `username` / `password`
- 环境变量优先：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`，以及本项目常用的 `POSTGRES_USER`、`POSTGRES_PASSWORD`
- 默认（无覆盖时）与仓库一致：`jdbc:postgresql://localhost:5432/relayflow`，用户/密码 `relayflow`

若解析出 **多个** 不同 JDBC URL：对 **每一个** 都走同一套 history 修复（先全部列入确认清单）。  
当前仓库通常只有一个业务库；不要假设改配置文件本身——**只连库改 `flyway_schema_history`**。

连接前在确认清单中打印最终 URL（密码打码）。

### 5. 落盘

1. 写入保留版本合并后的 SQL。
2. 删除吸收版本文件。
3. 不要删除或修改范围外迁移。

### 6. History 策略 A（确认后执行）

对每个目标库：

1. 确认表存在：`flyway_schema_history`（Flyway 默认表名；若项目将来定制则跟配置走，当前为默认）。
2. 删除被吸收版本的成功记录，例如：

```sql
DELETE FROM flyway_schema_history
WHERE version IN (/* 吸收版本，如 '0.1.0.1', '0.1.0.2' */);
```

3. **修复保留版本 history 元数据**（文件已变，必须 repair，否则启动校验失败）：
   - 优先运行本 skill 脚本：[`scripts/repair_history.sh`](scripts/repair_history.sh)（内部：删吸收行 + 按 Flyway CRC32 更新保留行 **checksum / script / description**；也可调 Flyway `repair` 若环境可用）。
   - 若改了保留文件 `__` 后的描述名：`description` 必须同步（Flyway 把 `init_foundation` 存成 `init foundation`）；只改 checksum/script 会报 **Migration description mismatch**。
   - 若脚本不可用：用 `psql` 执行 DELETE 后，再用 Flyway 官方 `repair`（classpath / Docker `flyway/flyway`）对准同一 `filesystem:.../db/migration`。
4. **禁止**：删除保留版本的 history 行后指望 Flyway 重跑合并 SQL（表已存在会失败）。
5. **禁止**：未确认就 `DROP DATABASE` / `flyway clean` / 清空业务表。

### 7. 校验（必须；非空库全量）

做完后由 AI 自行验证，**不要**强制清库重跑全链：

1. **静态**：保留文件是否 DDL→DML；范围内净效果是否覆盖吸收脚本终态；无对已省略对象的引用；范围外文件未误改。
2. **History**：`SELECT version, description, checksum, success, script FROM flyway_schema_history ORDER BY installed_rank;`  
   - 吸收版本行不存在  
   - 保留版本行仍在且 `success = true`  
   - checksum / script / description 均与当前保留文件一致（description 为 `__` 后 snake_case，下划线变空格）
3. **可选轻量**：若本机有 `psql`，对保留文件做语法无执行检查有限（或仅 `EXPLAIN` 不适用）；**不要**对已有库执行合并文件全文。
4. 向用户报告：改了哪些文件、history 如何、校验结果；提醒用户可自行在别处做空库安装验证。

失败则停下，说明是文件问题还是 history 问题，给出下一修复动作，不要偷偷清库。

## 调用示例

```text
/flyway-squash-migrations
把 V0.1.0.1、V0.1.0.2、V0.1.0.3 合并进 V0.1.0.3
```

## 辅助脚本

- [`scripts/repair_history.sh`](scripts/repair_history.sh) — 解析参数：JDBC/用户/密码、吸收 version 列表、保留 version、迁移目录；执行 DELETE + checksum 更新。
- [`scripts/flyway_checksum.py`](scripts/flyway_checksum.py) — 计算与本仓库 Flyway 10 `ChecksumCalculator` 一致的 checksum（去 BOM、去掉换行后 CRC32 → Java signed int）。

改库类脚本 **仅在用户确认后** 执行。
