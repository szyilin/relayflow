## Why

P1 扫描项 A1：成员私有下载直接复用管理员读取方法，只检查 tenant_id。隔离探针确认，同租户用户 2 可以为用户 1 的私有文件取得下载跳转，业务绑定没有参与授权；这与已有私有文件鉴权规格不符。

## What Changes

- 拆开成员下载和管理员下载：公开文件保持现有语义；未绑定私有上传仅创建者可读，已绑定私有文件由对应业务域判定当前可读性。
- 在 infra-api 提供文件授权 SPI，IM / docs 在本域实现策略，infra 不查询他域表或依赖他域 biz。多个有效绑定按可读授权并集处理，未知绑定不默认放行。
- 补齐 IM 私有附件的可信业务绑定，并对历史引用提供按域分页、可预览、可重跑的补绑定流程；保留正常聊天附件和云盘下载。
- 创建新的业务绑定前检查引用者已有文件读取资格，防止通过向自己的会话粘贴 fileId 绕过授权；绑定参数仅由可信业务服务生成。
- **BREAKING**：仅知道 fileId 或曾经拥有业务资源，不再构成当前访问授权；已有预签名 URL 的剩余 TTL 不在本轮强制撤销。

## Capabilities

### New Capabilities

无；细化现有文件与业务域访问控制。

### Modified Capabilities

- `infra`：成员私有下载、授权 SPI 与可信绑定规则。
- `im`：附件引用与消息绑定、当前会话成员的文件读取授权及历史绑定补齐。
- `docs`：云盘文件授权跟随当前对象所有权与生命周期，而非原始上传者。

## Impact

- 类型：**[平台]**；涉及 infra-api / infra-biz、im-api / im-biz、docs-api / docs-biz。保持 `biz → api`，不新增业务模块或业务域间 Mapper 直连。
- 复用 `/app-api/infra/file/download`、`upload-confirm` 和既有公开 / 管理下载端点；错误格式保持 CommonResult。`web/` / `deploy/` 无计划实现变更，但须浏览器回归消息、云盘与头像。
- 复用现有 `infra_file_binding` 与唯一索引，默认不新增 Flyway 或依赖；若实施发现必须改表，应先说明原因并取得许可，不能默默扩范围。
- 自部署：先完成历史引用盘点与补绑定预演再启用严格校验，不扫描后直接给所有租户成员补授权。回滚不得默认恢复租户级私有文件放行。
- 前置为 `member-session-revocation-hardening` 的有效身份规则；本 change 只承接 A1，不吞入历史存储位置修复、上传幂等或 P2 项。本轮已实施，验证见 verification.md。
