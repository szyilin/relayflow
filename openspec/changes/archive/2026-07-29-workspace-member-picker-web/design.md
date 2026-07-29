## Context

`WorkspaceMemberPicker` 已有三源 UI；`getGroupMembers` 返回 `role: owner|admin|member`；会话列表无 myRole 字段。

## Goals / Non-Goals

**Goals:** 真实可管理群；去掉 seed；邀请入群共用 picker；excludeUserIds；contract 草案。

**Non-Goals:** 专用 managed API 实现（可只写契约）；话题群模式。

## Decisions

1. `-web`：`memberPicker.loadManagedGroups()` = 拉 group 会话 → 并行 `getGroupMembers` → 当前用户 role∈{owner,admin} 则入列；成员展开仅 `subjectType=user`。
2. 并发上限 / 失败跳过单群；空列表合法。
3. `ImInviteMembersModal` 内嵌 picker + exclude 已在群成员。
4. 删除 `createHub.managedGroupSeeds`。
5. 可选：sources 页关键字过滤三个入口标题。

## Risks / Trade-offs

- [Risk] N 次 members 请求 → 契约预留批量 managed API；web 先限 30 群
