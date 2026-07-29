# workspace-create-hub Specification

## Purpose
TBD - created by archiving change workspace-create-hub-web. Update Purpose after archive.
## Requirements
### Requirement: Workspace create hub menu web

工作台 Rail 头像旁 MUST 提供「+」菜单，含创建群组、添加企业成员、添加外部联系人、创建文档。添加企业成员 MUST 在 `canInvite` 为假时隐藏。创建文档 MUST 打开云文档并创建新文档。创建群组 MUST 打开含成员选择器的创建流程。

#### Scenario: Open plus menu

- **WHEN** 用户点击 Rail 头像旁「+」
- **THEN** 展示上述菜单项（邀请项受 canInvite 约束）

#### Scenario: Create document from menu

- **WHEN** 用户选择「创建文档」
- **THEN** 导航至 `/app/docs` 并创建一篇新文档进入编辑

### Requirement: Member picker categories web

创建群组成员选择器 MUST 支持组织内联系人（部门钻取）、外部联系人、我管理的群组三类入口与面包屑；外部行 MUST 有「外部」标识。`-web` 阶段外部与我管理的群组 MAY 使用 store 临时数据。

#### Scenario: Switch picker source

- **WHEN** 用户从组织内切换到外部联系人
- **THEN** 列表展示外部候选并可多选加入已选区

### Requirement: Member picker uses real managed groups web

工作台成员选择器「我管理的群组」MUST 基于当前用户为群主或管理员的群会话与成员列表组装，MUST NOT 依赖演示种子数据。无此类群时 MUST 展示空状态。勾选群 MUST 将群内用户成员展开加入已选（排除自己与 exclude 列表）。

#### Scenario: Load managed groups from IM

- **WHEN** 用户打开选择器并进入「我管理的群组」
- **THEN** 系统加载其可管理的真实群组列表（或空）

#### Scenario: Invite members modal shares picker

- **WHEN** 用户在群聊中打开邀请成员
- **THEN** UI 使用共享成员选择器，且已在群内的用户不可再选

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

