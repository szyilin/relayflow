# API 契约：admin-role-slice

> **状态**：已冻结（2026-07-08）  
> **起草**：`admin-role-slice` 全栈切片  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端角色 CRUD：功能权限绑定（`sys_role_permission`）、数据范围（`data_scope` + `sys_role_dept`）。

## 端点

### GET /admin-api/system/role/page

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:role:list` |
| Query | `pageNo`（默认 1）、`pageSize`（默认 20，最大 100）、`keyword`（可选，匹配名称/编码） |

**Response `data`**：

```json
{
  "list": [{
    "id": 100,
    "parentId": 0,
    "name": "超级管理员",
    "code": "super_admin",
    "roleType": "SYSTEM",
    "dataScope": "ALL",
    "canDelegate": 1,
    "sort": 0,
    "status": 0,
    "remark": null,
    "createTime": "2026-01-01T00:00:00Z"
  }],
  "total": 1
}
```

### GET /admin-api/system/role/get

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:query` |
| Query | `id`（必填） |

**Response `data`**：分页项字段 + `permissionIds: number[]`、`deptIds: number[]`（CUSTOM 时有值）

### POST /admin-api/system/role/create

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:create` |

**Request body**：

```json
{
  "parentId": 100,
  "name": "部门管理员",
  "code": "dept_admin",
  "dataScope": "DEPT",
  "canDelegate": 0,
  "sort": 10,
  "status": 0,
  "remark": "",
  "permissionIds": [1010, 1020],
  "deptIds": []
}
```

**Response `data`**：新建角色 `id`（number）

### PUT /admin-api/system/role/update

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:update` |

**Request body**：create 字段 + `id`（必填）

**Response `data`**：`true`

### DELETE /admin-api/system/role/delete

| 项 | 值 |
|----|-----|
| 所需权限 | `system:role:delete` |
| Query | `id`（必填） |

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
| 子集校验 | 子角色 `permissionIds` 必须是父角色权限的子集 |
| CUSTOM 范围 | `dataScope=CUSTOM` 时写入 `sys_role_dept` |
| 删除限制 | 存在子角色或已分配用户时拒绝删除 |

## curl 示例

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/role/page?pageNo=1&pageSize=10" | python3 -m json.tool

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/role/get?id=100" | python3 -m json.tool

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/system/permission/list | python3 -m json.tool
```

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/role.ts` |
| Store | `web/src/stores/role.ts`；**无 Mock 回退** |
| 页面 | `/admin/system/role` |
| Nav | `useAdminNav` 增加「角色管理」，需 `system:role:list` |
