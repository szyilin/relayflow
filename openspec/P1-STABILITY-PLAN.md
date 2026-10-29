# P1 稳定性收尾规划

本轮将扫描中确认的 **6 项 P1** 纳入 **4 个独立 change**，现均已完成实现与各自验证，任务已勾选；四项实现及验收记录均纳入版本控制。迁移与历史回填只在获准的隔离库 / 备份副本验证，真实环境升级仍须执行维护窗口与明确映射。

各项证据：[成员会话](changes/member-session-revocation-hardening/verification.md)、[私有文件](changes/private-file-access-hardening/verification.md)、[文档保存](changes/docs-save-consistency-hardening/verification.md)、[历史存储](changes/storage-history-binding-hardening/verification.md)。尚未执行归档，不将完成实现等同于已发布。

## 问题与 change 对照

| 扫描项 | 明确问题 / 触发方式 | 目标行为 | 对应 change |
|---|---|---|---|
| A2 | 成员停用后旧 JWT 仍能访问部分仅要求登录的 API | 当前成员状态 + 持久会话版本校验，恢复成员不复活旧 token | [member-session-revocation-hardening](changes/member-session-revocation-hardening/proposal.md) |
| A3 | HTTP 拒绝已吊销 token，WS 握手仍接受；已连接收发不复核 | HTTP / WS 握手 / 上下行一致，失效停止收发并清理 | 同上 |
| A1 | 同租户成员知道他人 private fileId 即可请求下载 | 当前领域 ACL 授权后才签名，阻止伪造引用制造权限，兼容合法历史附件 | [private-file-access-hardening](changes/private-file-access-hardening/proposal.md) |
| B1 | 打开 / 重命名将旧文档整行写回，覆盖其他窗口的新正文与版本 | 元数据局部更新，正文只经既有 CAS 修改 | [docs-save-consistency-hardening](changes/docs-save-consistency-hardening/proposal.md) |
| B2 | 保存慢响应覆盖继续输入；防抖和导航可能丢失原文档草稿 | 文档隔离、修订号、串行保存、离开保护、冲突保留草稿 | 同上 |
| B3 | 上传走生效 Bootstrap A，下载走已保存但未启用 tenant B；改 endpoint 后旧位置丢失 | 上传会话 / 文件固定位置版本，历史数据显式映射，切换不改旧文件 | [storage-history-binding-hardening](changes/storage-history-binding-hardening/proposal.md) |

## 执行顺序与任务入口

已按 **成员会话 → 私有文件 → 文档保存 → 存储历史** 顺序实施。后续修订仍以各自 tasks 为范围。

| 顺序 | 任务清单 | 依赖 / 工作流 | 规划任务数 |
|---|---|---|---|
| 1 | [成员与 WS 会话](changes/member-session-revocation-hardening/tasks.md) | [平台] 后端先行；统一身份规则是文件授权前置 | 9 |
| 2 | [私有文件授权](changes/private-file-access-hardening/tasks.md) | [平台]；依赖 1；历史补绑定先预演再启用严格校验 | 8 |
| 3 | [文档保存一致性](changes/docs-save-consistency-hardening/tasks.md) | 单 change 前端 → 后端 → 联调；可独立于存储模型推进 | 9 |
| 4 | [历史存储绑定](changes/storage-history-binding-hardening/tasks.md) | 单 change 前端 → 后端 → 联调；下载依赖 2 的授权边界 | 10 |

每个目录都有 proposal、design、specs 与 tasks。具体方案、验收和回滚限制以该 change 为准。本表不新增第五个实施 change，也不替代 tasks。

## 实施与验收边界

- 成员会话已新增 `sys_tenant_user.auth_version`；存储历史已新增位置版本表和引用字段。两项增量迁移均已取得用户明确许可并在隔离库验证，未修改历史脚本。私有文件授权复用既有绑定表；文档保存未改 schema。
- 旧 JWT 按版本 0 受限兼容；旧 WS 在维护窗口重新握手。旧 IM 附件补绑定与旧文件位置映射均先在副本 dry-run、备份、核对，再显式写入。无法证明的历史 endpoint 必须报告未恢复，不能猜测。
- 不复制 / 删除对象，不改 fileId、key 或历史 URI；同位置凭据允许显式轮换，不能永久冻结密钥，也不能借轮换改变命名空间。
- 私有文件撤销阻止新签名，既有 15 分钟签名 URL 不承诺即时失效；仅内存的文档草稿不承诺浏览器强杀后的恢复。验收必须包含这些边界。
- 认证 / 文件使用真实 HTTP / WS 和负向权限验证；文档使用真实 PostgreSQL 并发交错与浏览器慢响应 / 冲突验证；存储覆盖未启用配置、切换期间确认、重启、历史读取及迁移重跑。
- 每项实施完成后运行相关自动化回归、Maven compile、OpenSpec strict；涉及 web 必须 build + typecheck 并记录浏览器路径。全量测试 / lint 的既有失败单独记录，不冒充通过，也不在此轮顺手清理。

## 已确认依据与复查入口

这些是扫描时的具体代码入口，后续实施须将临时探针固化为仓库回归；不依赖临时日志长期存在。

| 项 | 复查入口与已确认现象 |
|---|---|
| A1 | `FileDownloadServiceImpl.resolveMemberDownload` 委托管理下载，`FileServiceImpl.requireFile` 仅限定 tenant / fileId；方法探针确认非创建者在无业务授权检查下取得 URL |
| A2 | `UserServiceImpl.updateUserStatus` 与 `JwtAuthenticationFilter.authenticate`：状态更新和 HTTP 认证之间缺少当前成员有效性门禁 |
| A3 | `JwtWebSocketHandshakeInterceptor` 与 HTTP TokenRevocationStore 路径不一致；同一吊销 token 的 HTTP / WS 对照探针确认握手仍接受 |
| B1 | `DocDocumentServiceImpl.getDocument`、`DocLibraryServiceImpl` 重命名及云盘元数据路径；实际 MyBatis SQL 探针确认旧行更新包含 body / content_version 且无版本条件。正文 saveBody 本身已有 CAS，不将其误报为无 CAS |
| B2 | `web/src/pages/app/docs/index.vue` 的 activeDocument watcher / 防抖 / 打开与返回，`web/src/stores/docs.ts` 的 saveBody 响应回写；代码链路确认新草稿可被旧响应重置，实施时补可控延迟回归 |
| B3 | `StorageProviderServiceImpl.resolveEffectiveProviderConfig` 与 `resolveProviderConfig`；探针确认生效 Bootstrap A 与未启用 tenant B 的解析结果分离，旧 file 又无位置版本引用 |

## 本轮不纳入的 P2

以下保留原扫描分类，不因本轮规划而隐含扩项：C1 智能体流式消息序号并发冲突；C2 上传确认幂等 / 并发重复元数据；D1 IM 断线后的会话 / 未读 / 消息补拉；D2 全量测试与 lint 基线清理。需要实施时分别提案。

已排除的误报：docs 逻辑删除已有 BaseDO 的 TableLogic 保护；本计划不将其列为缺失的安全机制。
