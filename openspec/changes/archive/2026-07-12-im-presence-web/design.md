# 设计：在线状态 UI（im-presence-web）

## Goals / Non-Goals

**Goals：**

- 单聊 Aside：对端昵称 + 绿点/灰点 +「在线」/「离线」
- 通讯录 Aside：成员列表每项在线指示
- 轮询 `GET /presence/batch` 每 30s（WS 可选后续）
- Mock 回退

**Non-Goals：**

- 自定义状态、typing
- 管理端在线用户监控

## 组件

复用现有 Aside 区域，不新增路由。

## 数据流

```text
selectConversation(direct) → batch [peerUserId]
contacts loadMembers → batch [visible userIds]
定时 refresh batch
```

## 验证

```bash
cd web && pnpm build
openspec validate im-presence-web --strict
```
