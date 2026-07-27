## Why

`org-invite-settings-web` 已交付管理端邀请设置。工作台「添加企业成员」仍为四 Tab 骨架，需在 `-web` 阶段用 store 临时数据跑通 QR / 链接 / 邀请码 / 手机号交互与加入页占位，并为 `-api` 冻结契约章节。

## What Changes

- 扩展 `openspec/lanes/org-invite/contract.md`：capability、四渠道、加入/消费草案
- 增强 `WorkspaceOrgInviteModal`：按设置计算失效文案；重置/复制走 createHub/orgInvite store
- 新增 `/app/invite/join`（或等价）加入页骨架：贴链接 token / 输入邀请码
- 手机号邀请写入 store 临时 pending 列表（非真 API）
- 「+」菜单 `canInvite` 已读 `inviteSettings`（本切片确认联通）
- 看板 `org-invite-channels` web → ui_ready

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `org-invite`：四渠道工作台 UI 与加入流骨架（相对母 change 的可演示子集）

## Impact

- 仅 `web/` + contract/看板；不改 Java / Flyway（表结构留给 `-api`）
