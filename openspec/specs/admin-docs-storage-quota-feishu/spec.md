# admin-docs-storage-quota-feishu Specification

## Purpose

管理端云文档存储用量与配额（飞书对齐）：成员用量详情、独立上限管理页（成员/部门）、生效上限优先级，以及 Drive 上传对生效配额的强制校验。

## Requirements

### Requirement: Storage usage details page (Feishu-aligned)

管理端 SHALL 在 `/admin/products/docs/storage` 提供「存储用量详情」：按成员分页列出头像+姓名（`WorkspaceOwnerChip`）、可选部门名、已用存储、生效上限、已用占比、操作「查看云文档」。搜索框占位 MUST 表达支持姓名、邮箱或手机号。需 `docs:storage-quota:query`（或既有等价产品设置查询权限）。

#### Scenario: Member row uses owner chip

- **WHEN** 管理员打开存储用量页且列表有成员
- **THEN** 成员列展示 `WorkspaceOwnerChip`
- **AND** MUST NOT 在姓名下展示裸 userId 作为主副文案

#### Scenario: Search by name phone or email

- **WHEN** 管理员输入姓名、手机号或邮箱关键词并查询
- **THEN** 列表仅包含匹配成员（经成员搜索能力解析）

#### Scenario: View cloud docs jumps with owner filter

- **WHEN** 管理员点击某行「查看云文档」
- **THEN** 导航至文件管理并带上该所有者过滤条件

### Requirement: Storage quota management page

管理端 SHALL 提供独立的「存储上限管理」路由（如 `/admin/products/docs/storage/quota`），从用量页主按钮进入，MUST NOT 仅以小型 Modal 作为唯一编辑界面。页面 MUST 含 Tabs：**成员**、**部门**；**上限提示设置** MAY 为本版占位。需 `docs:storage-quota:update` 方可保存。

#### Scenario: Open quota management from usage page

- **WHEN** 具备更新权限的管理员在用量页选择「存储上限管理」
- **THEN** 进入上限管理页（非唯一依赖的小弹窗）

#### Scenario: Member tab tenant default in GB

- **WHEN** 管理员在成员 Tab 将租户默认设为每人最多 10 GB 并保存
- **THEN** 系统持久化对应字节上限
- **AND** UI 编辑单位为 GB（不限制与「每人最多 N GB」二选一）

#### Scenario: Member whitelist override

- **WHEN** 管理员添加成员白名单规则为不限制或指定 GB 上限并保存
- **THEN** 该成员生效上限按白名单覆盖租户默认
- **AND** 白名单条数 MUST NOT 超过约定上限（默认 20）

### Requirement: Effective quota priority member over department over tenant

生效上限 MUST 按：**成员白名单 > 部门规则 > 租户默认 > 不限制**。本版可不实现用户组规则；若 UI 提及飞书优先级，MUST 标明用户组后置。部门规则对成员生效时，MUST 基于该成员主部门（无主部门则跳过部门规则）。

#### Scenario: Member rule beats department and tenant

- **WHEN** 租户默认 10GB、成员主部门规则 5GB、该成员白名单为不限制
- **THEN** 该成员生效上限为不限制

#### Scenario: Department rule applies without member rule

- **WHEN** 成员无白名单、主部门规则为 5GB、租户默认为 10GB
- **THEN** 该成员生效上限为 5GB

### Requirement: Department quota tab

部门 Tab SHALL 允许为部门设置存储上限（一部门至多一条）：不限制或每部门最多 N GB（语义按 design：作用于该部门成员在无成员白名单时的生效值）。保存走同一配额写 API 的部门规则集合。

#### Scenario: One rule per department

- **WHEN** 管理员为同一部门保存两条冲突规则
- **THEN** 系统拒绝或按 API 约定去重，保证一部门至多一条生效规则

### Requirement: Drive upload still enforces effective quota

云盘新增 FILE 触顶行为 MUST 继续使用生效上限计算；超限返回可识别错误（如 `DOC_STORAGE_QUOTA_EXCEEDED`）。已有 ACTIVE FILE 在 ACL 允许下仍可访问。

#### Scenario: Upload blocked when over effective quota

- **WHEN** 成员已用 + 新 FILE 大小超过其生效上限
- **THEN** 注册/确认失败且不产生新的 ACTIVE FILE 占用
