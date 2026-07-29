## ADDED Requirements

### Requirement: Per-member docs FILE storage usage list

管理端 SHALL 提供按成员汇总的云文档存储用量列表（`GET /admin-api/docs/storage-usage`），需 `docs:storage-quota:query`。已用字节 MUST 等于该成员作为所有者、生命周期为 `ACTIVE` 的全部 `FILE` 所绑定 `infra_file.size` 之和。`RICH_DOC` 正文与跨产品附件 MUST NOT 计入 V1 用量。

#### Scenario: Usage reflects active files only
- **WHEN** 成员拥有 2 个 ACTIVE FILE（大小已知）与 1 个 DELETED FILE
- **THEN** 已用存储仅含 2 个 ACTIVE FILE 之和

#### Scenario: Jump to file ops filtered by owner
- **WHEN** 管理员在用量行选择查看云文档
- **THEN** 导航至文件管理并带上该所有者过滤条件

### Requirement: Default and per-user storage quota

系统 SHALL 在 `doc_tenant_settings.storage_quota_default_bytes` 存全员默认上限（`null`=不限制），并在 `doc_storage_quota_user_rule` 为单个用户覆盖上限（`null`=该用户不限制）。生效上限 MUST 为：用户规则 > 租户默认 > 不限制。Admin MUST 经 `GET/PUT /admin-api/docs/storage-quota`（`:query`/`:update`）读写。

#### Scenario: User override beats default
- **WHEN** 默认上限为 1GB 且用户规则为不限制
- **THEN** 该用户生效上限为不限制

#### Scenario: Default applies without user rule
- **WHEN** 默认上限为 100MB 且用户无覆盖行
- **THEN** 该用户生效上限为 100MB

### Requirement: Reject drive upload when over quota

当成员新增云盘 `FILE` 将导致已用+新文件大小超过生效上限时，系统 MUST 拒绝该上传/注册，并返回可识别错误（如 `DOC_STORAGE_QUOTA_EXCEEDED`）。已有 ACTIVE 文件 MUST 仍可按 ACL 访问。

#### Scenario: Upload blocked at quota
- **WHEN** 用户剩余配额小于待上传 FILE 大小
- **THEN** 注册/确认失败且不产生新的 ACTIVE FILE 占用

#### Scenario: Existing files remain accessible
- **WHEN** 用户已达上限
- **THEN** 其既有 ACTIVE FILE 仍可下载或预览（在 ACL 允许时）

### Requirement: Storage quota independent from infra storage page

成员云文档用量与上限管理 MUST 使用 `/admin/products/docs/storage` 与 `/admin-api/docs/storage-*`。`/admin/infra/storage` MUST 仅管理对象存储提供方配置，MUST NOT 被当作本需求的实现。

#### Scenario: Infra storage is not member quota
- **WHEN** 管理员打开基础设施存储配置
- **THEN** 该页不提供按成员 docs 配额编辑（本需求所指能力在 docs 产品设置下）
