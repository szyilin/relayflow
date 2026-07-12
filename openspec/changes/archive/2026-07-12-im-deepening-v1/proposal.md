# 提案：IM 深化 V1（im-deepening-v1 · 母 change · 执行路线图）

## Why

`im-direct-chat-*` 与 `im-group-chat-*` 已完成文本单聊/群聊闭环，但距离飞书式日常体验仍缺：**聊天附件**、**已读回执 UI**、**在线状态**。本 change 为 **总路线图**，各能力按独立子 change 纵向切片交付（`-web` → `-api` → `-integrate`）。

## What Changes

本 change **不直接改代码**，仅维护：

- 子 change 执行顺序与依赖
- 契约目录索引（`openspec/lanes/im-*`）
- 归档检查清单

## 子 change 清单

| 顺序 | Change | 状态 | 说明 |
|------|--------|------|------|
| 0 | `im-group-chat-*` | ✅ 完成待 archive | 建群、邀请、群消息 |
| 1 | `im-message-file-web` | ✅ 完成 | 附件 UI + contract |
| 2 | `im-message-file-api` | ✅ 完成 | file/image 消息校验 + downloadUrl |
| 3 | `im-message-file-integrate` | ✅ 完成 | 去 Mock 联调 |
| 4 | `im-read-receipt-web` | ✅ 完成 | 单聊「已读」UI |
| 5 | `im-read-receipt-api` | ✅ 完成 | read-status + WS read.updated |
| 6 | `im-presence-web` | ✅ 完成 | 消息/通讯录在线状态 UI |
| 7 | `im-presence-api` | ✅ 完成 | batch presence REST |
| 8 | `im-presence-integrate` | ✅ 完成 | REST 轮询联调 |

## Capabilities

### Modified Capabilities

- `im`：file/image 消息、已读回执、在线状态（各子 change spec delta 增量合并）

## Impact

| 区域 | 本 change |
|------|------------|
| 代码 | 无（子 change 分别提交） |
| 文档 | 本 `tasks.md` + lanes contract |

## 不在路线图内

- 频道 `im-channel-*`（Phase 4）
- 云文档、任务、审批
