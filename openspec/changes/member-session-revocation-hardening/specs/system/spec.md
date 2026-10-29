## ADDED Requirements

### Requirement: Current membership and session version gate

系统必须（MUST）在受保护请求的认证边界校验 token 签名、有效期、jti 吊销状态、当前租户真实成员关系与成员会话版本，再进行权限判断。成员状态必须来自权威数据，不能由缓存的角色集合或默认租户代替。签发 JWT 的所有入口必须携带目标成员当前 `member_ver`。

#### Scenario: Disabled member with an unexpired token
- **GIVEN** 成员停用事务已提交且其 token 尚未过期
- **WHEN** 使用该 token 发起需要登录但不额外要求 RBAC 的请求
- **THEN** 返回 HTTP 403 与明确成员失效错误，不执行业务操作

#### Scenario: Missing membership is not defaulted
- **GIVEN** token 对应 tenantId / userId 没有有效成员关系
- **WHEN** 在单租户或多租户模式下访问受保护 API
- **THEN** 返回 HTTP 403，不切换为默认租户身份

#### Scenario: Revoked or mismatched token
- **GIVEN** token 已吊销，或 ACTIVE 成员的 token 版本与数据库版本不同
- **WHEN** 请求受保护 API
- **THEN** 返回 HTTP 401，不能通过重新加载角色恢复该 token

#### Scenario: Required identity service unavailable
- **GIVEN** 吊销存储或成员权威查询不可用
- **WHEN** 校验受保护请求
- **THEN** 返回可重试的 HTTP 503，不降级放行，不暴露 token 或内部堆栈

#### Scenario: Explicit public entry remains public
- **GIVEN** 请求既有登录、注册、租户公开信息、public 文件或独立验证的 webhook 入口
- **WHEN** 未提供有效成员 token
- **THEN** 保持该入口原有匿名或独立签名规则，不授予其他私有能力

### Requirement: Atomic member session invalidation

系统必须（MUST）在 ACTIVE 成员变为非 ACTIVE 的同一事务内原子递增持久会话版本；重新启用不得降低版本。资料更新不得回写旧状态或旧版本，重复停用不得额外递增。

#### Scenario: Reenable does not revive old credentials
- **GIVEN** 版本为 N 的 token 已签发，随后成员停用再启用
- **WHEN** 分别使用旧 token 和重新登录签发的 token
- **THEN** 旧 token 被拒绝，新 token 使用数据库当前版本且正常访问

#### Scenario: Concurrent profile and status updates
- **GIVEN** 停用事务与读取了旧行的昵称或头像更新并发
- **WHEN** 两个事务以任一次序提交
- **THEN** 已提交的停用和版本递增保持，不被资料更新覆盖

#### Scenario: Retry and rollback
- **GIVEN** 同一成员重复停用，或首次停用事务回滚
- **WHEN** 检查提交后的成员行
- **THEN** 重复停用不多次递增，回滚不留下版本递增或部分状态变更

#### Scenario: Invalidation stays within tenant
- **GIVEN** 用户同时在两个租户拥有 ACTIVE 成员关系
- **WHEN** 其中一个租户停用该用户
- **THEN** 只使该租户成员会话失效，另一租户合法 token 继续有效

### Requirement: Legacy token transition and truthful logout

系统必须（MUST）将缺少 `member_ver` 的旧 JWT 仅视为版本 0，在成员仍 ACTIVE 且持久版本为 0 时允许用至原有效期；不得延长 exp 或清除原 jti 吊销记录。有效 token 的登出必须完成服务端吊销后才返回吊销成功。

#### Scenario: Legacy active member token
- **GIVEN** 旧 JWT 无版本字段，成员 ACTIVE 且版本为 0
- **WHEN** 升级后请求受保护接口
- **THEN** 在原 exp 内仍可访问；停用递增后即使重新启用也不能恢复

#### Scenario: Every token issuer uses authoritative version
- **GIVEN** 登录、注册、接受邀请或企业切换需要签发 token
- **WHEN** 签发目标租户的访问令牌
- **THEN** `member_ver` 来自该目标成员关系，不接受客户端指定版本

#### Scenario: Logout storage failure
- **GIVEN** 有效 token 的 jti 吊销写入失败
- **WHEN** 调用登出
- **THEN** 返回明确失败，不能吞异常伪装服务端注销成功

#### Scenario: Logout retry with invalid token
- **GIVEN** token 已过期、已吊销或无效
- **WHEN** 重复登出
- **THEN** 保持幂等，不重新激活任何凭据
