## 1. [平台] 契约与成员版本

- [x] 1.1 盘点所有 JWT 签发入口、成员状态写入、logout 与 WS 收发入口，固定现有漏洞的回归用例和 401 / 403 / 503 错误映射；读权限模型、API、数据库与 codegen 约定，记录不受影响的匿名入口。
- [x] 1.2 按 AGENTS.md 先取得新增 Flyway 的明确许可，再实施 auth_version 非破坏性增量 / 0 回填；CLI codegen 到临时目录并 diff 合入，验证旧数据与旧 JWT 的版本 0 兼容，不改历史迁移。
- [x] 1.3 实现成员状态 / 版本专用原子更新和资料字段局部更新；真实 PostgreSQL 测试覆盖停用重试、事务回滚、恢复后旧 token 失效及并发资料更新，禁止旧行回写状态 / 版本。

## 2. [平台] 统一认证与实时会话

- [x] 2.1 在 security 定义身份有效性 SPI 并由 system 实现权威成员查询；HTTP 按签名、吊销、成员、版本、权限顺序校验，所有签发入口读取目标成员版本，验证单租户、多租户与匿名例外。
- [x] 2.2 logout 正确区分幂等无效 token 与有效 token 吊销写入失败；加入 Redis / DB 故障回归，确保不伪成功或失效放行，日志无 token。
- [x] 2.3 WS 握手、上行路由与所有实际下行发送复用验证器，Session 保存必要元数据；验证 local / Redis fanout 的投递边界，停用、过期、吊销或状态服务失败时停止处理并关闭。
- [x] 2.4 增加默认 30 秒空闲巡检及幂等清理；验证同 jti 多连接、其他 jti / 其他租户有效连接和在线状态，不以巡检替代收发授权。

## 3. [平台] 集成与升级验收

- [x] 3.1 以真实 HTTP / WS 客户端覆盖登录、退出、旧 token、停用恢复及跨租户隔离；验收现有 `/app/login`、工作台多账号切换与 `/infra/ws`，确保被拒绝的业务无落库 / 无新推送，记录维护窗口升级与回退限制。
- [x] 3.2 运行本 change 涉及模块的自动化回归、`./mvnw -pl relayflow-server -am compile` 与 `openspec validate member-session-revocation-hardening --strict`，保存验证证据；已有无关全量测试失败单独说明，不将其记为本 change 通过。
