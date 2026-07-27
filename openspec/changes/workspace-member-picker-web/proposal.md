## Why

Create Hub 建群已有 `WorkspaceMemberPicker`，但「我管理的群组」仍用 createHub 演示种子，邀请入群仍用旧扁平选人。需用既有 IM 会话 + 成员角色拉真实可管理群，并让邀请入群复用同一选择器。

## What Changes

- 用 IM `conversation/list` + `group/members` 组装「我管理的群组」（owner/admin）；去掉 createHub seed
- Picker 支持 `excludeUserIds`；`ImInviteMembersModal` 改用共享 picker
- 起草 `GET /app-api/im/group/managed` 契约（`-web` 前端聚合，`-api` 可优化）
- 看板 + workspace-ui-patterns

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-create-hub`：成员选择器数据真源增强
- `im`：我管理的群组列表行为（web 聚合 / 契约草案）

## Impact

- 仅 `web/` + contract；不强制本切片写 Java（可选后续 `-api` 专用端点）
