# API 契约：workspace-search

> **状态**：草案（母 change 规划；`workspace-search-web` lane 实施时细化）  
> **起草**：`workspace-search-v1` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

工作台 Rail 顶部「搜索 (⌘K)」当前为 disabled 占位。本切片交付 **聚合搜索**：单次请求返回成员 / 会话 / 任务分组结果，支持键盘唤起与点击跳转。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## 聚合 REST

### GET /app-api/infra/workspace-search

**Query**：

| 参数 | 必填 | 说明 |
|------|------|------|
| `keyword` | 是 | trim 后 1–50 字符 |
| `limitPerGroup` | 否 | 默认 5，最大 10 |

**Response `data`**：

```json
{
  "keyword": "张",
  "groups": [
    {
      "type": "member",
      "label": "联系人",
      "items": [
        {
          "id": "1001",
          "title": "张三",
          "subtitle": "研发部",
          "route": "/app/contacts?memberId=1001",
          "entityType": "member",
          "entityId": "1001"
        }
      ]
    },
    {
      "type": "conversation",
      "label": "消息",
      "items": [
        {
          "id": "2001",
          "title": "张三",
          "subtitle": "上次消息预览…",
          "route": "/app/messages?conversationId=2001",
          "entityType": "conversation",
          "entityId": "2001"
        }
      ]
    },
    {
      "type": "task",
      "label": "任务",
      "items": [
        {
          "id": "3001",
          "title": "整理周报",
          "subtitle": "TODO",
          "route": "/app/tasks?taskId=3001",
          "entityType": "task",
          "entityId": "3001"
        }
      ]
    }
  ]
}
```

空关键词 → `SEARCH_KEYWORD_REQUIRED`。

## 各域调试端点（聚合层内部亦调用 `*-api`）

| 方法 | 路径 | 匹配语义 |
|------|------|----------|
| GET | `/app-api/system/member/search?keyword=&limit=` | 租户内 `ACTIVE` 成员；`nickname`、`mobile` |
| GET | `/app-api/im/conversation/search?keyword=&limit=` | 当前用户为成员的 `direct`/`group`；标题或单聊对方名 |
| GET | `/app-api/task/item/search?keyword=&limit=` | `assignee_id = 当前用户`；`title` LIKE |

## 错误码

| code | 说明 |
|------|------|
| `SEARCH_KEYWORD_REQUIRED` | keyword 为空或仅空白 |

## 前端 UI

| 组件 | 说明 |
|------|------|
| `WorkspaceRailHeader` | 移除 `disabled`；点击搜索框打开 Modal |
| `WorkspaceSearchModal` | `UModal` + 分组列表 + `UEmpty` |
| `useWorkspaceSearchShortcut` | `⌘K` / `Ctrl+K` 全局唤起（`workspace` layout 注册） |
| `stores/workspaceSearch.ts` | `search(keyword)`；300ms debounce |

**跳转约定**：

- `member` → `/app/contacts?memberId=`，通讯录页高亮/选中成员
- `conversation` → `/app/messages?conversationId=`，消息页 `selectConversation`
- `task` → `/app/tasks?taskId=`，任务页打开/高亮任务

## curl 示例

```bash
curl -s 'http://localhost:8080/app-api/infra/workspace-search?keyword=张&limitPerGroup=5' \
  -H "Authorization: Bearer $TOKEN" | jq

curl -s 'http://localhost:8080/app-api/system/member/search?keyword=张&limit=5' \
  -H "Authorization: Bearer $TOKEN" | jq
```

## 浏览器验证路径

1. `/app/messages` 按 `⌘K` → Modal 打开并 focus 输入框
2. 输入成员名 → 联系人分组有结果 → 点击跳转 `/app/contacts?memberId=...`
3. 输入会话名 → 跳转 `/app/messages?conversationId=...`
4. 输入任务标题 → 跳转 `/app/tasks?taskId=...`

## 非目标（V1）

- Elasticsearch、消息正文全文、审批/文档纳入搜索
- 搜索历史、拼音分词、管理端搜索
