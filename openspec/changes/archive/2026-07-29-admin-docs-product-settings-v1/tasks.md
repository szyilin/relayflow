# Tasks: admin-docs-product-settings-v1

> 详规已填空。**编码在子 change 中进行**；本清单跟踪母框架验收与拆分。  
> 顺序锁定：**B → A1 → A2**。单次会话只做一波中的一层（web / api / integrate）。

---

## 0. 母框架（文档）

- [x] 0.1 能力地图与 infra 边界
- [x] 0.2 关闭 Open Questions；写入 Decisions
- [x] 0.3 填空 Wave B / A1 / A2
- [x] 0.4 壳层 IA 终稿
- [x] 0.5 正式 delta specs
- [x] 0.6 `openspec validate admin-docs-product-settings-v1 --strict`
- [x] 0.7 拆出子 change 目录（或按波次直接在本 change 实现——二选一；推荐拆子 change 名称如下）→ **决定：本母 change 内按波次实现，不另拆目录**

建议子 change 名（若日后拆分可参考）：

- `admin-docs-org-policy-web` / `-api` / `-integrate`
- `admin-docs-file-ops-web` / `-api` / `-integrate`
- `admin-docs-storage-quota-web` / `-api` / `-integrate`

---

## B. 组织策略

### B-web

- [x] B.1 壳层：`/admin/products/docs` redirect → `/permissions`；侧栏三入口；未交付页占位
- [x] B.2 权限与分享页：三开关（成员分享 / MD 导出 / 云盘上传）+ Mock store
- [x] B.3 `docs.adminEnabled=true`；权限码挂导航
- [x] B.4 `pnpm build` + `pnpm typecheck`；路径：`/admin/products/docs/permissions`

### B-api

- [x] B.5 Flyway：`doc_tenant_settings` + 权限种子
- [x] B.6 `GET/PUT /admin-api/docs/product-settings`
- [x] B.7 `GET /app-api/docs/settings/effective`
- [x] B.8 强制：share 写、md export、drive files 注册
- [x] B.9 `./mvnw -pl relayflow-server -am compile`

### B-integrate

- [x] B.10 去 Mock；看板；联调开关关闭时 API/UI 一致

---

## A1. 企业文件管理

### A1-web

- [x] A1.1 文件列表：列、类型/生命周期/所有者筛、标题搜索、分页（Mock）
- [x] A1.2 动作：转移所有者、删除、恢复、销毁、复制链接
- [x] A1.3 `pnpm build` + `pnpm typecheck`；路径：`/admin/products/docs/files`

### A1-api

- [x] A1.4 Flyway：`lifecycle` / `deleted_at`（及必要索引）；Admin 查询绕过逻辑删
- [x] A1.5 `GET /admin-api/docs/files`
- [x] A1.6 `POST .../transfer-owner`（迁到新主根，contract 写死）
- [x] A1.7 `POST .../delete` / `restore`（90d）/ `purge`
- [x] A1.8 禁止 Admin API 返回 body 内容
- [x] A1.9 `mvn compile`

### A1-integrate

- [x] A1.10 去 Mock；与工作台软删/最近 tombstone 对齐验收

---

## A2. 成员存储用量

### A2-web

- [x] A2.1 用量列表 + 默认上限/用户覆盖表单（Mock）
- [x] A2.2 「查看云文档」→ `/files?ownerUserId=`
- [x] A2.3 `pnpm build` + `pnpm typecheck`；路径：`/admin/products/docs/storage`

### A2-api

- [x] A2.4 Flyway：`storage_quota_default_bytes` + `doc_storage_quota_user_rule`
- [x] A2.5 `GET /admin-api/docs/storage-usage`；`GET/PUT .../storage-quota`
- [x] A2.6 计量：ACTIVE `FILE` → `infra_file.size`；触顶 `DOC_STORAGE_QUOTA_EXCEEDED`
- [x] A2.7 `mvn compile`

### A2-integrate

- [x] A2.8 去 Mock；上传触顶与列表用量联调

---

## 明确不做（验收对照）

- [x] N.1 管理员不凭身份读正文
- [x] N.2 不改造 `/admin/infra/*` 冒充本能力
- [x] N.3 不做外链/水印/信息流通/Wiki 组织设置/正文检索/跨产品总容量
