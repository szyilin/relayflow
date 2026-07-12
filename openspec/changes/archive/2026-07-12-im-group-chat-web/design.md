# 设计：群聊工作台 UI（im-group-chat-web）

## Context

- 架构真源：[`im-platform-foundation` design](../../changes/archive/2026-07-12-im-platform-foundation/design.md) Phase 3
- 单聊 UI：[`im-direct-chat-web`](../archive/2026-07-12-im-direct-chat-web/design.md)
- UI 真源：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)
- 契约：本 change 起草 [`contract.md`](../../lanes/im-group-chat/contract.md)

## Goals / Non-Goals

**Goals（MVP）：**

- 建群：群名称 + 选择 ≥1 名组织成员（不含自己）
- 邀请成员：群主/成员可向已有群追加成员
- 群消息：复用现有发送框；群会话 `type=group` 走 `conversationId` 发送
- 系统消息：列表中 `senderType=system` 居中展示（如「李晓明 加入了群聊」）
- 群成员侧栏：选中群会话时 `#aside` 展示成员列表
- 群 API 404/未实现 → store 内 Mock 回退（单聊仍走真实 API）
- `pnpm build` 通过

**Non-Goals：**

- Java 实现、Flyway 变更
- 频道、群公告、@提及、附件、已读 UI
- 踢人、退群、转让群主
- 真实 WebSocket 群推送（`-integrate`）

## UI 结构

```text
/app/messages
├── Panel 顶栏：标题 +「建群」按钮
├── 会话列表：direct / group 混排；group 副标题显示人数
├── Main
│   ├── Header：群名 +「群聊 · N 人」+ 邀请按钮
│   ├── 消息区：user 气泡（群聊显示发送者昵称）；system 居中灰字
│   └── 输入栏：与单聊相同
└── Aside（群会话）：成员列表 +「邀请成员」
```

## 组件

```text
web/src/components/workspace/
├── ImCreateGroupModal.vue      # 建群：名称 + 成员多选
└── ImInviteMembersModal.vue    # 邀请：成员多选（排除已在群内）
```

## 数据流

```text
建群 → POST /app-api/im/group/create → 返回 conversationId → selectConversation
邀请 → POST /app-api/im/group/members/add → 刷新成员 + 消息列表（含 system 消息）
发消息 → POST /app-api/im/message/send { conversationId }（与单聊同构）
```

成员候选：复用 `GET /app-api/system/user/list-by-dept`（根部门 + keyword），与通讯录同源。

## Mock 策略

| API | 未就绪时 |
|-----|---------|
| `group/create` | `mocks/im.ts` 创建本地群会话 + 初始 system 消息 |
| `group/members/add` | Mock 追加成员 + 插入 system 消息 |
| `group/members` | Mock 成员列表 |
| `conversation/list` | 真实列表 + 合并 Mock 群会话（仅 Mock 模式下） |

检测：`ApiError` 且 HTTP 404 或业务码表示未实现时启用 Mock。

## 验证

```bash
cd web && pnpm build
openspec validate im-group-chat-web --strict
```

浏览器（无群聊 API 时走 Mock）：

1. `/app/login` 登录
2. `/app/messages` →「建群」→ 选成员 → 创建
3. 发群消息、邀请成员、查看系统消息与成员侧栏
