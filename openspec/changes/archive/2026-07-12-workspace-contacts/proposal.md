# 提案：工作台通讯录（workspace-contacts）

## Why

[`im-direct-chat-integrate`](../im-direct-chat-integrate/proposal.md) 已打通 `/app/messages` 单聊，但用户**无法发现同事**——`/app/contacts` 仍为空壳。飞书路径为：**组织内联系人 → 部门树 → 成员 → 名片卡 → 发消息**。

在 [`org-member-dept-default`](../org-member-dept-default/proposal.md) 保证每人有主部门、[`admin-user-by-dept`](../admin-user-by-dept/proposal.md) 理顺组织数据后，须为员工工作台提供**只读通讯录**，并衔接 IM `peerUserId` 懒创建单聊。

## What Changes

### Lane `-web`

- `/app/contacts`：左栏部门树 + 成员列表 + 搜索；主区/弹层**名片卡**（昵称、部门、头像字）
- 名片 **「消息」** 按钮 → 跳转 `/app/messages` 并打开/准备与对方的 direct 会话
- `stores/contacts.ts`、`api/app/contacts.ts`、Mock 回退
- 起草 [`openspec/lanes/workspace-contacts/contract.md`](../../lanes/workspace-contacts/contract.md)

### Lane `-api`

- `GET /app-api/system/dept/tree` — 只读部门树（有效成员可用，不用 `sys_permission`）
- `GET /app-api/system/user/list-by-dept?deptId=` — 部门直属成员
- `GET /app-api/system/user/profile?userId=` — 名片详情（可选，V1 可与 list 字段合并）

### Lane `-integrate`

- store 去 Mock；名片「消息」与 `stores/im.ts` 联调（`openDirectChat(peerUserId)`）
- 看板 `workspace-contacts` → done

## Capabilities

### New Capabilities

（行为写入 `system` app 面只读目录 + 工作台 UI；无新 Maven 域）

### Modified Capabilities

- `system`：产品面只读组织目录 API
- `im`：从通讯录发起单聊的衔接（store 方法，非新 REST）

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `pages/app/contacts`、`stores/contacts.ts`、`stores/im.ts`（bridge） |
| `relayflow-module-system-biz` | `controller/app` 只读 Dept/User 端点 |
| Flyway | **无** |
| 前置 | `org-member-dept-default` + 建议 `admin-user-by-dept` 已完成 |

## 不在本 change

- 外部联系人、星标、群组、服务台（飞书侧栏扩展项）
- 编辑个人资料、个性签名持久化
- 管理端 UI
- 群聊 / 频道

## 实施顺序（本 change 内）

```text
workspace-contacts-web → workspace-contacts-api → workspace-contacts-integrate
```
