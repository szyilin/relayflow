## ADDED Requirements

### Requirement: 个人名片资料与备注持久化

系统 MUST 将用户个性签名与封面 fileId 持久化在用户资料中；MUST 将查看者对联系人的备注名与描述按 `(tenant_id, owner_user_id, target_user_id)` 持久化。首次读取无备注行时 MUST 返回空备注且 MUST NOT 仅为读取而插入行。

#### Scenario: 保存本人签名与封面

- **WHEN** 成员通过 profile 更新提交有效 signature 或 coverFileId
- **THEN** 系统持久化到该用户资料
- **AND** 再次 GET 本人或他人资料可见对应公开字段

#### Scenario: 保存联系人备注

- **WHEN** 成员对目标用户 PUT 备注
- **THEN** 系统 upsert 仅属于该查看者的备注行
- **AND** 其他用户 GET 同一目标 MUST NOT 看到该备注
