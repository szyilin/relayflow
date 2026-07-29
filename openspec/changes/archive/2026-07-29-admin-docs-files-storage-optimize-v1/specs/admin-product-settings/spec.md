## ADDED Requirements

### Requirement: Admin docs file ops human search and identity UX

管理端云文档「文件管理」页 SHALL 以标题关键词、类型、状态及**成员搜索选人**筛选所有者；MUST NOT 以「所有者用户 ID」文本框作为主筛选控件。标题列 MUST NOT 默认在标题下展示 objectId。所有者列 MUST 使用 `WorkspaceOwnerChip`。转移/恢复指定新所有者 MUST 通过成员搜索选人（姓名/手机/邮箱），而非仅手填用户 ID。

#### Scenario: Owner filter via member search

- **WHEN** 管理员通过姓名、手机号或邮箱选中某成员并查询
- **THEN** 列表按该成员 `ownerUserId` 过滤
- **AND** 主 UI MUST NOT 要求管理员记忆数字用户 ID

#### Scenario: Title column without object id subtitle

- **WHEN** 文件列表渲染标题列
- **THEN** 主文案为文档标题
- **AND** MUST NOT 在标题正下方默认展示 `ID {objectId}`

#### Scenario: Deep link ownerUserId still works

- **WHEN** 从存储用量跳转携带 `?ownerUserId=`
- **THEN** 文件管理页应用该所有者过滤并展示对应用户 chip（若可解析）

### Requirement: Docs product storage navigation split

云文档管理壳层 SHALL 区分「存储用量详情」与「存储上限管理」入口（上限管理可为用量页主按钮进入的子路由）。文案 MUST 避免与基础设施「存储」配置混淆。

#### Scenario: Quota management is reachable from docs storage

- **WHEN** 管理员在云文档存储用量页
- **THEN** 可进入存储上限管理
- **AND** MUST NOT 依赖 `/admin/infra/storage` 完成成员配额编辑
