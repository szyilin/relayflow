## ADDED Requirements

### Requirement: Managed groups for member picker web

系统 MUST 能确定当前用户可管理的群组（至少含群主；管理员可选）。`-web` MAY 通过会话列表与成员接口前端聚合；后续 MAY 提供专用 `GET /app-api/im/group/managed`。

#### Scenario: Owner group appears in managed list

- **WHEN** 用户是某群的 owner 并打开选择器「我管理的群组」
- **THEN** 该群出现在列表中且可展开/勾选其用户成员
