# API 契约：admin-docs-product-settings（B + A1 草案）

> **状态**：B / A1 / A2 = **全部 integrate 完成**  
> **变更**：`admin-docs-product-settings-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

- **B**：组织策略三开关（成员分享 / MD 导出 / 云盘上传）
- **A1**：企业文件运营（列表 / 转移 / 删 / 恢复 / 销毁）
- **A2**：按人用量与配额（已交付）

---

## Wave B（已交付）

### 鉴权

| 面 | 路径 | 鉴权 |
|----|------|------|
| Admin | `/admin-api/docs/product-settings` | `docs:product-settings:query` / `update` |
| App | `/app-api/docs/settings/effective` | JWT + 有效成员 |

### Admin GET/PUT `/admin-api/docs/product-settings`

```json
{
  "memberShareEnabled": true,
  "markdownExportEnabled": true,
  "driveUploadEnabled": true
}
```

### App GET `/app-api/docs/settings/effective`

字段同 Admin。强制：share 写 / md export / drive file 注册。

---

## Wave A1（integrate 完成）

前端：`api/admin/docsFiles.ts` → `stores/docsFiles.ts` → `/admin/products/docs/files`（无本地 Mock）。  
**禁止**返回文档正文；**禁止**用 `/admin-api/infra/file` 冒充。

工作台对齐：软删写 `lifecycle=DELETED` + `deleted_at`；最近列表对 `@TableLogic` 不可见行展示 tombstone（`accessState=DELETED`）。

### 鉴权（A1 已种子）

| 码 | 用途 |
|----|------|
| `docs:files:query` | 列表 |
| `docs:files:transfer` | 转移所有者 |
| `docs:files:delete` | 软删 |
| `docs:files:restore` | 恢复 |
| `docs:files:purge` | 销毁 |

### GET `/admin-api/docs/files`

Query：`pageNo`、`pageSize`、`keyword`（标题）、`type`=`RICH_DOC|FILE`、`lifecycle`=`ACTIVE|DELETED`、`ownerUserId`。

响应：

```json
{
  "list": [
    {
      "objectId": "9001",
      "title": "产品路线图",
      "type": "RICH_DOC",
      "lifecycle": "ACTIVE",
      "ownerUserId": "100",
      "ownerDisplayName": "管理员",
      "updatedAt": "2026-07-29T12:00:00Z",
      "sizeBytes": null,
      "deletedAt": null,
      "daysUntilPurge": null
    }
  ],
  "total": 1,
  "pageNo": 1,
  "pageSize": 20
}
```

不含 `body`。列表含 ACTIVE + DELETED（未 purge）；不含 PURGED。

### POST `/admin-api/docs/files/transfer-owner`

```json
{ "objectIds": ["9001"], "newOwnerUserId": "101" }
```

仅 `ACTIVE`；新主须本企业 ACTIVE 成员。V1：**整批事务**——任一失败则全部回滚（写死）。

**Placement 冻结（V1）**：保持当前归属面——有 Library 节点则迁到新主「我的文档库」根（`parent_id=null`）；否则有 Drive 项则迁到新主云盘根（`folder_id=null`）；皆无则按类型新建（`RICH_DOC`→Library 根，`FILE`→Drive 根）。

### POST `/admin-api/docs/files/delete`

```json
{ "objectIds": ["9001"] }
```

→ `DELETED` + `deletedAt`（仅 ACTIVE）。

### POST `/admin-api/docs/files/restore`

```json
{ "objectIds": ["9003"], "newOwnerUserId": null }
```

`newOwnerUserId` 省略/null = 原主；否则指定新主。仅 `DELETED` 且自 `deletedAt` 起 ≤90 天。恢复后 placement 规则同 transfer。

### POST `/admin-api/docs/files/purge`

```json
{ "objectIds": ["9003"] }
```

仅 `DELETED` → `PURGED`（清空 `body`），不可恢复。

---

## Wave A2（integrate 完成）

### 鉴权

| 码 | 用途 |
|----|------|
| `docs:storage-quota:query` | 用量列表 / 读配额 |
| `docs:storage-quota:update` | 写配额 |

### GET `/admin-api/docs/storage-usage`

Query：`pageNo`、`pageSize`、`keyword`（姓名/用户名/手机/邮箱）。

```json
{
  "list": [
    {
      "userId": "100",
      "displayName": "管理员",
      "deptName": "研发",
      "avatarUrl": "12345",
      "usedBytes": 1048576,
      "quotaBytes": 1073741824,
      "usageRatio": 0.001
    }
  ],
  "total": 1,
  "pageNo": 1,
  "pageSize": 20
}
```

`quotaBytes` / `usageRatio` 为 `null` 表示不限制。已用 = 该成员 ACTIVE `FILE` 的 `doc_object.size_bytes` 之和（禁止 JOIN `infra_file`）。
`avatarUrl` 为租户成员头像 file id（前端拼公开 URL）；`deptName` 为主部门名。

### GET/PUT `/admin-api/docs/storage-quota`

```json
{
  "defaultQuotaBytes": null,
  "userRules": [
    { "userId": "101", "displayName": "张三", "quotaBytes": 209715200 }
  ]
}
```

生效上限：用户规则 > 租户默认 > 不限制。PUT 全量替换用户规则列表。**白名单最多 20 条**，超限 `DOC_STORAGE_QUOTA_RULES_LIMIT`。UI 编辑单位为 **GB**，API 仍存 bytes。

触顶：云盘 `registerFile` 超限 → `DOC_STORAGE_QUOTA_EXCEEDED`。

### 前端路由

| 路径 | 说明 |
|------|------|
| `/admin/products/docs/permissions` | B |
| `/admin/products/docs/files` | A1；支持 `?ownerUserId=` |
| `/admin/products/docs/storage` | 存储用量详情 |
| `/admin/products/docs/storage/quota` | 存储上限管理（成员 / 部门已交付；上限提示 Tab 占位；用户组后置） |

### curl 草案（A1）

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/docs/files?pageNo=1&pageSize=20&lifecycle=ACTIVE"

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"objectIds":["9001"],"newOwnerUserId":"101"}' \
  http://localhost:8080/admin-api/docs/files/transfer-owner
```

