# 提案：群聊工作台 UI（im-group-chat-web）

## Why

`im-direct-chat-*` 已完成单聊闭环，但 `/app/messages` 仍无法建群、邀请成员或展示群会话与系统消息（如「XXX 加入了群聊」）。须按 **前端优先** 完成群聊 UI + Mock + API 契约，供 `im-group-chat-api` 按 contract 实现。

## What Changes

- `/app/messages`：建群入口、群会话列表项、群聊头部（人数）、系统消息样式
- 建群弹窗：群名称 + 从组织成员多选邀请
- 群成员侧栏：成员列表 + 邀请成员（复用成员选择器）
- 扩展 `stores/im.ts`、`api/app/im.ts`；群相关 API 未就绪时 store 内 Mock 回退
- 起草 `openspec/lanes/im-group-chat/contract.md`（REST 扩展 + 系统消息约定）
- 更新 `docs/dev/api-integration-board.md`

## Capabilities

### New Capabilities

（无新 spec 域；行为增量写入 `im` delta。）

### Modified Capabilities

- `im`：用户端群聊 UI 与 REST 契约草案（Java 实现在 `-api`）

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | messages 页、workspace 组件、im store/api、mocks |
| Java | **不改** |
| Flyway | **无** |
| 看板 | `im-group-chat` web → ui_ready |

## 不在本 change

- 群聊 REST / WS Handler 实现（`im-group-chat-api`）
- 去 Mock 联调（`im-group-chat-integrate`）
- 频道、附件、已读 UI、踢人/退群

## 前置

- 单聊契约：[`openspec/lanes/im-direct-chat/contract.md`](../../lanes/im-direct-chat/contract.md)
- 通讯录成员列表：[`openspec/lanes/workspace-contacts/contract.md`](../../lanes/workspace-contacts/contract.md)
- 表结构：`V0.1.0.6__init_im.sql`（`im_group` 已存在）
