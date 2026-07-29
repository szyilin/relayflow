## ADDED Requirements

### Requirement: Group typing display names

系统在群会话展示 typing 时 MUST 使用正在输入者的显示名（用户昵称或 Bot 名称），例如「张三正在输入…」「助手名正在输入…」；多人时可聚合为「张三等 N 人正在输入…」（文案可微调）。bot_dm 会话 MAY 继续使用「对方正在输入…」。MUST NOT 在群聊一律显示「对方正在输入…」。

#### Scenario: Group shows member name while typing

- **WHEN** 群成员 A 上报 typing，且会话类型为群
- **THEN** 其他成员标题区/副标题看到含 A 显示名的输入提示

#### Scenario: Group shows bot name while bot typing

- **WHEN** 群内 Bot 发出 typing=true
- **THEN** 成员看到含该 Bot 显示名的输入提示
