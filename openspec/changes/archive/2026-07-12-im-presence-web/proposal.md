# 提案：在线状态 UI（im-presence-web）

## Why

`/app/messages` 与 `/app/contacts` 右侧栏占位「在线状态将在后续切片接入」。`RealtimeTransportApi.isUserOnline` 已存在，须按 **前端优先** 完成在线状态 UI + contract，供 `im-presence-api` 实现 batch 查询。

## What Changes

- 消息页 Aside（非群聊）：展示单聊对端在线/离线
- 通讯录 Aside：当前可见成员在线点
- `api/app/presence.ts`、`stores/presence.ts`（或 im store 扩展）
- 起草 `openspec/lanes/im-presence/contract.md`
- Mock：随机 online 标志

## Capabilities

### Modified Capabilities

- `im`：工作台在线状态 UI 与 presence batch 契约草案

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | messages、contacts、store |
| Java | **不改** |

## 下一 change

- `im-presence-api`
