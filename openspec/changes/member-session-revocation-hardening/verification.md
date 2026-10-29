# 实施与验证记录

日期：2026-10-29。范围仅为本 change；其余三个 P1 change 未实施。用户已明确允许新增非破坏性 Flyway，以及在隔离测试库执行迁移和 codegen。未改历史迁移、版本号、web 源码或依赖。

## 入口盘点与失败契约（任务 1.1）

| 边界 | 原问题 | 实施后的规则 |
|---|---|---|
| `AuthServiceImpl` 登录；`AuthRegisterServiceImpl` 注册；`TenantServiceImpl` 企业切换；`MemberInviteServiceImpl` 接受邀请 | 签发时没有持久成员版本 | 四处都调用 `JwtTokenService.createAccessToken`；该方法通过 `MemberIdentityProvider` 查目标企业 ACTIVE 成员版本，统一写 `member_ver` |
| `UserServiceImpl.updateUserStatus` | 读旧行后 `updateById` 只写状态，恢复可复活旧凭据 | ExtMapper 在同一 SQL/事务更新状态，仅 ACTIVE → 非 ACTIVE 时递增版本；重复停用不递增 |
| 个人资料与管理端昵称更新 | 整行回写可能恢复旧状态 | ExtMapper 只更新本次提供的资料字段；DO 的 status/authVersion 均禁止普通更新回写 |
| 注册时接受待加入邀请、旧邀请接口、邀请卡片、组织邀请消费 | 整行回写成员状态 | 条件状态更新；待加入的激活要求原状态匹配，保留已有版本 |
| HTTP JWT filter | 吞掉状态存储故障，未查真实成员 | 签名/exp/claims → jti → ACTIVE 成员 → 版本 → 权限；专用身份 SQL 禁用二级缓存并刷新一级缓存；缺组件/服务失败拒绝 |
| logout | 捕获全部异常后伪成功，关闭业务缓存会跳过吊销 | 无效/过期 token 幂等；有效 token 吊销写入异常返回 503；吊销 Redis 与业务缓存开关独立 |
| WS 握手；`InfraWebSocketHandler` 上行；Local sender 和定向 session sender 下行 | 握手只验 JWT，连接后未复核 | 复用 `IdentityValidityService`；属性只保留解析后的身份元数据，不存原始 Bearer；实际发送的唯一 `session.sendMessage` 位于 guard 之后 |
| Redis fanout 发布本地分支及远端订阅分支 | 可能向失效连接继续投递 | 都进入同一个 Local sender 的最终发送门禁 |
| 空闲连接及 close 回调 | 仅等待客户端心跳，重复清理容易误伤 | 默认 30 秒巡检；注册表移除、关闭和在线清理幂等；保留同用户其他有效 jti/其他租户连接 |

错误映射已固定：

| 条件 | HTTP/握手 | code |
|---|---|---|
| 无 token、无效/过期/吊销 token、ACTIVE 成员版本不符 | 401 | 2000001002 |
| 成员不存在或非 ACTIVE | 403 | 2000001004 |
| 既有 RBAC 权限不足 | 保留 403 语义 | 2000001003 |
| 必需身份状态服务/组件不可用，或有效 token 吊销写失败 | 503，可重试 | 2000001005 |

既有匿名入口保持独立规则：`/admin-api/system/auth/login`、`/admin-api/system/tenant/default`、`/app-api/system/auth/register`、`/app-api/infra/file/public/**`、`/app-api/rtc/webhook`；`/infra/ws` 的 HTTP Upgrade 自行走同一身份验证器。logout 允许不依赖有效登录身份进入，以实现无效 token 的幂等重试，但自身只接受经签名验证的 token 进行吊销。匿名入口忽略 Bearer，不由其授予私有身份。

解析异常不记录 token/claims；身份门禁仅返回固定安全文案，登出也不输出原始异常。浏览器原生 WS 错误可能显示原有 query URL，应用不额外记录 Bearer。

