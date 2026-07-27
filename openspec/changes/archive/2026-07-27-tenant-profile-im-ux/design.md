## Context

产品约定：账号密码属全局账号；头像/昵称/签名/封面等展示资料跟随「当前企业 + 账号」。现行实现写在 `sys_user`，与 `sys_user_preference` 的 `(tenant_id, user_id)` 作用域不一致。IM 切换会话无按会话缓存且滚底仅 watch `messages.length`。

## Goals / Non-Goals

**Goals**

- 成员展示资料落在 `sys_tenant_user`（C 类：有值才物化；读时 fallback 用户名）
- 前端 Dock / 破图 / IM 缓存与未读导航对齐主流体验

**Non-Goals**

- 本次不引入 IndexedDB
- 不改消息 list API 分页形状（仍 `afterSeq`；缓存为前端内存）
- 不清理 `sys_user.avatar` 列（保留兼容；新读写以成员列为准）

## Decisions

1. **落库**：扩展 `sys_tenant_user` 增加 `nickname` / `avatar` / `signature` / `cover_file_id`；Flyway 从 `sys_user` 回填 ACTIVE 行。属 **C 类**（显式更新才写成员列；读 merge：成员列 → `sys_user` 昵称 → username）。
2. **API**：路径不变；`UserService` profile 与 permission-info 的展示字段改读/写成员行。
3. **Dock**：`updateCurrentProfile` / `syncAllTenants` 不得用当前企业 avatar/nickname 覆盖其他 `userId:tenantId` 条目；其他企业条目保留已有或留空待切过去再拉。
4. **头像 UI**：统一经 `resolveAvatarUrl`；原生 img 必须 `@error` 回退文字；优先 `UAvatar`。
5. **IM**：`messagesByConversation` 内存 Map；有缓存先展示、`loadingMessages` 仅无缓存时为 true；切换后强制 `scrollToBottom`；进入会话记录 `enterUnreadCount` / `firstUnreadSeq`，不立刻 `markRead(max)`；可视区最大 seq debounce 上报；离开会话再报一次；右下角 FAB：未读跳转 / 回到最新。

## Risks / Migrations

- 旧客户端仍假设全局资料：0.x 可接受；回填保证切企业前各企业初始值一致，之后各自改互不影响。
- IM 已读时机变严可能短暂多未读角标：属预期。
