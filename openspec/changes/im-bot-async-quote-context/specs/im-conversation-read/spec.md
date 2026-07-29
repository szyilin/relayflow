## ADDED Requirements

### Requirement: ImConversationReadApi for programmatic message read

系统 MUST 在 `relayflow-module-im-api` 暴露 `ImConversationReadApi`（名称可等价），供其他域经 `*-api` 读取会话消息。实现 MUST 在 `im-biz`；调用方 MUST NOT 访问 `im_*` Mapper。API MUST 接受服务端构造的调用主体（含 `tenantId`、触发用户或 bot 身份、以及**允许的** `conversationId`）。模型或不可信入参中的会话 ID 若与允许集合不一致，MUST 拒绝。

#### Scenario: Read message in allowed conversation

- **WHEN** 合法主体请求读取本会话内某 `messageId`
- **THEN** 返回该消息的结构化摘要（至少含 id、seq、senderType、senderId、文本预览、createTime）
- **AND** MUST NOT 返回其他会话消息

#### Scenario: Reject foreign conversationId

- **WHEN** 请求携带的 `conversationId` 或目标消息所属会话不在主体允许范围内
- **THEN** API 拒绝（错误或空结果按实现约定，但 MUST NOT 返回越权内容）

#### Scenario: Nearby and history windows are capped

- **WHEN** 主体请求附近窗口或向前历史
- **THEN** 条数 MUST 受服务端硬上限约束
- **AND** 仅返回允许会话内的消息

### Requirement: Read principal membership checks

`ImConversationReadApi` MUST 校验：触发用户为该会话成员；若以 bot 身份读取，该 bot MUST 为该会话成员（含 bot_dm 对端）。MUST NOT 仅凭数值 ID 放行跨群或他人 bot_dm。

#### Scenario: Non-member cannot read

- **WHEN** 用户不是会话成员却请求读该会话消息
- **THEN** 读取失败且无消息正文泄露
