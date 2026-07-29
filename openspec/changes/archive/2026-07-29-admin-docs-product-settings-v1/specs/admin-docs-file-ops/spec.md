## ADDED Requirements

### Requirement: Admin enterprise docs file list

管理端 SHALL 提供企业云文档对象分页列表（`GET /admin-api/docs/files`），覆盖 `RICH_DOC` 与 `FILE`，并包含 `ACTIVE` 与未销毁的 `DELETED`。列表 MUST 支持按标题关键词、类型、生命周期、所有者筛选。调用须具备 `docs:files:query`。本能力 MUST NOT 使用 `/admin-api/infra/file` 冒充。

#### Scenario: List filters by owner and lifecycle
- **WHEN** 管理员按所有者与 `DELETED` 筛选
- **THEN** 返回符合条件的对象元数据页

#### Scenario: Title search
- **WHEN** 管理员提供标题关键词
- **THEN** 系统按标题匹配过滤（不必全文检索正文）

### Requirement: Admin transfer ownership API

系统 SHALL 提供 `POST /admin-api/docs/files/transfer-owner`（需 `docs:files:transfer`），支持单个或批量 ACTIVE 对象转移所有者。

#### Scenario: Batch transfer
- **WHEN** 管理员提交多个 ACTIVE objectId 与同一新所有者
- **THEN** 各对象所有权均更新为该新所有者
- **AND** 任一失败 MUST 按 contract 明确部分成功或事务失败策略（实现时写死并测）

### Requirement: Admin delete restore and purge APIs

系统 SHALL 提供 `POST /admin-api/docs/files/delete`（`:delete`）、`restore`（`:restore`）、`purge`（`:purge`），语义遵循 `docs` 规格中的生命周期与 90 天窗口。

#### Scenario: Purge is irreversible
- **WHEN** 管理员对 `DELETED` 或超窗对象执行 purge
- **THEN** 对象进入 `PURGED`（或等价不可恢复状态）
- **AND** 后续 restore MUST 失败

### Requirement: Copy workspace link action

管理端文件行操作 SHALL 支持复制工作台打开链接（`/app/docs?docId={objectId}`）。复制链接 MUST NOT 授予管理员额外 ACL。

#### Scenario: Link does not bypass ACL
- **WHEN** 管理员仅复制链接且自身非所有者/被授权人
- **THEN** 其打开该链接时仍按普通成员 ACL 被拒绝或需申请（与现有产品行为一致）
