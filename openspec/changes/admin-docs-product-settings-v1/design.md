## Context

- 母壳层 `admin-product-settings-v1` 已交付：`/admin/products/docs` 为占位页；IM 组织策略已按「Admin 表单 + App effective + 写路径强制」落地。
- 工作台 docs 已有：`doc_object`（`RICH_DOC`/`FILE`）、库/云盘树、`doc_share`（本企业成员 `VIEW`/`EDIT`）、Markdown 导出、MyBatis `@TableLogic` 软删；**无**外链分享、无管理员运营 API、无按人配额、无恢复窗口。
- 表前缀约定：**`doc_`**（非 `docs_`）。Admin API 前缀：`/admin-api/docs/**`（与 `/app-api/docs/**` 同模块名）。
- 飞书参考：文件管理、存储上限、云文档设置 / 文档权限默认值（见 proposal 链接）。

## Goals / Non-Goals

**Goals**

- 云文档管理后台按 **B 组织策略 → A1 企业文件运营 → A2 按人用量** 交付
- 每波 `-web` → `-api` → `-integrate`；策略/配额强制以后端为准
- 与 `/admin/infra/file|storage`、个人偏好严格分界

**Non-Goals**

- V1 不做：外链/互联网分享、水印、信息流通度、知识库组织设置、全文内容检索、跨产品统一企业总存储、管理员任意阅读正文
- 不改造 IM/日历产品设置；不把 infra 页冒充 docs 运营台

---

## Capability map

```text
B  组织策略     /admin/products/docs/permissions
A1 企业文件     /admin/products/docs/files
A2 成员用量     /admin/products/docs/storage
C  单文档 ACL   工作台已有；受 B 天花板约束
```

---

## Shell IA（终稿）

| 侧栏 | 路由 | 波次 |
|------|------|------|
| （入口） | `/admin/products/docs` → redirect `/permissions` | 壳 |
| 权限与分享 | `/admin/products/docs/permissions` | B |
| 文件管理 | `/admin/products/docs/files` | A1 |
| 存储空间 | `/admin/products/docs/storage` | A2 |

权限码（种子，绑 `super_admin`）：

| 码 | 用途 |
|----|------|
| `docs:product-settings` | 菜单可见（可选） |
| `docs:product-settings:query` / `:update` | B |
| `docs:files:query` / `:transfer` / `:delete` / `:restore` / `:purge` | A1 |
| `docs:storage-quota:query` / `:update` | A2 |

未交付子页：侧栏可点但内容区「即将开放」，**不得**可保存假成功。

---

## Decisions

| ID | 决策 | 说明 |
|----|------|------|
| D1 | 路由根 | `/admin/products/docs` + 上表三子页 |
| D2 | 分层 | 仅组织面；不写 `user-preference` |
| D3 | 顺序 | **B → A1 → A2** |
| D4 | vs infra | docs 运营/配额 ≠ MinIO 配置 / 通用 `infra_file` 列表 |
| D5 | 管理员读正文 | **禁止**凭管理身份读 body；仅元数据 + 生命周期；可「复制工作台链接」 |
| D6 | 表前缀 | `doc_*` |
| D7 | 策略存储 | A 类：`doc_tenant_settings` 每租户一行（对齐 `im_tenant_settings`） |
| D8 | 外部分享 | 产品尚无外链；V1 **不提供**外开开关；规格写明隐含禁止组织外/匿名访问 |
| D9 | 软删恢复 | 扩展生命周期：`ACTIVE` / `DELETED` / `PURGED`；用户侧暂无独立回收站 UI；管理员可查已删并恢复/销毁 |
| D10 | A2 计量 | V1 **仅**计量所有者名下 `FILE` 对象绑定的 `infra_file.size`；`RICH_DOC` 正文不计入（后置） |
| D11 | A2 配额对象 | 全员默认上限 + **USER** 白名单覆盖；部门/用户组白名单后置 |
| D12 | 离职转移 | V1 不另做 HR 入口；管理员在 A1 对已离职所有者文件手工转移即可 |

### Open Questions → 关闭

| # | 结论 |
|---|------|
| 1 第一刀 | **B**，然后 A1，再 A2 |
| 2 B 首批 | 见 B1：`member_share_enabled`、`markdown_export_enabled`、`drive_upload_enabled` |
| 3 恢复窗口 | **90 天**可 `restore`；超时仅允许 `purge`（或已 purge）；用户 trash 30 天 UI **不做** |
| 4 A2 范围 | **仅 docs 域 FILE 按人配额**；不做企业总存储跨产品 |
| 5 策略表 | 单表 `doc_tenant_settings` 分列（非 JSON 大包） |
| 6 权限码 | 独立 `docs:*`（上表） |
| 7 读正文 | **否**（D5） |
| 8 离职 | **并入 A1 手工转移**（D12） |

