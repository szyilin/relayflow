## Context

既有 enrich list/accept/star API；Hub 弹窗用 createHub 假搜索。母 change design D7。

## Goals / Non-Goals

**Goals:** 精确手机/邮箱搜索 UI；发起申请草稿；拒绝 pending；通讯录与 Hub 共用弹窗；contract。

**Non-Goals:** 真搜索/POST API、短信、邮箱联系人目录、扫码加好友实装。

## Decisions

1. Store `contactAdd`：searchHits 生成规则仍可本地（确切 11 位或含 @）；`outgoingPending`；`isAlreadyRelated(userId)` 查 external + outgoing。
2. 「添加」写入 outgoing；可选把一条镜像 pending 写入 `contacts.contactRequests` **仅当**演示同端自测——**否决**：不同用户。仅维护 outgoing + toast。
3. Reject：`contacts.rejectRequestLocal(id)` 改 status=rejected；接 API 后换调用。
4. 从 createHub 移除 `searchExternalAccounts`，改走 contactAdd。
5. 通讯录外部列表 header 加「添加」按钮 → `createHub.openAddExternal()`。

## Risks / Trade-offs

- [Risk] 本地搜索与真 API 结果不一致 → contract 定字段；integrate 换实现