## 数据库与 codegen（任务 1.2 / 1.3）

- 在专用容器 `relayflow-member-pg`（PostgreSQL 16，宿主端口 55433，库 `member_auth_test`）执行迁移，未连接业务数据库。
- `V0.1.0.2__add_member_auth_version.sql` 增加 `BIGINT NOT NULL DEFAULT 0`；先运行原 foundation，再升级到新版本，既有行读取为 0。
- 用现有 CLI 执行：

```sh
RELAYFLOW_CODEGEN_JDBC_URL=jdbc:postgresql://localhost:55433/member_auth_test \
RELAYFLOW_CODEGEN_JDBC_USER=relayflow \
RELAYFLOW_CODEGEN_JDBC_PASSWORD="$RELAYFLOW_AUTH_TEST_JDBC_PASSWORD" \
./scripts/codegen.sh --module system --tables sys_tenant_user --migrate \
  --output /tmp/relayflow-member-codegen
```

实际输出：`Successfully applied 2 migrations ... now at version v0.1.0.2`。临时三件套均与 src 做 diff；基础 Mapper 接口只有生成日期变化，无需合入；DO 合入版本字段和更新保护；XML 同步列映射。业务 SQL 只在 ExtMapper。

`MemberAuthPostgresTest` 使用真实 PostgreSQL/MyBatis/JDBC 事务，4 项全部通过：停用重试/回滚/恢复及跨租户隔离；旧行普通 updateById 不回写状态和版本；并发资料与停用分别先提交时均保留版本递增，且只写指定资料字段；同一事务内多次身份读取必须看到另一事务已提交的停用/恢复，不能使用 MyBatis 一级缓存。

## 自动化回归（任务 3.2）

本 change 相关回归 **37 项通过，0 failure / 0 error / 0 skipped**，其中真实 PostgreSQL 测试 4 项。运行：

