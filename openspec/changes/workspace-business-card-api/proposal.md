## Why

飞书式个人名片 UI（`-web`）已用 localStorage 暂存签名/封面/备注，无法跨设备，不符合产品系统要求。需按契约落库并接通 API。

## What Changes

- `sys_user` 扩展 `signature`、`cover_file_id`
- 新建 `sys_contact_remark`（查看者对目标成员的备注）
- 扩展本人 profile GET/PUT；新增他人资料 GET、备注 GET/PUT
- 前端去掉 localStorage 真源，改走 API（封面走 public 文件上传）
- 冻结 `openspec/lanes/workspace-business-card/contract.md`

## Capabilities

### New Capabilities

- （无独立新 capability 名；行为归入既有 `workspace-business-card` / `user-preference` 域之外的 profile 扩展）

### Modified Capabilities

- `workspace-business-card`: 持久化与端点从草案变为已实现；去除本地暂存真源

## Impact

- **Java / Flyway**：system 域；`V0.1.0.20__…`
- **web/**：`profile` / `businessCard` store、名片组件
- **回滚**：开发期可 drop 列/表（见 database.md `0.x` 政策）；正式 `1.x` 前不做数据兼容包
