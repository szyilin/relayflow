# 基础设施模块规格（infra）

## 目的

定义文件存储、审计日志、WebSocket 等基础设施能力。

## 需求

### 需求：Bootstrap 默认对象存储启动校验

系统 MUST 在应用启动时校验已配置的默认对象存储；若缺失或不完整，启动 MUST 失败。

#### 场景：MinIO Bootstrap 配置完整

- 给定 `relayflow.storage.default-provider=minio` 且 MinIO 连接参数完整
- 当 应用启动
- 那么 启动成功
- 并且 未配置自定义存储的租户使用此 Bootstrap 配置

#### 场景：缺少默认 provider

- 给定 未设置 `relayflow.storage.default-provider`
- 当 应用启动
- 那么 启动失败并返回明确错误信息

#### 场景：生产环境禁止 local provider

- 给定 非 dev profile 设置 `default-provider=local`
- 当 应用启动
- 那么 启动失败

### 需求：租户可配置存储 Provider

系统 SHALL 允许租户在管理端配置一个或多个对象存储 Provider，指定其中一个为默认上传目标；历史文件按 `infra_file` 记录的 `storage_uri` / `provider` 解析对应 Provider 配置读取。

#### 场景：保存 MinIO 租户配置

- 给定 管理员提交有效的 MinIO 连接参数
- 当 调用存储配置保存 API
- 那么 系统持久化 Provider 配置且密钥加密存储
- 并且 该 Provider 可设为租户默认

#### 场景：切换默认 Provider 保留历史

- 给定 租户将默认 Provider 从 MinIO 切换至另一已配置 Provider
- 当 保存配置
- 那么 新上传使用新默认
- 并且 旧 `provider` 值的已有文件仍可通过旧 Provider 配置读取

#### 场景：删除仍被引用的 Provider

- 给定 Provider 仍被 `infra_file` 行引用
- 当 管理员尝试删除该 Provider 配置
- 那么 系统拒绝删除并返回明确错误

#### 场景：测试存储连通性

- 给定 管理员调用存储测试连接 API
- 当 提交或引用已保存参数
- 那么 系统探测连通性
- 并且 测试请求中的密钥 MUST NOT 写入审计日志

#### 场景：存储配置 API 权限不足

- 给定 用户缺少 `infra:storage:*` 权限
- 当 调用任意存储配置 API
- 那么 系统返回 HTTP 403

### 需求：可插拔对象存储策略

系统 MUST 通过可插拔的 `ObjectStorageClient` 策略访问对象存储；业务代码 MUST NOT 直接依赖厂商 SDK。

#### 场景：V1 MinIO 实现

- 给定 Provider 类型为 `minio`
- 当 创建上传会话或执行对象 I/O
- 那么 系统使用 MinIO 兼容实现

#### 场景：未实现的 Provider 类型

- 给定 Provider 类型为尚未实现的枚举值（如 `oss`）
- 当 请求创建客户端
- 那么 系统返回明确错误
- 并且 MUST NOT 静默回退到本地磁盘

#### 场景：Factory 拒绝未实现 Provider

- 给定 Provider 类型为 `oss` 且无注册实现
- 当 `ObjectStorageClientFactory` 创建客户端
- 那么 抛出 `UnsupportedStorageProviderException`

### 需求：对象存储 Bootstrap 属性绑定

应用 SHALL 绑定 `relayflow.storage.*` 属性，并为配置的默认 Provider 暴露 Bootstrap `StorageProviderConfig` Bean。

#### 场景：启动时绑定属性

- 给定 `application.yml` 含完整 `relayflow.storage.minio` 配置
- 当 应用启动
- 那么 `StorageProperties` 暴露 endpoint、凭证、bucket 与路径前缀

### 需求：对象存储文件上传

系统 SHALL 将上传文件存入 MinIO 兼容对象存储并返回可访问 URL 或 `fileId`；大文件字节默认 MUST NOT 经应用服务器代理。

#### 场景：直传附件完成上传

- 给定 已认证用户完成 upload session 与 confirm 流程
- 当 确认上传
- 那么 文件写入对象存储
- 并且 元数据写入 `infra_` 前缀表
- 并且 返回 `fileId` 供业务引用

#### 场景：无上传权限

- 给定 用户缺少 `infra:file:upload` 权限
- 当 请求创建 upload session
- 那么 系统返回 HTTP 403

