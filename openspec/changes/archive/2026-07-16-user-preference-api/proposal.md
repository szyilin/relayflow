## Why

企业内用户主题、主题色、会话显示等配置需要持久化，且同一账号在不同企业可不同。需 `sys_user_preference` + app-api，采用 C 类（读合并默认、写 upsert），无入企灌表。

## What Changes

- Flyway 新增 `sys_user_preference`（`tenant_id` + `user_id` 唯一，`settings` JSONB，`schema_version`）。
- 代码常量作为默认真源；`GET` 合并返回完整视图；`PUT`/补丁 upsert。
- 用户端 API：`GET/PUT /app-api/system/user/preference`（路径以 contract 为准）。
- V1 **不做** `sys_tenant_preference`；merge 层预留注释/接口形状即可。
- codegen 按需生成 DO/Mapper 后 diff 合入。

## Capabilities

### New Capabilities

- `user-preference`: 企业内用户偏好读写、默认合并与 upsert 行为

### Modified Capabilities

- （无）

## Impact

- **后端**：`relayflow-module-system`（api + biz）、`relayflow-server` Flyway。
- **前端**：由后续 integrate / settings-web 对接；本 change 可只提供 contract。
- **跨域**：IM 气泡布局键可放在 JSON `im.*` 命名空间，IM 经只读消费或 system API，禁止 im-biz 直查 `sys_`。
- **回滚**：新迁移向前兼容；回滚需数据迁移评估（开发期可 drop 表）。
