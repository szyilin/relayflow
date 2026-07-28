## ADDED Requirements

### Requirement: Org-level docs product settings reserved

系统 SHALL 在管理端产品设置目录中为 **云文档** 预留组织级设置入口。本版本 MUST 仅提供占位体验，MUST NOT 定义或实现具体云文档组织策略项、策略表或 Admin API。

#### Scenario: Placeholder only

- **WHEN** 管理员从产品设置进入云文档
- **THEN** 系统展示占位说明（组织级设置后续开放）
- **AND** MUST NOT 要求 `docs` 域已存在产品设置持久化
