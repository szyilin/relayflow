## ADDED Requirements

### Requirement: Org-level docs product settings (V1 subset)

系统 SHALL 持久化并执行当前租户的云文档组织级策略（V1 子集由 design 拍板）。工作台 docs 写路径 MUST 以后端策略为准，不得仅依赖前端隐藏控件。

#### Scenario: Effective policy applies
- **WHEN** 租户已保存云文档组织策略
- **THEN** 相关 docs App API / 服务端强制执行该策略
- **AND** 未保存时使用安全默认（与 design 一致）
