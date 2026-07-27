## ADDED Requirements

### Requirement: Create group from shared member picker

系统 MUST 允许用户从工作台 Create Hub 或消息页发起创建群组，并使用共享成员选择器选择初始成员。创建成功后 MUST 建立 `type=group` 会话并选中该会话。从「我管理的群组」多选时，系统 MUST 将所选群的成员展开为用户 ID 并入创建请求（V1 简化语义），MUST NOT 把整个群会话嵌套为子会话。

#### Scenario: Create group with org and external members

- **WHEN** 用户选择至少一名组织内成员与一名外部联系人并确认创建
- **THEN** 系统创建群聊，初始成员包含这些用户 ID

### Requirement: List groups managed by current user

系统 MUST 提供「当前用户可管理的群组」列表供成员选择器使用（基于群主/管理员角色或产品约定的等价规则）。无管理群时 MUST 返回空列表而非错误。

#### Scenario: Empty managed groups

- **WHEN** 用户打开选择器「我管理的群组」且没有任何可管理群
- **THEN** 面板展示空状态，创建群组仍可仅从组织内/外部选人
