# API 契约：admin-user-mutate

> **状态**：已冻结（2026-07-08）  
> **起草**：`admin-user-mutate-slice` 全栈切片  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端用户创建/编辑：维护租户成员基本信息、主部门归属、角色分配；用户列表按当前操作者 **data_scope** 过滤。

## 端点

### GET /admin-api/system/user/get

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:user:query` |
| Query | `id`（必填） |

**Response `data`**：

```json
{
  "id": 2,
  "username": "zhangsan",
  "nickname": "张三",
  "mobile": "13800138000",
  "email": "zhangsan@example.com",
  "status": 0,
  "deptId": 1,
  "roleIds": [100],
  "createTime": "2026-01-01T00:00:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | number | `0` 启用 · `1` 禁用（租户成员状态） |
| `deptId` | number \| null | 主部门 ID |
| `roleIds` | number[] | 已分配角色 ID 列表 |

### POST /admin-api/system/user/create

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `system:user:create` |

**Request body**：

```json
{
  "username": "zhangsan",
  "password": "Passw0rd!",
  "nickname": "张三",
  "mobile": "13800138000",
  "email": "zhangsan@example.com",
  "deptId": 1,
  "roleIds": [100]
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `username` | 是 | 全局唯一 |
| `password` | 是 | 明文，后端加密存储 |
| `deptId` | 否 | 主部门 |
| `roleIds` | 否 | 角色 ID 列表 |

**Response `data`**：新建用户 ID（number）。

### PUT /admin-api/system/user/update

| 项 | 值 |
|----|-----|
| 所需权限 | `system:user:update` |

**Request body**：

```json
{
  "id": 2,
  "nickname": "张三",
  "mobile": "13800138000",
  "email": "zhangsan@example.com"
}
```

**Response `data`**：`true`

### PUT /admin-api/system/user/update-status

| 项 | 值 |
|----|-----|
| 所需权限 | `system:user:update` |

**Request body**：

```json
{
  "id": 2,
  "status": 1
}
```

| `status` | 含义 |
|----------|------|
| `0` | 启用（ACTIVE） |
| `1` | 禁用（SUSPENDED） |

**Response `data`**：`true`

### PUT /admin-api/system/user/update-dept

| 项 | 值 |
|----|-----|
| 所需权限 | `system:user:update` |

**Request body**：

```json
{
  "id": 2,
  "deptId": 1
}
```

`deptId` 为 `null` 时清除主部门归属。

**Response `data`**：`true`

### PUT /admin-api/system/user/update-role

| 项 | 值 |
|----|-----|
| 所需权限 | `system:user:update` |

**Request body**：

```json
{
  "id": 2,
  "roleIds": [100, 101]
}
```

**Response `data`**：`true`

### GET /admin-api/system/user/page（增强）

在既有分页契约基础上，服务端按当前登录用户 **data_scope** 过滤可见用户：

| data_scope | 可见范围 |
|------------|----------|
| ALL | 租户内全部成员 |
| SELF | 仅本人 |
| DEPT / DEPT_AND_CHILD / CUSTOM | 主部门落在允许 deptIds 内的用户；与 SELF 取并集 |

## 业务规则

| 规则 | 说明 |
|------|------|
| 用户名唯一 | 创建时 `username` 全局不可重复 |
| 租户成员 | 用户须存在于 `sys_tenant_user` |
| 部门/角色 | 须属于当前租户 |
| 状态 | V1 UI 仅暴露 ACTIVE(0) / SUSPENDED(1) |

## curl 示例

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"Passw0rd!","nickname":"测试","deptId":1,"roleIds":[100]}' \
  http://localhost:8080/admin-api/system/user/create | python3 -m json.tool

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/user/get?id=2" | python3 -m json.tool
```

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/user.ts` |
| Store | `stores/user.ts`；**无 Mock 回退** |
| 页面 | `/admin/system/user/create` 新建；列表页编辑弹窗 |
| 部门/角色 | 从 `stores/dept`、`api/admin/role` 加载选项 |
| 权限 | 按钮按 `system:user:create/update` 显示 |

## 非目标（本切片）

- 重置密码、删除用户
- 用户组、邀请流
- 修改用户名
