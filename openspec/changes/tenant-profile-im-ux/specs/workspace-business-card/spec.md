## MODIFIED Requirements

### Requirement: 个人名片资料与备注持久化

个人名片的签名、封面、展示昵称与头像 MUST 按当前企业成员持久化（`sys_tenant_user` 成员资料），不得作为全局账号唯一真源。

#### Scenario: 本人资料按企业隔离

- **WHEN** 成员通过 profile 更新提交有效 signature、coverFileId、avatar 或 nickname
- **THEN** 写入当前租户成员资料
- **AND** 切换到其他企业后 MUST 展示该企业成员资料（或回退），不得强制沿用上一企业头像
