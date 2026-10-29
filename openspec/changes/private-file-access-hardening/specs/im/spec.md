## ADDED Requirements

### Requirement: Private attachments follow current message access

系统必须（MUST）以服务端生成的 `im_message` 绑定关联实际 private 附件。只有消息仍存在、未撤回、当前内容仍引用文件，且请求者按既有会话与消息规则可读时，才授予该引用的文件下载能力。

#### Scenario: Legitimate recipient reads attachment
- **GIVEN** 已持久化消息包含该附件，当前成员有权读取该消息
- **WHEN** 接收方请求文件下载
- **THEN** 对应消息策略通过并允许签发短效链接

#### Scenario: Recall removal or loss of access
- **GIVEN** 消息已撤回、删除、编辑移除附件，或用户已失去该消息读取权限
- **WHEN** 使用仍存在的旧绑定下载
- **THEN** 该引用不再授权，不能仅因历史绑定存在继续放行

### Requirement: Attachment reference cannot manufacture read access

系统必须（MUST）在发送、转发、编辑及实际 private sticker / Bot 附件路径创建新引用前验证引用主体已有文件读取资格；内部主体必须使用显式领域授权。消息与可信绑定必须在同一业务事务内持久化后才确认。

#### Scenario: Arbitrary fileId in own conversation
- **GIVEN** 发送者无权读取他人 private 文件
- **WHEN** 把该 fileId 放进自己的消息或转发请求
- **THEN** 拒绝提交，不产生可被用作授权的新消息或绑定

#### Scenario: Authorized forward and history
- **GIVEN** 转发主体有原附件读取资格
- **WHEN** 合法转发或对可靠历史引用补绑定
- **THEN** 以实际新消息 ID 建立绑定，后续授权由新消息当前可读性决定；重跑不产生重复绑定
