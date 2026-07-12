# API 契约：admin-user-by-dept

> **状态**：草案（`-web` lane）  
> **起草**：`admin-user-by-dept` change  
> **基线**：[`admin-user-list`](../admin-user-list/contract.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端 `/admin/system/user` 按部门浏览成员：左侧部门树选中节点，右侧表格仅显示该部门**直属**主部门成员（V1 不含子部门递归）。

## 端点（扩展）

### GET /admin-api/system/user/page

在 [admin-user-list](../admin-user-list/contract.md) 基础上新增 Query：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `deptId` | long | 否 | 有值时仅返回主部门（`primary_flag=1`）等于该 id 的用户 |
| `pageNo` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 20，最大 100 |
| `keyword` | string | 否 | 用户名/昵称模糊 |

**行为**：

- `deptId` 与调用者 `data_scope` **取交集**
- V1 不实现 `includeChildren`；子部门成员仅在选中该子部门时出现

**Response `data`**：与 admin-user-list 相同（`list` + `total`）

**curl**：

```bash
TOKEN="<jwt>"
DEPT_ID=1

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/user/page?deptId=${DEPT_ID}&pageNo=1&pageSize=20"
```

## 前端约定

| 项 | 约定 |
|----|------|
| 页面 | `/admin/system/user` |
| 部门树 | `stores/dept.ts` → `buildTree`；默认选中根部门 |
| 用户表 | `stores/user.ts` → `fetchPage({ deptId, keyword, pageNo })` |
| 新建 | `/admin/system/user/create?deptId=<选中>` |

## V1 不在范围

- `includeChildren=true` 递归子部门
- 批量改部门 API
