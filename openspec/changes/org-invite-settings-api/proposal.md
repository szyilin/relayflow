## Why

`org-invite-settings-web` 已交付管理端邀请设置 UI，但设置仅存 Pinia 内存。需按契约落地 `sys_tenant_invite_settings` 与 `GET/PUT /admin-api/system/invite-settings`，供后续 `-integrate` 去草稿，并为四渠道 / capability 提供租户级真源。

## What Changes

- Flyway 新增 `sys_tenant_invite_settings`（每租户一行；默认语义 `ADMIN_ONLY`）
- codegen 生成 DO / Mapper 后 diff 合入 `relayflow-module-system-biz`
- Admin API：`GET` / `PUT /admin-api/system/invite-settings`（全量 PUT）
- 读无行时返回代码默认（C 类：不强制入企灌表）；写时 upsert
- 定稿 `openspec/lanes/org-invite/contract.md` settings 章；看板 api → archived（本切片完成后）
- **不**实现工作台四渠道、`sys_org_invite`、capability（留给 `org-invite-channels-api`）

## Capabilities

### New Capabilities

- `org-invite`: 租户邀请全局设置持久化与管理端读写（本切片仅 settings；渠道/消费后续增量）

### Modified Capabilities

- （无）

## Impact

- **后端**：`relayflow-module-system`（api DTO 可选 + biz）、`relayflow-server` Flyway（建议 `V0.1.0.41__…`）
- **前端**：本 change 可不改 `web/`（integrate 再接）；API 客户端已预留 `api/admin/inviteSettings.ts`
- **权限**：与 `-web` 对齐，读 `system:user:list`、写 `system:user:create`（可不新增 permission 种子）
- **回滚**：开发期可 drop 表；生产需评估（0.x 可破坏性需征得同意）
- **跨域**：仅 system 域；无 `*-biz → *-biz`
