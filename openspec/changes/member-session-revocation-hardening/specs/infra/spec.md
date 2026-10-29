## ADDED Requirements

### Requirement: WebSocket identity validity matches HTTP

系统必须（MUST）在 `/infra/ws` 握手、每条上行路由前及每次实际下行发送前复用 HTTP 的成员状态、会话版本、过期和 jti 吊销规则。Session 只保存必要验证元数据，不保存或记录原始 Bearer。

#### Scenario: Revoked token cannot handshake
- **GIVEN** 同一 token 已被 HTTP 吊销校验拒绝
- **WHEN** 使用它连接 `/infra/ws`
- **THEN** 握手返回与 HTTP 同类的 401；成员失效为 403，状态服务不可用为 503

#### Scenario: Existing session cannot send after disable
- **GIVEN** WS 已连接，随后其成员被停用且事务已提交
- **WHEN** 该连接发送普通业务消息或 ping
- **THEN** 不执行业务路由或刷新有效在线状态，并关闭失效连接

#### Scenario: Existing session cannot receive after logout
- **GIVEN** WS 使用的 jti 已吊销
- **WHEN** local 或 Redis fanout 分支准备向该连接实际发送
- **THEN** 不发送载荷并关闭连接，不能等下一次 ping 才阻止投递

#### Scenario: Expired or unverifiable existing session
- **GIVEN** token 已到期或必须的身份状态服务不可用
- **WHEN** 进入任一 WS 收发验证边界
- **THEN** 停止本次处理并关闭连接，不使用旧验证结果放行

### Requirement: Invalid idle session cleanup is bounded and scoped

系统必须（MUST）以默认 30 秒巡检周期清理空闲失效连接；巡检只能作为资源回收兜底，不能代替收发授权。关闭和在线状态清理必须幂等，并按其余有效连接计算用户在线状态。

#### Scenario: Idle token revoked
- **GIVEN** token 已吊销且连接没有新收发
- **WHEN** 下一轮巡检执行
- **THEN** 关闭该 jti 的本地连接并清理其在线记录，不等待客户端心跳

#### Scenario: Other valid sessions remain
- **GIVEN** 同一账号还有其他有效 jti 或其他租户连接
- **WHEN** 失效连接被重复关闭与回调清理
- **THEN** 不误关其他有效连接，不错误清除它们的在线状态