### curl 草案（A2）

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/docs/storage-usage?pageNo=1&pageSize=20"

curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"defaultQuotaBytes":1073741824,"userRules":[]}' \
  http://localhost:8080/admin-api/docs/storage-quota
```

---

## Optimize v1（`admin-docs-files-storage-optimize-v1`）

> 对齐飞书：身份用 `WorkspaceOwnerChip`；计量落 `doc_object.size_bytes`；上限管理分波。

### W1 — 计量 + 文件/用量身份 UX

| 项 | 约定 |
|----|------|
| `doc_object.size_bytes` | FILE 注册时写入；RICH_DOC 为 null；用量 `SUM(size_bytes)`；**禁止** docs Mapper JOIN `infra_file` |
| Admin files `sizeBytes` | 读 `size_bytes`；热路径禁止逐行 `FileApi.getFile` |
| `POST /admin-api/docs/files/backfill-sizes` | `docs:files:query`；经 `FileApi.getFiles` 回填历史 null |
| `FileApi.getFiles(ids)` | infra-api 批量 |
| 成员搜索 keyword | 姓名 / 用户名 / 手机 / **邮箱**（`MemberUserApi`） |
| UI | 文件页：无标题下 objectId；所有者/新主用选人 + `WorkspaceOwnerChip`；用量页成员列 chip；搜索占位含邮箱 |

### W2 — 飞书式上限页（大纲）

| 路径 | 说明 |
|------|------|
| `/admin/products/docs/storage` | 用量详情 |
| `/admin/products/docs/storage/quota` | 上限管理（成员 Tab：租户默认 GB + 白名单 ≤20） |

`GET/PUT /admin-api/docs/storage-quota` 保持 bytes；UI 用 GB。usage 增补 `avatarUrl` / `deptName`（可选）。

### W3 — 部门规则（大纲）

- 表 `doc_storage_quota_dept_rule`（`UNIQUE(tenant_id, dept_id)`）；PUT 带 `deptRules[]`
- 生效：成员白名单 > **主部门**规则 > 租户默认 > 不限制
- 无主部门则跳过部门规则
- `deptRules` 全量替换；非法部门 → `DOC_STORAGE_QUOTA_DEPT_INVALID`

```json
{
  "defaultQuotaBytes": 10737418240,
  "userRules": [],
  "deptRules": [
    { "deptId": "200", "deptName": "研发", "quotaBytes": 5368709120 }
  ]
}
```

### W4 — 上限提示占位与收口

- 「上限提示设置」Tab：前端占位说明（阈值/文案/渠道后续版本）；**不**新增 API / 表。
- 用户组白名单：明确后置（飞书优先级含用户组；本版仅成员 + 主部门）。
- 看板：`admin-docs-file-ops` / `admin-docs-storage-quota` 注明 optimize-v1。
