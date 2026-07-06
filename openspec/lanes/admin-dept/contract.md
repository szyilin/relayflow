# API 契约：admin-dept

> **状态**：已冻结（2026-07-08）  
> **起草**：`admin-dept-slice` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端部门组织树 CRUD：维护租户内 `sys_dept` 层级结构，供用户归属与数据范围计算使用。

## 端点

### GET /admin-api/system/dept/list

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:dept:list` |

**Response `data`**：当前租户内部门扁平列表（前端自行组树），按 `sort`、`id` 升序。

```json
[
  {
    "id": 1,
    "parentId": 0,
    "name": "总部",
    "sort": 0,
    "status": 0,
    "leaderUserId": null,
    "createTime": "2026-01-01T00:00:00Z"
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `parentId` | number | 上级部门 ID；`0` 表示顶级 |
| `status` | number | `0` 启用 · `1` 禁用 |
| `leaderUserId` | number \| null | 负责人用户 ID（V1 可选） |

### GET /admin-api/system/dept/get

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:dept:query` |
| Query | `id`（必填） |

**Response `data`**：单条部门，字段同 list 元素。

### POST /admin-api/system/dept/create

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:dept:create` |

**Request body**：

```json
{
  "name": "研发部",
  "parentId": 1,
  "sort": 0,
  "status": 0,
  "leaderUserId": null
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 部门名称 |
| `parentId` | 否 | 默认 `0`（顶级） |
| `sort` | 否 | 默认 `0` |
| `status` | 否 | 默认 `0` |

**Response `data`**：新建部门 ID（number）。

### PUT /admin-api/system/dept/update

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:dept:update` |

**Request body**：同 create，且必须包含 `id`。

**Response `data`**：`true`

**业务规则**：

- 上级部门不能为自身或其子孙部门
- 上级部门必须存在于当前租户

### DELETE /admin-api/system/dept/delete

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:dept:delete` |
| Query | `id`（必填） |

**Response `data`**：`true`

**拒绝删除**（业务错误码）：

| code | 含义 |
|------|------|
| `1001004004` | 存在子部门 |
| `1001004005` | 部门下存在用户（`sys_user_dept`） |

## Flyway

`V0.1.0.4__seed_root_dept.sql`：租户 `1` 无部门时插入根部门「总部」（`id=1, parent_id=0`）。

## curl 示例

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/system/dept/list | jq

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/dept/get?id=1" | jq

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"研发部","parentId":1,"sort":1}' \
  http://localhost:8080/admin-api/system/dept/create | jq
```

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/dept.ts` |
| Store | `stores/dept.ts`；**无 Mock 回退** |
| 页面 | `/admin/system/dept` 树形 CRUD |
| 权限 | 按钮按 `system:dept:create/update/delete` 显示 |

## 非目标（本切片）

- 不改 `useAdminNav.ts`（nav 已在 rbac-kernel 配置 `system:dept:list`）
- 不改 role 相关 Java/Vue
- 不做用户创建/编辑
