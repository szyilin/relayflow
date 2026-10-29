## Context

覆盖 A2 / A3。`UserServiceImpl.updateUserStatus` 只改成员状态；`JwtAuthenticationFilter` 只验 JWT / jti 吊销及权限加载；WS handshake 只验 JWT。已建立 WS 依赖保存的 tenantId / userId，不复核失效身份。探针已确认同一已吊销 token 的 HTTP 与 WS 认证结果不同。本 change 为无新增 UI 的平台修复，唯一运行时仍为 relayflow-server。

## Goals / Non-Goals

**Goals:** 停用事务提交后，后续受保护请求和实时读写拒绝该成员；重新启用必须重新登录；登出 jti 对 HTTP / WS 一致；有效成员和其他租户登录态不受误伤。

**Non-Goals:** 不更换 JWT、引入刷新 token / httpOnly Cookie、重构账号 Dock、实现跨实例即时踢人消息总线或 P2 断线补拉；已经签出的对象存储 URL 不由本 change 撤销。

## Decisions

### D1. 统一身份有效性 SPI，成员状态来自数据库

- 在 security starter 定义验证 SPI / 结果模型，system-biz 实现读取当前 tenantId + userId 的成员状态与会话版本。HTTP 和 websocket starter 依赖同一 SPI，禁止 starter 依赖 system-biz，也不直查 sys_*。
- 验证顺序：签名 / exp / 必需 claims → jti 吊销 → 当前成员 ACTIVE → 会话版本相等 → 当前权限。V1 单租户也查真实成员关系，不以 default-id 或“有 JWT”替代。
- 本轮采用有索引的成员查询作为权威路径，不复用 30 分钟权限缓存来判断停用。校验组件缺失或 Redis / DB 不可用不能按身份有效放行。
- 仅对需要登录的用户请求执行成员门禁；显式匿名的登录、注册、租户默认信息、public 文件、RTC webhook 等保持既有独立规则。公开端点携带失效 Bearer 不授予私有能力。

### D2. 成员会话版本避免重新启用复活旧 token

- 规划新增 `sys_tenant_user.auth_version BIGINT NOT NULL DEFAULT 0`；JWT 新增 `member_ver`，所有签发入口（login、register、tenant switch、invite accept）从目标成员关系读取版本，不信任客户端传入值。
- ACTIVE → 非 ACTIVE 在同一数据库事务中原子递增 auth_version；重新启用保持已递增版本。停用回滚不改变版本；同一停用请求重复执行不重复增加，也不能因并发资料更新把状态 / 版本写回旧值。
- 状态 / 版本写使用专用 ExtMapper 条件更新；普通成员昵称、头像等更新仅写允许字段。codegen 后对 auth_version 的普通 `updateById` 回写作保护，不能为了方便在所有修改上增加会话失效。
- 老 JWT 缺 `member_ver` 仅按版本 0 处理：数据库仍为 0 的 ACTIVE 成员继续使用到原 exp；一旦发生停用递增即永久失配。新签发统一带版本，不延长旧 exp，不清除已有 jti 吊销记录。
- 相比仅检查 ACTIVE，这个增量字段可避免恢复账号后旧凭据重新生效；相比 Redis 独立版本，数据库事务不会因 Redis 数据丢失把版本重置。

### D3. WS 保存最小验证元数据并在投递边界检查

- 握手使用同一验证器，Session 保存 tenantId、userId、jti、exp、member_ver，不保存原始 Bearer 或放入日志。
- 每次上行路由前、每次下行实际发送前重新验证；失效时停止处理 / 推送并关闭该 Session，不能只在 ping 中校验。Fanout 的本地最终发送分支同样检查，其他活跃用户不受影响。
- 每 30 秒默认巡检空闲连接，清除过期 / 吊销 / 停用 Session 及在线状态；这只是资源清理兜底，提交停用后的新消息不能等待巡检才拦截。关闭去重，兼容已有 afterConnectionClosed 逻辑。
- 登出以 jti 为粒度关闭对应 token 的所有本地连接；成员停用以 tenantId + userId + 版本为边界，不能注销同一账号其他企业会话。

### D4. 失败契约

| 条件 | HTTP / 握手 | 已建立 WS |
|---|---|---|
| token 无效、过期、已吊销、ACTIVE 成员版本不匹配 | 401，统一认证错误 | 不路由 / 不发送，关闭 |
| 成员不存在或非 ACTIVE | 403，明确成员失效业务码 | 不路由 / 不发送，关闭 |
| 权限不足 | 保留原 403 | 业务域现有授权保持 |
| 必需身份状态服务不可用 | 503，可重试，不降级放行 | 暂停处理并关闭，前端可重连 |

实施先冻结错误码映射；成功仍 code=0。无效 / 已过期 token 的重复 logout 保持幂等；有效 token 的吊销写入失败必须明确失败，不能捕获后返回服务端注销成功。客户端本地清凭据与服务端吊销结果分别描述，不暴露 token / 堆栈。

## Risks / Trade-offs

- 每次实时投递增加状态读取 → 先保证正确性，评估索引与实际请求数；不以全局旧缓存换取停用窗口。
- 与停用并发的已开始请求 / 已在途网络帧 → 保证验证边界之后的新请求 / 新投递拒绝；不承诺回滚已经执行的操作或撤回客户端已收到的数据。
- 长连接频繁关闭 / 重复清理 → 断开操作幂等，在线状态按同用户剩余有效连接计算。
- 会话版本回退 / SQL 丢更新 → 专用条件 SQL，覆盖停用、恢复与并发资料更新的真实数据库测试。

## Migration Plan

1. 实施前按 AGENTS.md 确认新增 Flyway；读 CONTRIBUTING / database / codegen 约定，选择当时合法的下一迁移序号，不修改历史脚本。
2. 先增量列并回填 0，使用 CLI codegen 到临时目录后 diff 合并；已在获得明确许可后完成隔离库迁移与 CLI codegen diff；证据见 verification.md。
3. 维护窗口统一升级 token 签发、HTTP 和 WS 校验；断开旧长连接以便重新握手，验证版本 0 的合法旧 JWT 可过渡。
4. 回滚不能直接恢复旧认证代码：其不识别版本 / 吊销一致性。备份恢复或向前修复；如必须回退，需维护窗口轮换 JWT 签名密钥使所有旧 token 失效，并保留数据库版本列，不自动降级 / 删列。

## Open Questions

无阻塞规划的产品问题；建议采用“重新启用须重新登录”。实施前仍需确认新增迁移许可与实际错误码编号。无新端口 / 外部服务依赖；巡检默认 30 秒，如暴露配置需记录属性和环境变量，不能改变安全验证边界。
