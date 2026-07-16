## ADDED Requirements

### Requirement: 飞书式个人名片共享组件

系统 MUST 提供可复用的个人名片界面，包含：封面背景区、叠压头像、显示名、企业/组织信息、签名或备注相关区域、操作按钮区。同一组件 MUST 支持本人（self）与他人（peer）两种模式。

#### Scenario: 本人打开个人名片

- **WHEN** 已登录成员从 Rail 资料菜单点击「我的个人名片」
- **THEN** 打开个人名片且模式为 self
- **AND** 可见封面、头像、显示名与个性签名编辑入口

#### Scenario: 通讯录打开他人名片

- **WHEN** 已登录成员在通讯录点击某组织内成员
- **THEN** 打开同一套个人名片且模式为 peer
- **AND** 展示该成员的显示名与可用资料（无签名则空态或占位文案）

### Requirement: 本人可替换封面且他人只读

个人名片封面区域在 self 模式下 MUST 支持点击以选择并预览新背景图；peer 模式下 MUST NOT 提供替换入口。

#### Scenario: 本人替换封面

- **WHEN** 成员在 self 名片点击封面区并选择图片
- **THEN** 名片立即展示新封面预览（本切片可先本地暂存）

#### Scenario: 他人封面不可改

- **WHEN** 成员在 peer 名片查看封面
- **THEN** 不出现替换/上传封面的交互入口

### Requirement: 个性签名位于名片内且按模式可写

个性签名 MUST 展示在个人名片内，MUST NOT 再作为 Rail 账号菜单的独立占位菜单项。self 模式 MUST 允许编辑签名；peer 模式 MUST 只读展示对方签名，MUST NOT 允许修改对方签名。

#### Scenario: 本人编辑签名

- **WHEN** 成员在 self 名片编辑并确认个性签名
- **THEN** 名片展示更新后的签名（本切片可先本地暂存）

#### Scenario: 他人签名只读

- **WHEN** 成员在 peer 名片查看对方签名
- **THEN** 签名不可编辑
- **AND** MUST NOT 出现可保存对方签名的控件

### Requirement: 他人名片支持备注与描述

在 peer 模式下，系统 MUST 提供「备注与描述」入口，允许当前用户为该联系人设置备注名与描述（本切片可本地暂存）。备注数据 MUST 视为当前查看者私有，MUST NOT 表现为对方公开资料的一部分。

#### Scenario: 编辑他人备注

- **WHEN** 成员在 peer 名片打开备注编辑并保存备注名或描述
- **THEN** 名片或备注区反映所保存内容（本切片可本地暂存）
- **AND** 该内容仅对当前查看者可见（产品语义；跨设备待 API）

#### Scenario: 本人名片无备注区

- **WHEN** 成员打开 self 名片
- **THEN** 不展示「给自己写备注」的备注编辑入口

### Requirement: 他人名片操作含消息与通话占位

peer 名片 MUST 提供「消息」操作，行为与现有通讯录发消息一致（打开/创建单聊）。peer 名片 MUST 提供「语音」「视频」入口，且二者在 V1 本切片 MUST 为显式占位（可见反馈如 toast「功能即将推出」），MUST NOT 静默无响应。

#### Scenario: 发消息

- **WHEN** 成员在 peer 名片点击消息
- **THEN** 进入与该成员的单聊会话（沿用现有 IM 打开逻辑）

#### Scenario: 语音视频占位

- **WHEN** 成员在 peer 名片点击语音或视频
- **THEN** 给予可见占位反馈
- **AND** MUST NOT 发起真实通话

### Requirement: 契约草案可对接后续 API

本切片 MUST 起草 `openspec/lanes/workspace-business-card/contract.md`，至少覆盖：本人签名/封面读写、他人资料只读获取、查看者备注读写的预期路径与字段，并标明本切片为前端暂存。

#### Scenario: 契约可发现

- **WHEN** 后续 `-api` 实现者查阅 lane 契约
- **THEN** 可从该文件获知字段与端点草案
