## ADDED Requirements

### Requirement: Enterprise code is globally unique opaque id

系统 MUST 在创建租户时生成全局唯一的企业编号（`sys_tenant.code`）：11 位大写字母与数字（排除易混字符 `0/O/1/I`），创建后不可更新。种子租户 MUST NOT 再使用可读单词 `default` 作为运行时编号；既有 `code=default` 行 MUST 由迁移改为同风格唯一编号。

#### Scenario: New tenant receives opaque code

- **WHEN** 用户注册并创建新企业
- **THEN** 持久化的 `sys_tenant.code` 匹配 `^[A-HJ-NP-Z2-9]{11}$` 且全库唯一

#### Scenario: Seed default code migrated

- **WHEN** 应用执行含企业编号迁移的 Flyway
- **THEN** 不再存在未删除且 `code='default'` 的租户行

#### Scenario: Code immutable via profile update

- **WHEN** 客户端在更新企业档案请求中携带 `code`
- **THEN** 服务端忽略或拒绝变更，库中编号保持不变

### Requirement: Enterprise logo avatar

系统 MUST 在 `sys_tenant` 持久化可选 `logo_file_id`（infra 文件 id）。`GET/PUT /admin-api/system/enterprise/profile` MUST 暴露 `logoFileId`（string | null）。具备 `system:enterprise:update` 的管理员 MUST 能通过上传公开文件并更新 profile 更换企业头像。概览与企业信息页 MUST 使用同一字段展示。

#### Scenario: Upload and display enterprise logo

- **WHEN** 管理员上传企业头像并成功更新 profile
- **THEN** 后续 GET profile / overview 返回同一 `logoFileId`
- **AND** 管理端企业信息与概览展示对应公开文件 URL

#### Scenario: Clear or absent logo uses placeholder

- **WHEN** `logoFileId` 为空
- **THEN** UI 以企业名称首字（或默认字）作为头像占位
