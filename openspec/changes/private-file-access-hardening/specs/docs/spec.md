## ADDED Requirements

### Requirement: Drive file download follows current object ownership

系统必须（MUST）根据现有 `doc_object` 绑定与 FILE 对象当前所有权判断 private 文件可读性：对象必须 ACTIVE 且未逻辑删除，用户必须具备该 FILE 对象当前合法所有权。不得把仅适用于 RICH_DOC 的分享规则直接当作 FILE 的授权。

#### Scenario: Current owner downloads
- **GIVEN** private 文件绑定当前用户拥有的 ACTIVE FILE 对象
- **WHEN** 通过云盘或成员文件入口请求下载
- **THEN** docs 策略通过，授权后才签发链接

#### Scenario: Ownership transferred or object deleted
- **GIVEN** FILE 已转移给另一成员，或已删除 / 非 ACTIVE
- **WHEN** 原上传者凭旧 fileId 或旧绑定请求下载
- **THEN** 该引用不授予读取权；当前新所有者仅在对象有效时可读

#### Scenario: Invalid file registration cannot create access
- **GIVEN** 成员对一个 private fileId 没有引用资格
- **WHEN** 尝试将其登记到自己的云盘
- **THEN** 拒绝登记，不能通过新建 FILE 对象获取他人文件权限
