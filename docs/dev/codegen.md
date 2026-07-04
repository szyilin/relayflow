# DO / Mapper 代码生成约定

PostgreSQL 表结构以 **Flyway 迁移为唯一真源**；DO 与基础 Mapper **禁止 AI 或人工从零手写字段列表**，须通过 **MyBatis-Plus FastAutoGenerator** 连库生成。

> 行为规格见 `openspec/specs/` 与 active change；本文描述 **如何生成与分层**。

## 原则

| 项 | 规则 |
|----|------|
| 表结构真源 | `relayflow-server/src/main/resources/db/migration/*.sql` |
| 生成工具 | MyBatis-Plus `FastAutoGenerator`（`mybatis-plus-generator`） |
| 生成物位置 | **`target/generated-sources/mybatis/`**（不进 Git） |
| 禁止 | 在 `src/` 下手写 DO 字段、手写与表结构一一对应的 Mapper |
| 禁止 | 编辑 `target/generated-sources/` 下任何文件 |
| 允许手写 | 枚举、ExtMapper、ExtMapper XML、Service、Controller |

## 目录布局

以 `relayflow-module-system-biz` 为例：

```text
relayflow-module-system-biz/
├── src/main/java/.../module/system/
│   ├── dal/
│   │   └── mysql/
│   │       └── SysUserExtMapper.java      # 手写：自定义 SQL 接口
│   ├── enums/
│   │   └── TenantUserStatus.java          # 手写：业务枚举（非表反射生成）
│   └── ...
├── src/main/resources/mapper/
│   └── SysUserExtMapper.xml               # 手写：复杂 SQL
└── target/generated-sources/mybatis/      # 生成：不提交 Git
    └── com/relayflow/module/system/dal/
        ├── dataobject/SysUserDO.java
        └── mysql/SysUserMapper.java
```

框架基类（手写、供生成器模板引用）：

```text
relayflow-common/.../dal/
├── BaseDO.java          # id、审计字段、逻辑删除
└── TenantBaseDO.java    # + tenantId
```

代码生成入口（待 `scaffold-system-codegen` change 落地）：

```text
relayflow-tools/codegen/                   # Generator 配置 + Freemarker 模板
```

## 生成 vs 手写边界

### 生成（可覆盖，每次 regenerate）

- `*DO.java` — Lombok、`@TableName`、`BaseDO` / `TenantBaseDO` 继承
- `*Mapper.java` — `extends BaseMapper<XxxDO>` + `@Mapper`，**仅** CRUD，无自定义方法
- **不生成** Mapper XML（MyBatis-Plus `BaseMapper` 足够）

### 手写（永不覆盖）

| 类型 | 命名 | 说明 |
|------|------|------|
| 扩展 Mapper | `{Entity}ExtMapper` | `extends XxxMapper`，自定义方法 |
| 扩展 XML | `{Entity}ExtMapper.xml` | 复杂 SQL、JOIN |
| 业务枚举 | 如 `TenantUserStatus` | DB 存 VARCHAR/SMALLINT，Java 侧枚举映射在模板或类型转换中配置 |
| 基类 | `BaseDO`、`TenantBaseDO` | 生成器 `superClass` 指向 |

### 全局表（无 `tenant_id`）

`sys_user`、`sys_tenant`、`sys_tenant_user` 等须在生成策略与 `RelayflowTenantLineHandler.IGNORED_TABLES` 中单独配置；对应 DO 继承 `BaseDO` 而非 `TenantBaseDO`。

## 工作流

### 日常（表结构变更）

```text
1. 新增/修改 Flyway 迁移脚本（只增不改历史脚本）
2. 本地或 CI：PostgreSQL 执行 migrate
3. ./mvnw -pl relayflow-module-system-biz -am compile   # generate-sources 阶段跑 Generator
4. 审查 Flyway diff；必要时本地查看 generate 产物
5. 提交：仅 Flyway + 手写代码（ExtMapper 等），不提交 target/
```

### 首次 clone

```bash
docker compose -f deploy/compose.yml up -d    # PostgreSQL
./mvnw -pl relayflow-server -am compile       # Flyway + codegen + compile
```

生成物在 `target/generated-sources/`，IDEA 通过 `build-helper-maven-plugin` 识别为源码根。

## 生成器配置要点（实现参考）

实现 `scaffold-system-codegen` 时须满足：

- **PostgreSQL** JDBC，连接与 `application.yml` 一致
- **`enableLombok()`** — 禁止手写 getter/setter
- **`entityBuilder().superClass(BaseDO.class)`** — 按表是否含 `tenant_id` 切换 `TenantBaseDO`
- **`formatEntityFileName("%sDO")`** — 与 `code-style.md` 一致
- **`mapperBuilder().enableMapperAnnotation()`** — `@Mapper`
- **禁用** Service、Controller、Mapper XML 生成
- **`enableFileOverride()`** — 仅对生成目录内文件生效（`target/` 每次可清空重建）
- **表过滤**：`addInclude("sys_*")` 或按 change 指定；排除 `flyway_schema_history`
- **枚举列**：Freemarker 模板或 `typeConvertHandler` / `columnOverride`（如 `status` → `TenantUserStatus`）

## CI（推荐，随 codegen 模块一并接入）

```text
flyway migrate → codegen → mvn compile
```

可选：将 generate 后的 tree 与期望快照 diff，或仅依赖 compile 失败暴露不同步。

## AI 编码代理规则

1. **不得**在 `src/.../dal/dataobject/` 创建或修改 `*DO.java`（该目录仅存在于 `target/generated-sources/`）。
2. **不得**在 `src/.../dal/mysql/` 创建与表结构镜像的基础 `*Mapper.java`（无 `Ext` 后缀且仅 extends BaseMapper）。
3. 表增删字段：**只改 Flyway**，然后提示运行 codegen compile。
4. 自定义 SQL：只写 `{Entity}ExtMapper` + XML。
5. 业务枚举：可手写于 `enums/`，并在 codegen 模板中引用。

## 与 OpenSpec change 的关系

| Change | 范围 |
|--------|------|
| `system-schema-v1` 等 | Flyway 表结构 + 种子 + **手写枚举** |
| `scaffold-system-codegen`（待建） | Generator 模块、Maven 插件、模板、首跑验证 |

## 参考

- [MyBatis-Plus 代码生成器（新）](https://baomidou.com/guides/new-code-generator/)
- [MyBatis-Plus Generator 配置](https://baomidou.com/reference/new-code-generator-configuration/)
- 本项目：[database.md](database.md)、[code-style.md](code-style.md)
