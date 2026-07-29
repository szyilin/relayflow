## ADDED Requirements

### Requirement: Create document from workspace create hub

当用户从工作台 Create Hub 选择「创建文档」时，系统 MUST 使用既有个人文档库创建能力新建 RICH_DOC，并打开编辑视图。MUST NOT 为此入口单独引入第二套文档存储模型。

#### Scenario: Hub creates library document

- **WHEN** 有效成员从「+」选择「创建文档」
- **THEN** 系统创建文档库节点/对象成功并进入该文档编辑界面