---

## Wave B — 组织策略（完整）

### B1 设置项

| ID | 字段 | 语义 | V1 | 默认 | 强制点 |
|----|------|------|----|------|--------|
| B-01 | `member_share_enabled` | 是否允许文档所有者向本企业成员授予/变更 `doc_share` | ✅ | `true` | `POST/PUT/DELETE .../shares`；为 `false` 时拒绝并返回明确错误 |
| B-02 | `markdown_export_enabled` | 是否允许 Markdown 导出 | ✅ | `true` | export `format=md` |
| B-03 | `drive_upload_enabled` | 是否允许注册/上传云盘 `FILE` | ✅ | `true` | `POST /app-api/docs/drive/files`（及必要的前置校验） |
| B-04 | 外链/组织外分享 | — | ❌ | 隐含禁止 | 产品无此能力；禁止另开后门 |
| B-05 | 水印 / 信息流通 / Wiki 谁可建 | — | ❌ | — | 后置 change |

已有 share 不受 B-01 关闭影响（历史授权仍可读）；**仅禁止新建/变更/再授权**。关闭导出不影响在线阅读。

### B2 数据与 API

```text
doc_tenant_settings
  tenant_id PK
  member_share_enabled BOOLEAN NOT NULL DEFAULT TRUE
  markdown_export_enabled BOOLEAN NOT NULL DEFAULT TRUE
  drive_upload_enabled BOOLEAN NOT NULL DEFAULT TRUE
  create_time, update_time, creator, updater, deleted  -- 公共字段按 database.md
```

未建行时：读路径使用上表默认（C 类合并或首次 GET 虚拟默认；PUT 时 upsert）。

| API | 说明 |
|-----|------|
| `GET /admin-api/docs/product-settings` | 需 `docs:product-settings:query` |
| `PUT /admin-api/docs/product-settings` | 需 `:update`；body 含 B-01..03 |
| `GET /app-api/docs/settings/effective` | 登录成员；返回生效布尔值供 UI 显隐 |

### B3 与单文档 ACL

- B-01=`false`：分享对话框只读提示；API 拒绝写 share。
- 单文档无法「突破」组织关闭的分享/导出/上传。

### B4 子 change

`admin-docs-org-policy-web` → `-api` → `-integrate`

---

## Wave A1 — 企业文件管理（完整）

### A1-1 能力

| ID | 能力 | V1 | 说明 |
|----|------|----|------|
| A1-01 | 分页列表 | ✅ | `RICH_DOC`+`FILE`；含已软删（未 purge） |
| A1-02 | 标题关键词检索 | ✅ | `title ILIKE`；**不做**正文检索 |
| A1-03 | 筛：类型 / 生命周期 / 所有者 | ✅ | |
| A1-04 | 转移所有者（单/批） | ✅ | 仅 `ACTIVE`；新主须本企业 ACTIVE 成员；库/盘 placement 仍挂原树或按设计迁移见下 |
| A1-05 | 管理员软删 | ✅ | → `DELETED` |
| A1-06 | 恢复 | ✅ | `DELETED`→`ACTIVE`；90 天内；可选挂原主或指定新主 |
| A1-07 | 销毁 purge | ✅ | 清 body/解绑存储；不可恢复 |
| A1-08 | 复制工作台链接 | ✅ | `/app/docs?docId=`；不保证管理员能打开 |
| A1-09 | 管理员操作日志 | ❌ | 后置 |
| A1-10 | 正文内容检索 | ❌ | 后置 |

### A1-2 状态机

```text
ACTIVE ──用户删或管理员删──▶ DELETED
                              │
                    ≤90d restore ▶ ACTIVE（owner=原主或新主）
                    purge / >90d 仅可 purge ▶ PURGED
```

| 状态 | `doc_object` | 占 FILE 存储 | 成员可见 |
|------|--------------|--------------|----------|
| ACTIVE | `deleted=0`，lifecycle=`ACTIVE` | 是 | 按 ACL |
| DELETED | `deleted=1`，lifecycle=`DELETED`，`deleted_at` | **否**（逻辑上不计入 A2；物理对象可延后 GC） | 否（recent 可 tombstone） |
| PURGED | 保留墓碑或硬删行；lifecycle=`PURGED` | 否 | 否 |

