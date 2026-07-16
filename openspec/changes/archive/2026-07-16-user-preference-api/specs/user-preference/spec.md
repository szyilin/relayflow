## ADDED Requirements

### Requirement: 企业内用户偏好持久化

系统 MUST 按 `(tenant_id, user_id)` 持久化当前成员的工作台偏好。同一全局账号在不同租户下 MUST 允许不同偏好。表 MUST 使用 JSON 文档存储设置，并包含 `schema_version`。

#### Scenario: 首次读取无行

- **WHEN** 成员从未保存过偏好且调用获取偏好 API
- **THEN** 系统返回与代码默认合并后的完整有效配置
- **AND** MUST NOT 仅为该次读取而强制插入数据库行

#### Scenario: 首次保存

- **WHEN** 成员提交有效的偏好更新
- **THEN** 系统 upsert 该租户下该用户的偏好行
- **AND** 再次 GET 返回合并后的有效配置（含新默认键）

#### Scenario: 跨租户隔离

- **WHEN** 同一用户在租户 A 保存深色主题后切换到租户 B 且 B 无偏好行
- **THEN** 租户 B 的 GET 返回默认（或 B 自己的行），MUST NOT 自动沿用 A 的行

### Requirement: 仅本人可读写当前租户偏好

获取与更新偏好 API MUST 仅作用于 JWT 中的当前用户与当前租户。MUST NOT 提供通过路径参数修改其他用户偏好的 V1 接口。

#### Scenario: 鉴权

- **WHEN** 未登录客户端调用偏好 API
- **THEN** 系统拒绝（与其他 app-api 一致）

### Requirement: 默认数据类别为 C

用户偏好实现 MUST 遵循默认数据惯例的 **C 类**（读合并、写 upsert），MUST NOT 在成员激活时批量预插默认偏好行。

#### Scenario: 成员激活

- **WHEN** 新成员在租户内激活
- **THEN** 系统不因此插入 `sys_user_preference` 行
