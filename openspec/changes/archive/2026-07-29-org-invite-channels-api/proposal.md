## Why

`org-invite-channels-web` 已用 store 草稿跑通四渠道与加入页；`org-invite-settings-api` 已提供租户设置真源。需落地 `sys_org_invite` 与 app-api（capability / 共享渠道 / 手机号 / consume），供后续 integrate 去草稿。

## What Changes

- Flyway：`sys_org_invite`（邀请实体）；可选将既有 `NOT_JOINED` 迁移为 MOBILE 邀请行
- App API：`GET …/org-invite/capability`；共享渠道 ensure + reset；`POST …/mobile`；`POST …/consume`
- 消费时 upsert `sys_tenant_user` 为 ACTIVE（共享渠道可多次消费；手机号一次性 CONSUMED）
- 定稿 `openspec/lanes/org-invite/contract.md` channels 章；看板 api → archived
- **不**改管理端 UI / web store（integrate）；审批列表页、Bot 卡推送可本切片轻量跳过或后续

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `org-invite`: 增加工作台渠道创建/重置/消费与 capability 行为（相对 settings-api）

## Impact

- **后端**：`relayflow-module-system`、`relayflow-server` Flyway（建议 `V0.1.0.42`+）
- **前端**：本 change 不改 `web/`
- **回滚**：0.x 可 drop 表；迁移 NOT_JOINED 需评估
- **跨域**：consume 可调用 `ImBotApi.ensureUserEnablementsOnActive`（已有模式）
