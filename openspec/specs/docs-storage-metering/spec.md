# docs-storage-metering Specification

## Purpose

云文档存储计量：在 `doc_object` 上对云盘 FILE 反范式维护 `size_bytes`，管理端列表与按所有者汇总均基于该字段，并支持历史数据回填。

## Requirements

### Requirement: Denormalized FILE size on doc_object

系统 SHALL 在 `doc_object` 上为云盘 `FILE` 维护 `size_bytes`（可空）。当 Drive 注册/确认 FILE 成功并绑定 `storage_file_id` 时，MUST 将对应 infra 文件大小写入 `size_bytes`。`RICH_DOC` 与无对象存储绑定的对象 MUST 保持 `size_bytes` 为 null。docs-biz MUST NOT 通过 Mapper JOIN `infra_file`（或其他非 `doc_` 表）读取大小。

#### Scenario: FILE register writes size

- **WHEN** 成员成功注册一个 ACTIVE `FILE` 且 infra 文件 size 为 1048576
- **THEN** 该 `doc_object.size_bytes` 等于 1048576

#### Scenario: RICH_DOC has no size

- **WHEN** 创建或更新 `RICH_DOC`
- **THEN** 其 `size_bytes` 保持 null
- **AND** MUST NOT 因正文 JSON 长度写入配额用字节

### Requirement: Admin file list size from denormalized field

管理端文件列表（`GET /admin-api/docs/files`）的 `sizeBytes` MUST 取自 `doc_object.size_bytes`，MUST NOT 对每一行调用单条 `FileApi.getFile` 作为热路径。

#### Scenario: FILE row shows size without N+1 FileApi

- **WHEN** 管理员查询含 ACTIVE `FILE` 且 `size_bytes` 已填充的列表页
- **THEN** 响应中该行 `sizeBytes` 等于库中 `size_bytes`
- **AND** 列表装配 MUST NOT 依赖逐行 `getFile`

#### Scenario: Missing size displays as null

- **WHEN** `FILE` 的 `size_bytes` 尚未回填
- **THEN** `sizeBytes` 为 null（前端可展示为 —）

### Requirement: Per-owner usage sums size_bytes only

按所有者汇总的已用存储 MUST 等于该所有者名下 `type=FILE` 且 `lifecycle=ACTIVE` 且未删除的对象之 `size_bytes` 之和（null 视为 0）。汇总查询 MUST 仅访问 `doc_` 表。

#### Scenario: Usage ignores RICH_DOC and deleted FILE

- **WHEN** 用户拥有 1 个 ACTIVE FILE（10MB）、1 个 DELETED FILE（5MB）、1 个 RICH_DOC
- **THEN** 其已用存储为 10MB

### Requirement: Historical size backfill

系统 SHALL 提供可执行的回填路径（迁移任务或一次性管理/运维入口），经 `infra` 的 `FileApi`（宜批量）将历史 ACTIVE `FILE` 的 size 写入 `size_bytes`。回填 MUST 可重复执行且不覆盖已与源一致的值（或幂等更新）。

#### Scenario: Backfill fills null sizes

- **WHEN** 对租户执行回填且某 ACTIVE FILE 绑定的 infra 文件 size 已知、`size_bytes` 为 null
- **THEN** 回填后该行 `size_bytes` 等于 infra size