### 需求：直传 Upload Session

系统 SHALL 支持 Upload Session，使客户端将文件字节直写对象存储，默认不经应用服务器代理。

#### 场景：创建 upload session

- 给定 已认证且具备上传权限的用户提交 filename、size、mimeType
- 当 调用 `POST /admin-api/infra/file/upload-session`
- 那么 系统返回 `uploadId`、presigned 上传 URL、objectKey 与过期时间
- 并且 objectKey MUST 含 `tenant/{tenantId}/` 前缀

#### 场景：Confirm 上传

- 给定 客户端直传成功后提交 `uploadId` 与对象元数据（如 ETag、size）
- 当 调用 confirm API 且对象存在且 size 匹配
- 那么 系统创建 `infra_file` 元数据行并返回 `fileId`

#### 场景：Confirm 拒绝过期 session

- 给定 upload session 已过期
- 当 调用 confirm
- 那么 系统拒绝并返回明确业务错误
- 并且 session 状态 MAY 更新为 `expired`

#### 场景：业务 API 仅引用 fileId

- 给定 其他模块关联业务附件
- 当 调用业务 API
- 那么 请求体仅含 `fileId`（或列表）
- 并且 multipart 文件流 MUST NOT 作为业务 API 默认上传路径

#### 场景：跨模块读取文件元数据

- 给定 其他模块调用 `FileApi.getFile(fileId)`
- 当 文件属于当前租户
- 那么 返回文件元数据且不暴露存储密钥

### 需求：文件业务绑定

系统 SHALL 支持将 `infra_file` 绑定到业务实体，用于授权与生命周期管理。

#### 场景：创建绑定

- 给定 业务服务调用 `FileApi.bindFile` 并传入 `fileId`、`bizType`、`bizId`
- 当 绑定成功
- 那么 系统持久化 `infra_file_binding` 行

### 需求：文件下载访问级别

系统 MUST 按 `access_level` 区分公开与私有下载路径。

#### 场景：公开文件下载（无需登录）

- 给定 `access_level=public` 的文件
- 当 客户端请求 `GET /app-api/infra/file/public/{fileId}`
- 那么 系统 HTTP 302 重定向至 presigned GET URL
- 并且 响应 SHOULD 含 `Cache-Control: public, max-age=31536000, immutable`

#### 场景：公开端点拒绝私有文件

- 给定 私有文件
- 当 客户端请求公开下载端点
- 那么 系统拒绝并返回明确业务错误

#### 场景：管理端私有文件下载

- 给定 具备 `infra:file:download` 的管理员
- 当 请求 `GET /admin-api/infra/file/{fileId}/download`
- 那么 系统 HTTP 302 重定向至有效期 15 分钟的 presigned GET URL

#### 场景：私有文件下载鉴权

- 给定 `access_level=private` 的文件
- 当 用户请求下载
- 那么 系统 MUST 先校验 JWT 与权限（或业务绑定授权）
- 并且 再返回短效 presigned 读 URL 或 HTTP 302

### 需求：操作审计日志

系统应记录管理端变更操作的基本审计日志，含用户、动作与时间戳。

#### 场景：记录管理操作

- 给定 管理员执行一次变更操作
- 当 操作完成
- 那么 持久化一条审计日志

### 需求：WebSocket 基础设施

系统应在 `/infra/ws` 暴露 WebSocket 端点，用于实时消息。

#### 场景：WebSocket 握手

- 给定 持有有效 JWT 的客户端
- 当 连接 `/infra/ws`
- 那么 连接建立成功
- 并且 会话注册以便投递消息

### 需求：WebSocket 发送模式

WebSocket 层应支持通过配置切换 `local` 与 `redis` 发送模式。

#### 场景：单实例模式

- 给定 `relayflow.websocket.sender-type=local`
- 当 向已连接用户推送消息
- 那么 在同一应用实例内完成投递

#### 场景：多实例模式

- 给定 `relayflow.websocket.sender-type=redis`
- 当 从某一实例推送消息
- 那么 经 Redis 订阅的所有实例均可向本地会话投递

### 需求：基础设施表前缀

基础设施模块全部数据表必须使用 `infra_` 前缀。

#### 场景：表命名

- 给定 新建一张 infra 模块表
- 当 应用迁移脚本
- 那么 表名以 `infra_` 开头
