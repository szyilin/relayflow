# 后端架构演进

RelayFlow 采用 **两阶段架构**：先 **微服务就绪的模块化单体（V1）**，在运维与业务成熟后再 **拆分为分布式微服务（Phase 2）**。业务代码边界从第一天按 Phase 2 编写，避免日后重写。

## 阶段总览

| 阶段 | 部署形态 | 何时 |
|------|----------|------|
| **V1** | 单一 `relayflow-server` JAR | 现在开始开发 |
| **Phase 2** | Gateway + Nacos + `*-server` | IM/运维成熟且满足拆分触发条件时 |

```text
V1（现在）                         Phase 2（以后）
─────────────                      ─────────────────
relayflow-server                   relayflow-gateway
  ├── system-biz                     ├── system-server → system-biz
  ├── infra-biz          ──演进──►    ├── infra-server  → infra-biz
  └── im-biz                         └── im-server     → im-biz
       │                                    │
       └── 一个 PostgreSQL（逻辑分域）        └── 分库（同 PG 实例）
```

## V1：微服务就绪的模块化单体

### Maven 结构

```text
relayflow-dependencies/
relayflow-framework/
relayflow-module-system/
  ├── relayflow-module-system-api/    # 跨域契约（未来 Feign）
  └── relayflow-module-system-biz/
relayflow-module-infra/
  ├── relayflow-module-infra-api/
  └── relayflow-module-infra-biz/
relayflow-module-im/
  ├── relayflow-module-im-api/
  └── relayflow-module-im-biz/
relayflow-server/                     # 唯一运行时入口
```

**V1 不创建**：`relayflow-gateway`、`relayflow-*-server`（Phase 2 再加薄启动壳）。

### 四条耦合铁律（从第一行代码遵守）

1. **依赖方向**：`server → *-biz`；`*-biz → 其他 *-api`；**禁止 `*-biz → *-biz`**
2. **跨域调用**：只通过 `*-api` 接口（V1 本地 `ApiImpl`，Phase 2 换 Feign，业务代码不改）
3. **数据分域**：表前缀 `sys_` / `infra_` / `im_`；**禁止** im 的 Mapper 访问 `sys_` 表
4. **API 路径**：`/admin-api/{module}/`、`/app-api/{module}/` 与未来 Gateway 路由一致

### 数据库（V1）

- **一个** PostgreSQL 数据库（如 `relayflow`）
- Flyway 位于 `relayflow-server/src/main/resources/db/migration/`
- 用 **表前缀** 逻辑分域；Phase 2 再迁到 `relayflow_system` / `_infra` / `_im`

### 产品模块 vs 部署单元

工作台导航中的每一项 **≠** 一个微服务。官方能力 → 新 `relayflow-module-*`；是否独立 `*-server` 见 Phase 2 触发条件。第三方集成应用 → **开放平台 API**，不新增 JVM。

## Phase 2：分布式微服务（目标态）

详见 OpenSpec change [`distributed-backend-v1`](../../openspec/changes/distributed-backend-v1/design.md) 中 Phase 2 章节。

| 组件 | 说明 |
|------|------|
| Spring Cloud Gateway | 唯一对外 HTTP/WS 入口 |
| Nacos | 注册发现 + 配置 |
| OpenFeign | `*-api` 远程化 |
| `relayflow-*-server` | 薄启动器，仅依赖对应 `*-biz` |
| 分库 | 各 `*-server` 独立 database + Flyway |

### 拆分触发条件（满足多项再执行 Phase 2）

- IM 需 **独立水平扩展**（连接数/吞吐）
- 希望 **独立发布** 某域（如改 IM 不回归全量）
- 团队能 **独立运维** Nacos、Gateway、多服务
- 单库成为瓶颈，需 **物理分库**

不满足时 **继续保持 V1 单体部署**，模块边界不变。

## 参考

- 模块与代码风格：[`code-style.md`](code-style.md)
- 数据库与 Flyway：[`database.md`](database.md)
- API 约定：[`api.md`](api.md)
- 实施顺序：[`../../.cursor/rules/implementation-workflow.mdc`](../../.cursor/rules/implementation-workflow.mdc)
