## MODIFIED Requirements

### Requirement: 租户可配置存储 Provider

系统必须（MUST）允许租户在管理端配置对象存储 Provider 并显式选择新上传的生效来源。保存未启用配置不得改变当前上传目标。历史文件必须按持久化存储位置版本读取，不能仅凭 `storage_uri` / `provider` 解析为当前同名配置。定位改变必须建立新位置；历史引用与原对象 key 必须保留。

#### Scenario: 保存 MinIO 租户配置
- **GIVEN** 管理员提交有效 MinIO 参数
- **WHEN** 保存租户配置但未启用
- **THEN** 密钥加密持久化，配置可随后设为默认；当前上传与已有文件读取不变

#### Scenario: 切换默认 Provider 保留历史
- **GIVEN** 租户已配置另一默认目标或同一 Provider 的新存储位置
- **WHEN** 管理员显式启用该目标
- **THEN** 新建上传会话使用新目标，旧文件和已有会话继续使用各自绑定的位置

#### Scenario: 删除仍被引用的 Provider
- **GIVEN** 位置仍被文件或未过期上传会话引用
- **WHEN** 管理员尝试删除或普通保存改写其定位
- **THEN** 拒绝并返回明确引用错误；停止新上传不得阻断历史读取

#### Scenario: 测试存储连通性
- **GIVEN** 管理员调用存储测试连接 API
- **WHEN** 提交或引用已保存参数
- **THEN** 探测连通性，不把密钥写入审计日志或响应

#### Scenario: 存储配置 API 权限不足
- **GIVEN** 用户缺少对应 `infra:storage:*` 权限
- **WHEN** 调用存储配置 API
- **THEN** 返回 HTTP 403，不泄露配置凭据

## ADDED Requirements

### Requirement: Upload session and file pin one storage location

系统必须（MUST）在签发 PUT 前给上传会话绑定租户内持久存储位置版本；确认必须按该位置 HEAD，文件必须继承同一引用。所有实际对象操作必须沿用绑定，不能重新读取当前默认来源。下载必须先通过文件授权，再解析位置及签名。

#### Scenario: Save inactive tenant configuration
- **GIVEN** 当前生效为 Bootstrap A，已保存但未启用 tenant B
- **WHEN** 创建上传会话、确认并下载该文件
- **THEN** 全程使用 A，不能在下载时因存在 B 配置而改走 B

#### Scenario: Switch between presign and confirm
- **GIVEN** 会话已绑定 A 且对象 PUT 到 A
- **WHEN** 默认切换 B 后确认该会话
- **THEN** 在 A 做 HEAD 并登记 A 引用，不因 B 中对象不存在而失败或错误确认 B 对象

#### Scenario: Change endpoint bucket or prefix
- **GIVEN** 文件已绑定 A，其完整 objectKey 已包含原前缀
- **WHEN** 管理员创建并启用 endpoint / bucket / prefix 不同的新配置
- **THEN** 旧文件仍读 A，使用原完整 key，不追加当前 prefix、不修改 fileId 或 storage_uri

#### Scenario: Missing cross-tenant or unresolved binding
- **GIVEN** 文件位置引用不存在、跨租户或尚未解析
- **WHEN** 执行下载或其他对象操作
- **THEN** 返回明确可恢复错误，未解析为 `STORAGE_LOCATION_UNRESOLVED`，不尝试当前默认存储或跨租户查找

### Requirement: Storage location identity and credential rotation

系统必须（MUST）将来源、endpoint / bucket 等命名空间定位与凭据轮换分开。普通保存不得改变被引用的位置；显式同位置凭据轮换必须验证定位不变、连接有效并说明影响范围。凭据必须加密存储且不出现在日志、迁移报告或普通响应中。

#### Scenario: Rotate credentials of the same location
- **GIVEN** 位置 A 已被历史文件引用
- **WHEN** 管理员显式轮换 A 的凭据且连接验证成功
- **THEN** 既有引用保留并使用新凭据，提示影响 A 的历史文件；不能借轮换改到其他 endpoint / bucket

#### Scenario: Managed internal and public routing
- **GIVEN** 绑定位置属于已确认的受管命名空间，Docker 浏览器访问端口改变
- **WHEN** 生成 PUT 或 GET 签名
- **THEN** 在签名前应用当前 internal / public 路由，保留位置 bucket、region、key 和选定凭据；不在签名后换 host

#### Scenario: Independent storage keeps its routing
- **GIVEN** 绑定位置为独立对象存储
- **WHEN** 平台本地 endpoint 配置变化
- **THEN** 不强行用平台地址替换独立位置，不把浏览器 localhost 端口持久化为对象身份

### Requirement: Explicit and replayable legacy storage binding

系统必须（MUST）对旧文件提供默认只读 dry-run 与显式映射计划，按 tenant / source / bucket / 时间范围记录候选、未解析和异常数量。原 provider / URI 或一次 HEAD 成功不足以唯一证明历史 endpoint；操作者必须提供原配置并确认映射。执行只填仍为空且满足确认范围的引用，必须幂等、可分批恢复且不复制或删除对象。

#### Scenario: Ambiguous history is not guessed
- **GIVEN** 多个位置有同名 bucket / key，或历史 endpoint 已丢失
- **WHEN** 执行 dry-run
- **THEN** 报告多义或未恢复，不能通过试读当前配置自动宣称迁移成功

#### Scenario: Confirmed mapping can resume
- **GIVEN** 已备份数据并确认输入范围和目标位置
- **WHEN** 分批绑定中断后重跑
- **THEN** 只补符合计划的空引用，不覆盖已绑定记录，原对象字节、key 与业务引用保持不变

#### Scenario: Old pending upload cannot be identified
- **GIVEN** 旧上传中会话无法确定原位置
- **WHEN** 升级后尝试继续上传或确认
- **THEN** 明确要求过期 / 重建，不重新绑定到当前默认位置

#### Scenario: Upgrade verification precedes strict reads
- **GIVEN** 准备部署严格位置解析
- **WHEN** 在副本完成预演与所需历史映射核对
- **THEN** 维护窗口中再启用新读写；未解析数据明确报告，旧版程序不能继续创建无位置绑定的新会话