```sh
RELAYFLOW_AUTH_TEST_JDBC_URL=jdbc:postgresql://localhost:55433/member_auth_test \
RELAYFLOW_AUTH_TEST_JDBC_USER=relayflow \
RELAYFLOW_AUTH_TEST_JDBC_PASSWORD="$RELAYFLOW_AUTH_TEST_JDBC_PASSWORD" \
./mvnw -pl relayflow-module-system/relayflow-module-system-biz,relayflow-module-infra/relayflow-module-infra-biz,relayflow-framework/relayflow-spring-boot-starter-websocket -am test \
  '-Dtest=*Auth*,*UserServiceImplInvite*,*MemberInviteAccept*,*TenantServiceImpl*,*InfraWebSocketIdentity*,*IdentityBoundary*,*WebSocketSessionRegistry*,*RedisWebSocketMessageSender*' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

| 测试类 | 通过数 |
|---|---:|
| IdentityBoundaryTest | 11 |
| WebSocketSessionRegistryTest / RedisWebSocketMessageSenderTest | 1 + 1 |
| InfraWebSocketIdentityTest | 1 |
| AuthServiceImplTest / AuthRegisterServiceImplTest | 7 + 4 |
| MemberAuthPostgresTest | 4 |
| UserServiceImplInviteTest / MemberInviteAcceptCardHandlerTest / TenantServiceImplTest | 5 + 1 + 2 |

覆盖旧 token 版本 0、签发目标版本、停用/恢复、吊销/过期/缺少 claims、匿名例外、权限/DB/Redis/组件缺失故障、握手错误类别、普通上行与 ping 不路由、local/Redis 实际下行不发送、空闲失效清理、同 jti 多连接与其他 jti/租户隔离；最后有效连接关闭时，尚未巡检的过期连接不能保留在线状态。

另已通过：

- `./mvnw -pl relayflow-server -am compile`
- `openspec validate member-session-revocation-hardening --strict`
- `git diff --check`

扩大模块测试时有**既有无关失败**：`TenantBootstrapServiceTest.bootstrapOwner_grantsAdminPermissions` 的 `UnnecessaryStubbingException`（测试第 144、146 行）。已将修改前 HEAD 通过 `git archive HEAD` 解到临时目录，单独运行同一测试，复现相同错误。本 change 未修改该测试或其权限初始化实现；不将扩大测试记为通过。

首次沙箱内 Mockito 无法 attach，已在允许本机测试进程/隔离数据库连接的环境重新执行并通过上述 37 项；这不是代码回归。

## 真实客户端验收（任务 3.1）

测试后端：多租户 `localhost:18081`、单租户 `localhost:18082`；连接上述专用 PostgreSQL 和 `relayflow-member-redis`（Redis 7，端口 56380）。关闭 XXL-JOB/存储启动探测。Redis 和 Hikari 连接超时设为 1 秒用于故障注入；巡检临时设为 1 秒加快客户端验收，生产默认仍为 30 秒。关闭业务缓存后也完成同样验收，吊销检查仍有效。

可复用验收脚本：

- `scripts/tests/fixtures/member-session-revocation.mjs`：真实注册、登录、企业切换、WS 握手和 ping；同 jti 两连接退出后清理；其他 jti/租户继续可用；旧版本 0 JWT 过渡；空闲过期关闭；停用后 HTTP 403、WS 拒绝，IM 消息计数和资料未变；恢复后旧 token 401、新登录版本 1；Redis 在线记录保留/清除符合剩余有效连接。
- `scripts/tests/fixtures/member-session-state-failures.mjs`：单租户不默认缺失成员；分别停止并恢复专用 Redis 和 PostgreSQL，真实 HTTP/握手均 503，已连接 WS 关闭；Redis 故障时有效 token logout 503、无效 token logout 幂等 200。

两个脚本均已实际运行通过。运行前显式设置 `RELAYFLOW_AUTH_TEST_BASE_URL`、`RELAYFLOW_AUTH_TEST_PG_CONTAINER`、`RELAYFLOW_AUTH_TEST_JWT_SECRET`；首个脚本可通过 `RELAYFLOW_AUTH_TEST_BROWSER_STATE` 保存仅含测试账号/密码/ID、不含 token 的临时浏览器夹具，第二个脚本读取该路径。仅允许指定的专用容器和 localhost 测试实例，不用于业务环境。

浏览器使用现有 `web/`，`VITE_API_BASE_URL=http://localhost:18081`，访问 `http://127.0.0.1:15173/app/login`：验证登录 → 多企业选择 → `/app/messages`、添加另一账号及 Account Dock 切换。实时连接仍走 `/infra/ws`。本 change 没有修改前端源码。

## 维护窗口升级与回退

1. 备份数据库；在维护窗口先执行新增迁移，再统一升级 token 签发、HTTP 和 WS 实现，断开升级前旧长连接使其重新握手。不可将新旧认证二进制长期混跑。
2. `auth_version=0` 的 ACTIVE 成员可继续使用缺少 member_ver 的旧 JWT 至其原 exp；不延长旧 exp，不清除现有吊销记录。首次停用即递增；恢复成员不降低版本，要求重新登录。
3. 配置 `relayflow.websocket.identity-scan-interval-millis` / 环境变量 `RELAYFLOW_WEBSOCKET_IDENTITY_SCAN_INTERVAL_MILLIS`，默认 30000。只影响空闲资源回收；每次收发仍独立授权。故障时在线键清理可由 Redis TTL 兜底，连接立即停止处理。
4. 旧二进制不识别本保护，不能无条件回退。优先向前修复；如必须回退，在维护窗口轮换 JWT 签名密钥使全部旧 token 失效，保留版本列，不自动删列或降低版本。
5. 不承诺撤销校验前已开始的业务、已在途帧或既有签名存储 URL；本 change 的授权边界是每次受保护请求/上行路由/实际下行发送之前。
