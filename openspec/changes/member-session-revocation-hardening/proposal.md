## Why

P1 扫描项 A2 / A3：禁用成员只更新成员表，现有 JWT 仍可通过部分接口；HTTP 会检查 token 吊销记录，WebSocket 握手与已建立连接没有同等校验。需要统一成员状态、成员会话版本及 token 吊销语义，阻止失效身份继续读写和接收推送。

## What Changes

- 在 security 定义身份有效性 SPI，由 system 实现 ACTIVE 成员关系与会话版本检查；HTTP、WS 握手、上行和下行复用同一规则，不以权限缓存代替成员状态。
- 规划 `sys_tenant_user.auth_version`：停用时在同一事务原子递增，签发 JWT 时携带当前版本；重新启用后旧版本 token 仍失效，需要重新登录。
- WS 保存 jti / exp / 会话版本元数据，失效连接停止路由和推送并关闭；定期巡检只负责清理空闲连接，不作为唯一授权检查。
- 明确登录、公开下载、webhook 等匿名入口，以及 401 / 403 / 状态服务不可用的失败语义；登出失败不能伪装成服务端吊销成功。
- **BREAKING**：停用成员与已吊销 token 不再享有此前意外保留的访问能力；原 ACTIVE 成员的合法登录态按兼容规则过渡。

## Capabilities

### New Capabilities

无；归入现有认证与实时基础设施。

### Modified Capabilities

- `system`：成员状态变更、会话版本、签发入口及失效 HTTP 身份的统一处理。
- `infra`：WS 握手、已建立连接和实时投递的持续身份校验。

## Impact

- 类型：**[平台]**，不新增产品页面；涉及 Java 根工程的 security / websocket / redis starter、system-api / system-biz、infra-api / infra-biz 与 server 装配。Starter 仅定义 SPI，不依赖业务 biz。
- `web/`、`deploy/` 不计划改实现；验收现有登录、退出、多账号与租户切换行为。没有新增 Maven / npm 依赖、中间件或端口。
- 需要非破坏性 Flyway 增量与 DO / Mapper codegen diff。**已在实施会话获得新增迁移及隔离测试库验证的明确许可；不修改历史 Flyway。**
- 自部署：升级前备份数据库；回退到旧认证实现会丢失本项保护，不支持无条件回滚旧二进制。旧 JWT 兼容、迁移与停机边界见 design。
- 不纳入 P2 的 IM 断线补拉、智能体并发或全量 lint 清理；本轮按用户指示进入实施；验证记录见 verification.md。
