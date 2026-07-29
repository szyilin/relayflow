## ADDED Requirements

### Requirement: Org-level calendar product settings (V1 subset)

系统 SHALL 持久化并执行当前租户的日历组织级策略（V1 子集由 design 拍板）。工作台 calendar 相关写路径 MUST 以后端策略为准。

#### Scenario: Effective policy applies
- **WHEN** 租户已保存日历组织策略
- **THEN** 相关 calendar App API / 服务端强制执行该策略
- **AND** 个人日历偏好仍由 `user-preference` 承载，MUST NOT 被本组织设置页直接编辑
