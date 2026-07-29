## ADDED Requirements

### Requirement: Member picker uses real managed groups web

工作台成员选择器「我管理的群组」MUST 基于当前用户为群主或管理员的群会话与成员列表组装，MUST NOT 依赖演示种子数据。无此类群时 MUST 展示空状态。勾选群 MUST 将群内用户成员展开加入已选（排除自己与 exclude 列表）。

#### Scenario: Load managed groups from IM

- **WHEN** 用户打开选择器并进入「我管理的群组」
- **THEN** 系统加载其可管理的真实群组列表（或空）

#### Scenario: Invite members modal shares picker

- **WHEN** 用户在群聊中打开邀请成员
- **THEN** UI 使用共享成员选择器，且已在群内的用户不可再选
