# 数据库约定

PostgreSQL + Flyway + MyBatis-Plus 的表结构、迁移与公共字段规范。

## 表命名

| 前缀 | 模块 | 示例 |
|------|------|------|
| `sys_` | system | `sys_user` |
| `infra_` | infra | `infra_file` |
| `im_` | im | `im_message` |
| `task_` | task | `task_item` |
| `cal_` | calendar | `cal_event` |
| `doc_` | docs | `doc_object`、`doc_library_node` |
| `bpm_` | bpm（V1.1） | `bpm_process` |

租户元数据表（`sys_tenant`、`sys_tenant_user`）不带业务表通用的 `tenant_id` 约束，详见 `tenant-ready-foundation` design。

## V1 单库与 Phase 2 分库

| 阶段 | 数据库 | Flyway 位置 |
|------|--------|-------------|
| **V1** | 单一 PostgreSQL（如 `relayflow`） | `relayflow-server/.../db/migration/` |
| **Phase 2** | 按域分库（`relayflow_system` 等） | 各 `*-server` 独立迁移 |

V1 用 **表前缀** 做逻辑分域，不建多库：

- `sys_*` 仅 system 域 Mapper 访问
- `infra_*` 仅 infra 域 Mapper 访问
- `im_*` 仅 im 域 Mapper 访问
- `task_*` 仅 task 域 Mapper 访问
- `cal_*` 仅 calendar 域 Mapper 访问
- `doc_*` 仅 docs 域 Mapper 访问

**禁止** im-biz 的 SQL 出现 `sys_` 表；跨域读数据须走 `*-api`（如 `AdminUserApi`），不直连他域表。跨域写副作用若不必与当前请求强一致，走领域消息（见 [`cross-domain-messaging.md`](cross-domain-messaging.md)）。

## 公共字段

业务表（除租户元数据等全局表外）应包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `BIGINT` | 主键，**雪花 ID**（应用层生成，非数据库自增） |
| `tenant_id` | `BIGINT NOT NULL` | 租户 ID；V1 默认 `1` |
| `creator` | `BIGINT` | 创建人 user id |
| `create_time` | `TIMESTAMPTZ NOT NULL` | 创建时间 |
| `updater` | `BIGINT` | 最后更新人 user id |
| `update_time` | `TIMESTAMPTZ NOT NULL` | 最后更新时间 |
| `deleted` | `SMALLINT NOT NULL DEFAULT 0` | 逻辑删除：`0` 未删除，`1` 已删除 |

### 命名

- 列名统一 **snake_case**，时间字段使用 `create_time` / `update_time`（不使用 `created_at`）。
- Java DO 字段与之对应：`createTime`、`updateTime`（MyBatis-Plus 驼峰映射）。

### 主键：雪花 ID

- 由框架层统一发号（如 MyBatis-Plus `IdType.ASSIGN_ID` 或自定义 `IdGenerator`）。
- 禁止使用 PostgreSQL `SERIAL` / `IDENTITY` 作为业务主键。
- 利于多实例部署与 IM 等高写入场景。

### 逻辑删除

- `deleted` 使用 `SMALLINT`，默认值 `0`，与 MyBatis-Plus `@TableLogic` 默认行为一致。
- 查询默认过滤 `deleted = 0`；物理删除仅用于运维/合规等特殊场景。

### 索引

- 租户内唯一约束：`UNIQUE (tenant_id, username)` 等。
- 查询索引以 `tenant_id` 为 leading column：`(tenant_id, …)`。

## Flyway 迁移

| 项 | 规则 |
|----|------|
| 位置 | `relayflow-server/src/main/resources/db/migration/` |
| 文件名 | `V{major}.{minor}.{patch}.{seq}__{description}.sql` |
| 前三段 | 与 **产品 SemVer** 对齐，表示该脚本随哪个版本发布 |
| 第四段 `seq` | 同一产品版本内从 `1` 递增，区分同版本多次 SQL |
| 内容 | 只增不改历史脚本；破坏性变更用新迁移 + 数据迁移步骤 |

### 命名示例

产品版本 **0.1.0**（开发期，major 为 `0`）首次发版，含 3 个迁移脚本：

```text
V0.1.0.1__init_tenant.sql
V0.1.0.2__init_system.sql
V0.1.0.3__init_infra.sql
```

产品版本 **0.2.0** 新增 IM 表：

```text
V0.2.0.1__add_im_message.sql
```

### 纪律

