# 设计：工作台通讯录（workspace-contacts）

## Context

- UI 真源：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)
- 权限：[`product-permission-model.md`](../../../docs/dev/product-permission-model.md) — `/app-api` 有效成员即可，不用 RBAC
- IM 衔接：[`im-direct-chat` contract](../../lanes/im-direct-chat/contract.md) — `peerUserId` 懒创建
- 组织数据：依赖每人有主部门（`org-member-dept-default`）

## Goals / Non-Goals

**Goals:**

- 飞书式 **组织内联系人**：部门树 → 直属成员 → 名片 → 发消息
- 只读 `/app-api/system/*`；跨域 IM 仅通过 `peerUserId`，不直连 `sys_*` 表
- 搜索：成员昵称/用户名（V1 当前部门内或全局 keyword，见 contract）

**Non-Goals:**

- 外部联系人、在线状态（右栏可继续占位）
- 用户资料编辑
- 递归子部门成员

## UI 布局

```text
/app/contacts  (WorkspaceShell)
┌──────────────┬────────────────────────────┬─────────────┐
│ 组织内联系人   │ 成员列表 + 搜索              │ （可选占位） │
│ 部门树        │ 点击 → 名片卡 / 主区详情      │ 活跃状态    │
└──────────────┴────────────────────────────┴─────────────┘
```

| 区域 | 内容 |
|------|------|
| Panel | 标题「通讯录」；根节点名 = 租户名；`UTree` 或 `workspace-list-item` |
| Main | 选中部门成员列表；点击行展示名片卡（`UCard` / `UModal`） |
| 名片 | 头像字、昵称、部门名、**「消息」** `UButton` |
| Aside | V1 保持「在线状态后续接入」占位（与 messages 页一致） |

## IM Bridge

```text
名片「消息」点击
  → contactsStore / imStore.openDirectChat(peerUserId)
       · 若 conversations 已有 peer → selectConversation
       · 否则设置 pendingPeerUserId，进入 /app/messages 空会话
  → router.push('/app/messages')
  → 用户首条发送时 POST send { peerUserId, clientMsgId, content }
```

`openDirectChat` 放在 `stores/im.ts`；`-integrate` 实现，`-web` 可 Mock 跳转。

## API（app 面）

| 端点 | 说明 |
|------|------|
| `GET /app-api/system/dept/tree` | 扁平列表或树形 JSON；仅 `status=启用` 部门 |
| `GET /app-api/system/user/list-by-dept?deptId=&keyword=` | 主部门 = deptId 的成员；含 `id,nickname,avatarText,deptName` |
| `GET /app-api/system/user/profile?userId=` | 可选；V1 list 字段够用则可延后 |

**鉴权**：JWT + 有效 `sys_tenant_user`；**不**检查 `sys_permission`。

**实现**：`system-biz` 新建 `controller/app/AppDeptController`、`AppUserController`；复用 `DeptService` / `UserService` 读逻辑，禁止暴露写接口。

## Mock（`-web`）

- `mocks/contacts.ts`：2–3 个部门、5–8 人；仅 `stores/contacts.ts` 引用

## 验证

```bash
cd web && pnpm build          # -web
./mvnw -pl relayflow-server -am compile  # -api
# 浏览器：/app/login → /app/contacts → 消息
openspec validate workspace-contacts --strict
```

## 看板

| 切片 | 页面 | 端点 |
|------|------|------|
| workspace-contacts | `/app/contacts` | `/app-api/system/dept/tree`、`.../user/list-by-dept` |
