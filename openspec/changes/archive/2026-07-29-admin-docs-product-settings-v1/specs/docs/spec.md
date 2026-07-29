## MODIFIED Requirements

### Requirement: Org-level docs product settings reserved

系统 SHALL 在管理端产品设置目录中为 **云文档** 提供组织级设置与运营入口。占位体验 MUST 被可配置壳层替代（见 `admin-docs-product-settings`）。具体策略项、文件运营与配额行为以本 change 新增需求为准。

#### Scenario: Admin opens docs from product catalog
- **WHEN** 管理员从产品设置进入云文档
- **THEN** 系统展示可导航的云文档管理壳层（非永久「后续开放」整页占位）

## ADDED Requirements

### Requirement: Org-level docs tenant settings persistence

系统 SHALL 以表 `doc_tenant_settings`（每租户一行，`tenant_id` 主键）持久化组织策略，至少包含：`member_share_enabled`、`markdown_export_enabled`、`drive_upload_enabled`（均布尔，默认 `true`）。无行时读路径 MUST 使用上述默认值；写路径 MUST upsert。

#### Scenario: First read without row
- **WHEN** 租户尚无 `doc_tenant_settings` 行且成员请求生效设置
- **THEN** 返回三项均为 `true` 的默认策略

#### Scenario: Upsert on admin save
- **WHEN** 管理员 PUT 产品设置
- **THEN** 该租户存在一行且字段与请求一致

### Requirement: Docs product-settings Admin and effective APIs

系统 SHALL 提供 `GET/PUT /admin-api/docs/product-settings`（需 `docs:product-settings:query|update`）以及 `GET /app-api/docs/settings/effective`（已登录成员）返回生效布尔策略。

#### Scenario: Admin get settings
- **WHEN** 具备 query 权限的管理员 GET 产品设置
- **THEN** 返回当前租户三项开关

#### Scenario: Member effective settings
- **WHEN** 企业成员 GET `/app-api/docs/settings/effective`
- **THEN** 返回与后端强制一致的生效值

### Requirement: Enforce member share org policy

当 `member_share_enabled` 为 `false` 时，系统 MUST 拒绝新建、更新文档分享授权的写操作；已有有效授权 MUST 仍可按 ACL 读取，直至被撤销或文档不可用。

#### Scenario: Share create rejected when disabled
- **WHEN** 组织关闭成员分享且所有者尝试 POST share
- **THEN** 请求失败且不创建新授权

#### Scenario: Existing share still readable
- **WHEN** 关闭成员分享前已存在 B 对文档的 VIEW 授权
- **THEN** B 仍可按原授权读取该文档（在文档仍 ACTIVE 且授权未撤销时）

### Requirement: Enforce markdown export org policy

当 `markdown_export_enabled` 为 `false` 时，系统 MUST 拒绝 Markdown 导出请求。

#### Scenario: Export rejected when disabled
- **WHEN** 组织关闭导出且用户请求 `format=md` 导出
- **THEN** 请求失败且不返回导出内容

### Requirement: Enforce drive upload org policy

当 `drive_upload_enabled` 为 `false` 时，系统 MUST 拒绝将新 `FILE` 注册到云盘的写路径。

#### Scenario: Drive file register rejected when disabled
- **WHEN** 组织关闭云盘上传且用户尝试注册新 FILE
- **THEN** 请求失败且不创建新的 ACTIVE FILE 对象

### Requirement: No external or anonymous doc share in V1

V1 MUST NOT 提供组织外用户、互联网链接或匿名访问云文档的能力；组织策略 MUST NOT 以开关形式开放此类访问。

#### Scenario: No external share API
- **WHEN** 客户端尝试以非本企业成员身份获得文档授权
- **THEN** 系统拒绝或保持现有「仅本企业成员」校验，且无「开放外链」组织开关

### Requirement: Document lifecycle for admin recovery

系统 SHALL 为 `doc_object` 区分生命周期：`ACTIVE`、`DELETED`、`PURGED`。用户或管理员删除 MUST 进入 `DELETED` 并记录 `deleted_at`。自 `deleted_at` 起 **90 天内**管理员可恢复为 `ACTIVE`；恢复时可保持原所有者或指定新所有者（须为本企业 ACTIVE 成员）。`PURGED` MUST 不可恢复，且 MUST NOT 再计入存储用量。

#### Scenario: Admin soft-delete
- **WHEN** 管理员对 ACTIVE 文档执行删除
- **THEN** 对象变为 `DELETED` 且成员无法再按 ACL 读取正文

#### Scenario: Admin restore within window
- **WHEN** 文档处于 `DELETED` 且未满 90 天
- **AND** 管理员执行恢复（可选新所有者）
- **THEN** 对象变为 `ACTIVE`
- **AND** `owner_user_id` 为原主或指定新主

#### Scenario: Restore after window denied
- **WHEN** `DELETED` 已超过 90 天且管理员请求恢复
- **THEN** 系统拒绝恢复

### Requirement: Admin transfer document ownership

管理员 SHALL 能将 **ACTIVE** 文档的所有权转移给本企业另一 ACTIVE 成员。转移后 `owner_user_id` MUST 更新；对象在原主个人库/云盘中的 placement MUST 按实现 contract 迁移到新主根（我的文档库或我的文件夹根之一，全租户一致）。

#### Scenario: Transfer active doc
- **WHEN** 管理员将文档从用户 A 转移给用户 B
- **THEN** B 成为所有者
- **AND** A 不再因所有权而拥有树节点管理权（除非仍持有 share）

### Requirement: Admin must not read document body via admin APIs

`/admin-api/docs/**` 文件运营接口 MUST NOT 返回文档正文（TipTap JSON 或文件字节）。管理员获取内容 MUST 仅能通过工作台链接并受普通 ACL 约束。

#### Scenario: Admin file list has no body
- **WHEN** 管理员 GET 企业文件列表
- **THEN** 响应含元数据且不含 body 字段或文件内容