实现要点：Admin 查询/恢复须绕过或扩展 `@TableLogic`（专用 Admin Mapper / `lifecycle` 列）。`deleted_at` 用于 90 天窗口。

**转移所有者（ACTIVE）**：更新 `owner_user_id`；Library/Drive 节点：V1 **将对象归属改为新主**，若存在原主树 placement，则 **移动到新主「我的文档库」或「我的文件夹」根**（实现时选一并在 contract 写死；禁止跨租户）。

### A1-3 API

| API | 权限 |
|-----|------|
| `GET /admin-api/docs/files` | `docs:files:query` |
| `POST /admin-api/docs/files/transfer-owner` | `:transfer` |
| `POST /admin-api/docs/files/delete` | `:delete` |
| `POST /admin-api/docs/files/restore` | `:restore` |
| `POST /admin-api/docs/files/purge` | `:purge` |

列表字段至少：`objectId`、`title`、`type`、`lifecycle`、`ownerUserId`+展示名、`updatedAt`、`sizeBytes`（FILE）、`deletedAt`、`daysUntilPurge`。

### A1-4 子 change

`admin-docs-file-ops-web` → `-api` → `-integrate`

---

## Wave A2 — 成员存储用量（完整）

### A2-1 能力

| ID | 能力 | V1 | 说明 |
|----|------|----|------|
| A2-01 | 按成员列出已用/上限/占比 | ✅ | 上限「不限制」时占比展示 `—` 或 `0` 按 UI 统一 |
| A2-02 | 姓名关键词筛选 | ✅ | 邮箱/手机若成员资料可查则支持；部门筛后置 |
| A2-03 | 全员默认上限 / 不限制 | ✅ | |
| A2-04 | USER 白名单覆盖 | ✅ | 单用户更高上限或不限制 |
| A2-05 | 触顶拒上传 | ✅ | `drive_upload` + infra 确认链路；已有 FILE 仍可访问 |
| A2-06 | 导出 CSV | ❌ | 后置 |
| A2-07 | 跳转 A1 并滤所有者 | ✅ | 链到 `/files?ownerUserId=` |
| A2-08 | 跨产品总存储 | ❌ | |

### A2-2 计量

| 计入 | V1 |
|------|----|
| 所有者为该用户且 lifecycle=ACTIVE 的 `FILE` → `infra_file.size` | ✅ |
| `RICH_DOC` body / 历史版本 | ❌ |
| `DELETED`/`PURGED` | ❌ |
| IM 附件等 | ❌ |

汇总：可同步 sum 或 `doc_user_storage_usage` 缓存表；V1 允许列表接口实时聚合，数据量大再改异步。

### A2-3 数据与 API

```text
doc_tenant_settings 追加:
  storage_quota_default_bytes BIGINT NULL  -- null = 不限制

doc_storage_quota_user_rule
  id, tenant_id, user_id, quota_bytes NULL  -- null = 该用户不限制
  UNIQUE(tenant_id, user_id)
```

生效上限：用户规则 > 租户默认 > 不限制。

| API | 权限 |
|-----|------|
| `GET /admin-api/docs/storage-usage` | `docs:storage-quota:query` |
| `GET/PUT /admin-api/docs/storage-quota` | query/update（默认 + 规则列表） |

触顶错误码：如 `DOC_STORAGE_QUOTA_EXCEEDED`（写入 `docs` 错误码约定）。

### A2-4 子 change

`admin-docs-storage-quota-web` → `-api` → `-integrate`

---

## Risks

| 风险 | 缓解 |
|------|------|
| `@TableLogic` 与 Admin 恢复冲突 | A1-api 专用查询/更新路径；测恢复 |
| 转移后树节点孤儿 | contract 写死「迁到新主根」；集成测 |
| A2 实时聚合慢 | 先限制页大小；必要时加汇总表 |
| 与 infra 文件页混淆 | 文案+权限码+路由隔离 |

## Migration Plan

1. B：Flyway `doc_tenant_settings` + 权限种子 → Admin/App API → 强制 share/export/upload → UI
2. A1：`lifecycle`/`deleted_at` 列（若需）+ Admin files API → UI
3. A2：配额列/规则表 + 用量 API + 上传强制 → UI
4. 每波：`pnpm build`+`typecheck`；API 波次 `mvn compile`
