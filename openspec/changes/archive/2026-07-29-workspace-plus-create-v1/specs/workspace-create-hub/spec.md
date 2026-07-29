## ADDED Requirements

### Requirement: Workspace create hub menu

工作台 MUST 在 Rail/资料头像旁提供「+」创建菜单。本期菜单 MUST 至少包含：创建群组、添加企业成员、添加外部联系人、创建文档。MUST NOT 在本期实现飞书其余项（录音转写、多维表格、问卷、会议室投屏、视频会议等）。

#### Scenario: Open create hub

- **WHEN** 有效组织成员点击头像旁「+」
- **THEN** 系统展示包含上述四项的菜单

#### Scenario: Create group from hub

- **WHEN** 用户选择「创建群组」
- **THEN** 系统打开创建群组流程（含成员选择器），成功后进入对应群会话

#### Scenario: Create document from hub

- **WHEN** 用户选择「创建文档」
- **THEN** 系统进入云文档并创建一篇新的个人文档库 RICH_DOC（或等价既有创建路径）

### Requirement: Create hub invite menu visibility

「添加企业成员」菜单项 MUST 仅在当前用户对该租户具备邀请能力时可见（由组织邀请设置与身份决定）。创建群组、添加外部联系人、创建文档 MUST 对有效组织成员可见，MUST NOT 依赖管理端 RBAC permission 码。

#### Scenario: Hide invite when disabled

- **WHEN** 租户邀请功能为关闭，或当前用户不在允许邀请的角色范围内
- **THEN** 「+」菜单 MUST NOT 展示「添加企业成员」

#### Scenario: Show invite for allowed member

- **WHEN** 租户允许邀请且当前用户满足管理员或全员邀请条件
- **THEN** 「+」菜单展示「添加企业成员」，点击后打开邀请弹窗（二维码/链接/邀请码/手机号）

### Requirement: Shared member picker shell

创建群组（及复用该选择器的场景）MUST 提供可选人面板，至少支持：搜索框；从「组织内联系人」「外部联系人」「我管理的群组」进入并面包屑返回；组织内部门钻取（含子部门「下级」）；外部联系人行展示「外部」标识；多选与已选汇总。

#### Scenario: Drill into organization department

- **WHEN** 用户在选择器中进入组织内并点击某子部门的「下级」
- **THEN** 面包屑更新，列表展示该部门下级与成员，且可用复选框选择成员

#### Scenario: Select external contact

- **WHEN** 用户在「外部联系人」列表勾选一名外部联系人
- **THEN** 该用户出现在已选列表，并带有可区分的外部标识（或等价信息）
