## ADDED Requirements

### Requirement: Tenant IM product settings persistence

系统 MUST 按租户持久化即时消息组织策略，至少覆盖：对话群默认人数上限、用户规则集、群组规则集、公开群策略、部门群退出策略、群成员导出策略、消息撤回与编辑开关及时间窗。无行时 MUST 使用代码默认（默认群上限 **50**；撤回/编辑默认允许且时间窗 **24 小时**；公开群默认允许；部门群退出默认允许；导出默认禁止任何人）。

#### Scenario: Default when no row

- **WHEN** 租户从未保存过 IM 产品设置
- **THEN** 生效策略使用上述代码默认
- **AND** 读路径 MUST NOT 强制要求预插行（若实现选择 A 类 ensure，亦须与默认语义一致）

#### Scenario: Admin save round-trip

- **WHEN** 管理员经 Admin API 保存有效策略
- **THEN** 再次读取返回已保存值
- **AND** 工作台强制执行使用新值

### Requirement: Group size control for chat groups only

群规模管控 MUST 仅作用于对话群（`type=group` 且非被排除的群种类）。企业默认上限可选 **5 / 10 / 30 / 50** 或自定义正整数，默认 **50**。补充规则 MUST 支持：

1. **用户规则集**：选择部门与/或用户，规定其可发起群聊的人数上限；可多条。
2. **群组规则集**：选择本企业已创建群，规定该群人数上限；可多条。

上限解析 MUST 为：群组规则 > 发起人用户/部门规则 > 企业默认。部门群等被排除种类 MUST NOT 受默认/用户规则上限约束（与飞书说明对齐的 V1 子集）。

#### Scenario: Create group respects initiator limit

- **WHEN** 成员创建对话群且初始成员数（含自己）超过其生效上限
- **THEN** 系统拒绝创建并返回明确业务错误

#### Scenario: Add members respects group limit

- **WHEN** 向已有对话群添加成员将使总人数超过该群生效上限
- **THEN** 系统拒绝添加
- **AND** MUST NOT 因下调上限而自动移除已有成员

#### Scenario: User rule for department

- **WHEN** 管理员为某部门配置用户规则上限为 100，且企业默认为 50
- **THEN** 该部门成员发起建群时生效上限为 100（在未被更高优先级规则覆盖时）

#### Scenario: Group rule overrides default

- **WHEN** 某群存在群组规则上限 30，企业默认为 50
- **THEN** 该群加成员以 30 为上限

#### Scenario: Only current-tenant groups in group rules

- **WHEN** 管理员配置群组规则集
- **THEN** 可选群 MUST 仅限当前租户的对话群
- **AND** 他租户会话 MUST NOT 可选中

### Requirement: Group permission control

系统 MUST 提供与飞书「群权限管控」语义对齐的三项组织策略：

1. **进群管理**：允许或禁止将群设为「公开」（可被企业内搜索并加入）；允许时群主/管理员可在群设置开启可搜索。
2. **部门群管理**：允许或禁止部门成员退出部门群。
3. **群成员信息导出**：禁止任何人导出；或允许群主与群管理员导出；或仅允许指定成员在担任群主/管理员时导出。

#### Scenario: Deny public groups

- **WHEN** 组织策略禁止设置公开群
- **THEN** 工作台 MUST NOT 允许将群标为可搜索公开
- **AND** 公开群搜索加入不可用（或对已有公开群按实现约定关闭）

#### Scenario: Dept group leave denied

- **WHEN** 策略禁止部门成员退出部门群，且用户为该部门群普通成员
- **WHEN** 用户请求退出
- **THEN** 系统拒绝退出

#### Scenario: Member export denied

- **WHEN** 导出策略为禁止任何人导出
- **THEN** 群主与管理员亦不可导出群成员信息

### Requirement: Public searchable groups

在组织允许公开群时，系统 MUST 支持对话群的「可被搜索」标记；企业成员 MUST 能搜索公开群并加入（V1：直接加入，无需审批流）。

#### Scenario: Search and join public group

- **GIVEN** 组织允许公开群且群主已开启可搜索
- **WHEN** 同租户非成员用户搜索并加入该群
- **THEN** 用户成为群成员
- **AND** 人数仍受该群生效上限约束

### Requirement: Department group minimal model

系统 MUST 支持最小部门群模型（群种类可区分普通对话群与部门群，并可关联部门）。部门群退出行为 MUST 受组织「部门群管理」策略约束。完整的部门成员自动同步可分期，但本能力落地前「部门群退出」策略不得仅有无执行的死开关。

#### Scenario: Identify department group

- **WHEN** 会话为部门群
- **THEN** 客户端与 API 可区分其与普通对话群
- **AND** 退出路径应用部门群策略而非普通群自由退出

### Requirement: Group member export

在导出策略允许时，系统 MUST 允许授权主体导出群成员信息（字段对齐名片可见信息范围）。导出 MUST 限于当前租户该群成员。

#### Scenario: Owner exports when allowed

- **GIVEN** 策略允许群主与管理员导出
- **WHEN** 群主请求导出
- **THEN** 系统返回可下载的成员信息文件或等价载荷

### Requirement: Message recall org policy

系统 MUST 支持组织级「是否允许企业成员撤回已发送消息」及时间窗。该设置 MUST NOT 剥夺群主/群管理员对群内消息的撤回能力（与飞书说明一致）。撤回后消息对会话成员 MUST 展示为已撤回占位，不得继续展示原文。

#### Scenario: Member recall within window

- **GIVEN** 组织允许撤回且时间窗为消息发出后 24 小时内
- **WHEN** 发送者在时限内撤回本人消息
- **THEN** 消息对成员显示为已撤回
- **AND** 经 WebSocket 或列表刷新对其他成员可见

#### Scenario: Member recall disabled

- **GIVEN** 组织不允许撤回
- **WHEN** 普通成员尝试撤回本人消息
- **THEN** 系统拒绝
- **AND** 群主或群管理员仍可按产品规则撤回群内消息

#### Scenario: Recall after window

- **GIVEN** 允许撤回但已超过时间窗
- **WHEN** 普通成员尝试撤回
- **THEN** 系统拒绝

### Requirement: Message edit org policy

系统 MUST 支持组织级「是否允许企业成员编辑已发送消息」及时间窗。编辑 MUST 仅限发送者本人（V1）；超时或禁用时 MUST 拒绝。编辑后 MUST 保留同一 `seq`，并标记已编辑。

#### Scenario: Edit within window

- **GIVEN** 组织允许编辑且在时间窗内
- **WHEN** 发送者编辑本人文本消息
- **THEN** 内容更新且对其他成员同步
- **AND** 客户端可展示已编辑标记

#### Scenario: Edit disabled

- **GIVEN** 组织不允许编辑
- **WHEN** 发送者尝试编辑
- **THEN** 系统拒绝

### Requirement: Admin IM product settings API

系统 MUST 提供 `/admin-api/im/` 下的产品设置读写接口，并按管理端 RBAC permission 保护。写操作 MUST 校验规则集主体属于当前租户。

#### Scenario: Unauthorized admin API

- **WHEN** 无权限管理员调用更新接口
- **THEN** 系统拒绝（与既有 `@PreAuthorize` 语义一致）

### Requirement: Effective settings for workspace

工作台 MUST 能获取当前租户生效的 IM 组织策略（用于菜单显隐等），但强制执行 MUST 以后端写路径校验为准。

#### Scenario: Client reads effective settings

- **WHEN** 已认证成员请求生效策略
- **THEN** 返回与强制执行一致的撤回/编辑/公开群等开关与时间窗摘要
