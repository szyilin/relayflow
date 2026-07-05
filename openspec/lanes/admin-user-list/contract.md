# API 契约：admin-user-list

> **状态**：已冻结（2026-07-05）  
> **起草**：前端对接切片  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端用户列表分页展示租户内成员。

## 端点

### GET /admin-api/system/user/page

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| Query | `pageNo`（默认 1）、`pageSize`（默认 20，最大 100）、`keyword`（可选，匹配用户名/昵称） |

**Response `data`**：

```json
{
  "list": [{
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "dept": "总部",
    "status": 0,
    "createTime": "2026-01-01T00:00:00Z"
  }],
  "total": 1
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | number | `0` 启用 · `1` 禁用（租户成员状态） |
| `dept` | string | 主部门名；无部门可省略 |

**curl**：

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin-api/system/user/page?pageNo=1&pageSize=5"
```

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/user.ts` → `getUserPage()` |
| Store | `stores/user.ts`；**无 Mock 回退** |
| 页面 | `/admin/system/user` |
