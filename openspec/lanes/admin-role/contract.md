# API 契约：admin-role（管理员角色）

> **状态**：修订中（`admin-roles-simplify`，2026-07-27）  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端「管理员角色」CRUD：功能权限绑定（`sys_role_permission`）、角色成员（`sys_user_role`）。**无**角色级数据范围（已删除 `data_scope` / `sys_role_dept`）。

## 端点

### GET /admin-api/system/role/page

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:role:list` |
| Query | `pageNo`（默认 1）、`pageSize`（默认 20，最大 100）、`keyword`（可选，匹配**名称**） |

**Response `data`**：

```json
{
  "list": [{
    "id": 100,
    "parentId": 0,
    "name": "超级管理员",
    "code": "super_admin",
    "roleType": "SYSTEM",
    "canDelegate": 1,
    "sort": 0,
    "status": 0,
    "remark": null,
    "createTime": "2026-01-01T00:00:00Z",
    "permissionSummary": "系统管理",
    "admins": [
      { "userId": 1, "nickname": "管理员", "avatar": null }
    ]
  }],
  "total": 1
}
```

| 字段 | 说明 |
|------|------|
| `code` | 系统角色有值（如 `super_admin`）；自定义角色为 `null`；**前端不展示** |
| `admins` | 该角色成员摘要（最多若干条，供列表展示） |
| `permissionSummary` | 已绑权限的简短文案（一级模块名去重拼接） |

### GET /admin-api/system/role/get

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:query` |
| Query | `id`（必填） |

**Response `data`**：分页项字段 + `permissionIds: number[]`（**无** `deptIds` / `dataScope`）

### POST /admin-api/system/role/create

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:create` |

**Request body**：

```json
{
  "parentId": 0,
  "name": "部门管理员",
  "canDelegate": 0,
  "sort": 10,
  "status": 0,
  "remark": "",
  "permissionIds": [1010, 1020]
}
```

| 字段 | 说明 |
|------|------|
| `parentId` | **可选**；省略 / `null` / `0` = 一级角色 |
| `name` | 必填 |
| `permissionIds` | 功能权限 ID 列表 |
| — | **不接受** `code`、`dataScope`、`deptIds`；服务端 CUSTOM 强制 `code=null` |

**Response `data`**：新建角色 `id`（number）

### PUT /admin-api/system/role/update

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:update` |

**Request body**：create 字段 + `id`（必填）；SYSTEM 角色仍不可改。

**Response `data`**：`true`

### DELETE /admin-api/system/role/delete

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:delete` |
| Query | `id`（必填） |

**Response `data`**：`true`

### GET /admin-api/system/role/members

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:query` |
| Query | `roleId`（必填） |

**Response `data`**：`[{ "userId", "nickname", "username", "avatar", "mobile", "deptName" }]`

### POST /admin-api/system/role/add-members

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:update` |

```json
{ "roleId": 100, "userIds": [1, 2] }
```

**Response `data`**：`true`（幂等写入 `sys_user_role`）

### POST /admin-api/system/role/remove-member

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:update` |

```json
{ "roleId": 100, "userId": 2 }
```

**Response `data`**：`true`

### GET /admin-api/system/permission/list

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:query`（只读权限树，供勾选） |

**Response `data`**：权限树数组，节点含 `id`、`parentId`、`name`、`code`、`type`、`sort`、`children`

## 业务规则

| 规则 | 说明 |
|------|------|
| SYSTEM 角色 | `roleType=SYSTEM` 不可修改、不可删除 |
| 子集校验 | 有上级时，子角色 `permissionIds` 必须是父角色权限的子集 |
| `code` | 仅 SYSTEM 锚点（如 `super_admin`）；CUSTOM 一律 `NULL` |
| 删除限制 | 存在子角色或已分配用户时拒绝删除 |
| 数据范围 | **已移除**；有功能权限即看全量对应管理数据 |

## curl 示例

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/role/page?pageNo=1&pageSize=10" | python3 -m json.tool

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/role/members?roleId=100" | python3 -m json.tool
```

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/role.ts` |
| Store | `web/src/stores/role.ts`；**无 Mock 回退** |
| 页面 | `/admin/system/role`（标题「管理员角色」） |
| Nav | `useAdminNav`「管理员角色」，需 `system:role:list` |