- 前三段必须与该脚本所属 **Release 的 SemVer 一致**；换版本时 `seq` 从 `1` 重新起。
- 同一 Release 需要多条 SQL 时，只递增第四段：`V0.1.0.1` → `V0.1.0.2`，不要跳到其他产品版本段。
- 某次发版无数据库变更：不新增文件即可，不要凑空迁移。
- 生产热修仍挂在对应产品版本下，例如 `V0.1.0.4__hotfix_index.sql`，或随 patch 发版 `V0.1.1.1__...`（团队择一后保持一致）。
- `{description}` 使用 **snake_case** 英文短描述。
- **开发期 major 固定为 `0`**；**禁止** AI 或贡献者自行升到 `1.0.0`。只有产品负责人宣布正式上线后才允许切到 `1.x`（见 [`CONTRIBUTING.md`](../../CONTRIBUTING.md) § 版本号）。

### 新装与升级

Flyway 对 **空库首次部署** 与 **老库升级** 行为一致：按版本号 **从小到大依次执行** 所有尚未记录在 `flyway_schema_history` 中的脚本。  
因此新用户安装最新包时，仍会执行从 `V0.1.0.1` 到当前版本的 **完整迁移链**，最终 schema 与从首版一路升级上来的环境一致。这是预期行为，不是重复执行错误。

### 开发期（`0.x`）schema 演进与合并

产品 **尚未正式发版 `1.0.0`** 之前（Maven / Flyway 前三段均为 `0.x`），允许在开发中 **重新设计表结构**，包括：

| 允许（须慎重） | 说明 |
|----------------|------|
| 多表合并为一表 | 例如发现若干「备注/扩展」表可收拢 |
| 一表拆成多表 | 字段职责清晰后再拆 |
| `DROP TABLE` / 删列后重建 | **不**要求编写数据回填或兼容旧数据的迁移脚本 |
| 改列类型 / 改唯一约束 | 以新迁移脚本落地即可 |

**硬性条件：**

1. **先征得产品负责人（人类）明确同意**，再执行删表、合表、不兼容改列；AI **禁止**擅自做破坏性 schema 变更。
2. 用 **新 Flyway 脚本** 表达变更（仍遵守「不改已执行历史脚本」）；`0.x` 下可不做旧数据适配，本地/测试库可重置后跑全链。
3. 变更须写进当次 OpenSpec change 的 design/tasks，说明「为何合并/拆分」与替代表。
4. **`1.0.0` 正式上线后** 恢复常规纪律：破坏性变更必须带可回滚/可迁移方案，**禁止**无迁移的暴力删表。

与 [`CONTRIBUTING.md`](../../CONTRIBUTING.md) § 版本号一致：major 升到 `1` 只能由产品负责人宣布；开发期可「暴力演进 schema」，但 **不是** 无沟通的随意删表。

> 示例：`sys_contact_remark` 在 `0.x` 看起来合理；若后续发现应并入更广的「关系扩展」表，可在征得同意后 drop 重建，**无需**迁就已有备注数据。

## SQL 编写

- 参数占位使用 `#{}`，禁止字符串拼接 SQL。
- 迁移脚本须可重复执行的安全写法（`IF NOT EXISTS` 等）或依赖 Flyway 版本保证只执行一次。
- 种子数据（如默认租户 `id=1`）写在首版相关迁移中。

### PostgreSQL JSONB

- 列类型使用 `JSONB`（如 `im_message.content_json`）。
- **禁止**在 Flyway 中把 JSON 列建成 `TEXT`/`VARCHAR` 再在应用层「凑合」——PostgreSQL 对 `varchar → jsonb` 无隐式转换，MyBatis 直接绑 `String` 会在 INSERT/UPDATE 时报错。
- DO 字段映射为 `String`（JSON 文本），并通过 `@TableField(..., typeHandler = JsonbTypeHandler.class)` 写入；`JsonbTypeHandler` 位于 `relayflow-spring-boot-starter-mybatis`。
- CLI 生成 DO 时会自动为 `jsonb` 列注入上述 `typeHandler`（见 [codegen.md](codegen.md)）。

## DO / Mapper 与表结构同步

- **Flyway 脚本是表结构唯一真源**。
- `relayflow-codegen` 向临时目录生成标准三件套（`*DO`、`*Mapper.java`、`*Mapper.xml`），供与 `*-biz/src/` 中已管理文件 **diff 后增量合并**；**三件套进 Git**。
- **禁止**脱离 codegen 从零手写整份 DO 字段列表；**禁止**把业务 SQL 写进生成的 `*Mapper.xml`。
- 自定义 SQL 使用 `{Entity}ExtMapper` + `*ExtMapper.xml`（或等价 PublicMapper），与基础 Mapper **分文件**。
- 详见 [codegen.md](codegen.md)。

## 与 OpenSpec 的关系

表结构、租户字段等行为要求以 `openspec/specs/` 及 active change 的 spec delta 为准；本文档约定 **如何实现** 的字段形态与迁移纪律。
