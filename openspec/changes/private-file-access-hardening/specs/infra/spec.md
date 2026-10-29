## ADDED Requirements

### Requirement: Member private file authorization before signing

系统必须（MUST）在签发 private 文件读 URL 之前验证当前有效身份、租户、未删除文件及业务可读性。成员入口不得委托管理员入口绕过授权；管理端仍须 `infra:file:download`。public 下载保持原有规则。

#### Scenario: Knowing another member fileId grants nothing
- **GIVEN** 同租户用户 B 知道 A 的无业务绑定 private fileId
- **WHEN** B 请求成员下载
- **THEN** 返回明确访问拒绝，不生成或返回签名 URL

#### Scenario: Unbound uploader preview
- **GIVEN** private 文件无业务绑定且创建者为当前有效成员
- **WHEN** 该创建者请求上传完成后的本人预览
- **THEN** 可获得既有短效下载 URL；创建者缺失时不能推定全员可读

#### Scenario: Bound file requires current domain access
- **GIVEN** private 文件具有业务绑定
- **WHEN** 原上传者或任意成员请求下载
- **THEN** 至少一个已识别且当前可读的业务引用通过才签名，不能单凭 creator 绕过

#### Scenario: Unknown or stale binding
- **GIVEN** 只有未知类型、失效或不可读的绑定
- **WHEN** 请求下载
- **THEN** 拒绝，不回退为租户全员授权；策略异常不能改走管理入口

#### Scenario: Multiple legitimate references
- **GIVEN** 同一文件的一个引用失效，另一个仍允许当前用户读取
- **WHEN** 请求成员下载
- **THEN** 允许按仍有效的引用下载，不受旧无效引用阻断

#### Scenario: Tenant and entry boundaries
- **GIVEN** 请求跨租户文件，或只有管理员角色但通过成员路径访问不可读文件
- **WHEN** 校验下载
- **THEN** 跨租户不泄露存在性；管理员角色不自动成为成员路径的授权依据

#### Scenario: Revocation respects signed URL boundary
- **GIVEN** 业务权限已撤销
- **WHEN** 用户请求新的签名 URL
- **THEN** 按当前权限拒绝；已签出的 GET URL 仅保证在既有最长 15 分钟 TTL 后过期，不承诺即时撤回

### Requirement: Trusted file bindings and history repair

系统必须（MUST）通过 infra-api 策略契约让领域判断其自身引用的可读性，业务 ID 与租户由服务器确定。不得增加可任意创建授权绑定的公开接口。历史修复必须来自各领域实际引用，默认预演、显式执行、幂等且可分批恢复。

#### Scenario: Business binding failure is atomic
- **GIVEN** 一次业务提交需要持久化资源及文件绑定
- **WHEN** 绑定失败
- **THEN** 业务事务回滚且不发成功 ACK，不能以异步尽力补写替代

#### Scenario: History preview and replay
- **GIVEN** 存在已验证的历史消息附件与云盘引用
- **WHEN** 执行 dry-run、实际补绑定及再次执行
- **THEN** 预演不写数据，执行只补可靠引用，重跑不重复；输出计数和异常 ID，不输出正文、密钥或签名链接

#### Scenario: Ambiguous or cross-tenant historical reference
- **GIVEN** 引用不可靠、文件不存在或租户不一致
- **WHEN** 补绑定扫描处理该引用
- **THEN** 记录异常并要求核对，不扩大权限或静默制造绑定
