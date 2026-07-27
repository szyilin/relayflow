# API 契约：workspace-docs-ia-redesign

> **状态**：draft（`-web` 起草；`-api` 按本契约补齐 VO）  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **母 change**：[`workspace-docs-ia-redesign-v1`](../../changes/workspace-docs-ia-redesign-v1/proposal.md)  
> **前端 change**：[`workspace-docs-home-ia-web`](../../changes/workspace-docs-home-ia-web/proposal.md)  
> **基线**：share-star / library / drive 既有端点保留；本契约仅写 **列表字段增量** 与行为对齐

## 背景

飞书式 IA：主页 Tab、文档库主区树形列表、云盘滑入树。前端已用既有 API；下列字段缺口由 `-api` 补齐。**默认无新表 / 无 Flyway**。

## 鉴权

同既有 `/app-api/docs/**`（有效成员 + 对象 ACL）。

## 通用所有者字段

列表行展示 `WorkspaceOwnerChip` 时，响应宜包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `ownerUserId` | string | 必填（可访问行） |
| `ownerDisplayName` | string | **真实昵称/用户名**；禁止仅返回「我」；可用 `ownedByMe` 辅助 |
| `ownedByMe` | boolean | 可选 |
| `ownerAvatarFileId` | string \| null | 可选；`UserBasicDTO` 扩 avatar 后批量填充 |

前端临时回退：auth / contacts 解析头像与昵称。

---

## GET /app-api/docs/recent

既有。对齐列：

- `title` / `titleSnapshot` / `locationLabel` / `accessState`
- `ownerUserId` / `ownerDisplayName` / `ownedByMe` / `ownerAvatarFileId?`
- `createTime` / `lastOpenedAt` / `starred`

## GET /app-api/docs/shared

增量（相对现状）：

| 字段 | 说明 |
|------|------|
| `createTime` | 文档创建时间（可选，有则展示） |
| `lastOpenedAt` | 当前用户 visit（可选） |
| `locationLabel` | 如「与我共享」 |
| `ownerAvatarFileId` | 可选 |

既有：`objectId` `title` `type` `permission` `ownerUserId` `ownerDisplayName` `grantedBy*` `sharedAt` `starred`

## GET /app-api/docs/starred

增量：

| 字段 | 说明 |
|------|------|
| `createTime` | 可选 |
| `lastOpenedAt` | 可选 |
| `ownerAvatarFileId` | 可选 |

既有：`objectId` `title` `type` `starredAt` `accessState` `ownerUserId` `ownerDisplayName` `locationLabel`

## GET /app-api/docs/library/tree

节点增量（可选，无则前端列显示 —）：

```json
{
  "nodeId": "…",
  "parentId": null,
  "objectId": "…",
  "title": "…",
  "sortOrder": 0,
  "createTime": "2026-07-27T10:00:00+08:00",
  "lastOpenedAt": "2026-07-27T11:00:00+08:00",
  "children": []
}
```

`lastOpenedAt` = 当前用户对该 `objectId` 的 visit（左连 `doc_user_visit`）。

## GET /app-api/docs/drive/items?folderId=

listing 中 folders / items 增量：

| 字段 | 说明 |
|------|------|
| `ownerUserId` | V1 个人云盘 = 当前用户 |
| `ownerDisplayName` | 真实名 |
| `ownerAvatarFileId` | 可选 |
| `updateTime` | 已有则保留 |

## （可选）UserBasicDTO

`com.relayflow.module.system.api.user.dto.UserBasicDTO` 增加 `avatarFileId`（Long/string 按现有惯例），docs 组装列表时批量 `UserApi` 填充。

## 非目标

- 新 REST 资源（无「主页」专用 API）
- 云盘详情检视栏
- 模板库

## curl 烟测（`-api` 后）

```bash
# recent
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/app-api/docs/recent" | jq .

# shared
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/app-api/docs/shared" | jq .

# drive items
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/app-api/docs/drive/items" | jq .
```
