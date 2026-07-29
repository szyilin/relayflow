## ADDED Requirements

### Requirement: Org-level calendar product settings reserved

系统 SHALL 在管理端产品设置目录中为 **日历** 预留组织级设置入口。本版本 MUST 仅提供占位体验，MUST NOT 定义或实现具体日历组织策略项、策略表或 Admin API。个人日历偏好继续由 `user-preference` 的日历段承载。

#### Scenario: Placeholder only

- **WHEN** 管理员从产品设置进入日历
- **THEN** 系统展示占位说明（组织级设置后续开放）
- **AND** MUST NOT 与用户头像菜单中的个人日历偏好配置混淆为同一真源

#### Scenario: Personal preference unchanged

- **WHEN** 成员在工作台设置中编辑日历个人偏好
- **THEN** 行为仍遵循既有 `user-preference` 规格
- **AND** MUST NOT 依赖尚未实现的日历组织产品设置 API
