# 设计：workspace-docs-library-integrate

## Store 状态

| 字段 | 来源 |
|------|------|
| `treeRoots` | `GET /library/tree` |
| `recentItems` | `GET /recent` |
| `activeDocument` | `GET /documents/{objectId}` |

不再维护 `localObjects` / `localNodes`。

## 写操作后刷新

| 操作 | API | 本地更新 |
|------|-----|----------|
| 新建 | `POST /library/nodes` | 重载树 + `openDocument` |
| 重命名 | `PUT /library/nodes/{nodeId}` | 补丁树标题 + active |
| 移动 | `PUT /library/nodes/{nodeId}` parentId | 重载树 |
| 删除 | `DELETE /library/nodes/{nodeId}` | 重载树 + 最近 |
| 打开 | `GET /documents/{objectId}` | active + 重载最近 |
| 保存正文 | `PUT …/body` | 更新 contentVersion |
| 保存标题 | `PUT …/nodes/{nodeId}` title | 由 objectId 查 nodeId |
| 导出 MD | `GET …/export?format=md` | 无持久状态 |

## nodeId 解析

标题保存走 node API：自 `treeRoots` 递归 `findNodeByObjectId(objectId)`。

## 错误

`ApiError.code === 1006001003` → 用户可见「正文版本冲突，请刷新后重试」（沿用服务端 msg）。

## 租户切换

`resetLocal()` 清空 tree/recent/active/hydrated；页面 `ensureHydrated()` 再拉。
