## ADDED Requirements

### Requirement: Create Hub four actions use live backends

工作台 Create Hub「+」四项 MUST 使用已对接的后端或既有领域路径，MUST NOT 依赖已删除的客户端假数据草稿（组织邀请、外部联系人搜索/发起）。

#### Scenario: Add external uses contact API

- **WHEN** 用户从「+」打开添加外部联系人并搜索确切手机号
- **THEN** 结果来自 `GET /app-api/system/contact/search-account`

#### Scenario: Org invite gated by capability

- **WHEN** `GET /app-api/system/org-invite/capability` 返回 `canInvite=false`
- **THEN** 「添加企业成员」不出现在「+」菜单

#### Scenario: Create group and document

- **WHEN** 用户选择创建群组或创建文档
- **THEN** 分别走既有建群 API 与文档库 `createPage` 路径并可进入对应页面
