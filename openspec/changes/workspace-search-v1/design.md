# 设计：工作台全局搜索 V1（workspace-search-v1）

## Context

- UI 真源：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)、[`WorkspaceRailHeader.vue`](../../../web/src/components/workspace/WorkspaceRailHeader.vue)
- 可搜数据源已就绪：通讯录成员（`system`）、IM 会话（`im`）、个人任务（`task`）
- 架构：跨域只走 `*-api`；聚合编排放 **`infra-biz`**（与通知中心同属工作台基础设施）
- bootstrap：**全文搜索属 V2**；V1 用 SQL `LIKE` / 现有分页 API 扩展即可

## Goals / Non-Goals

**Goals:**

- 飞书式 **⌘K 唤起** + Rail 搜索框可用
- 单次聚合 API 返回 **分组结果**（成员 / 会话 / 任务），每组 Top N（默认 5）
- 点击跳转：成员 → `/app/contacts` 并高亮；会话 → `/app/messages` 并 `selectConversation`；任务 → `/app/tasks?taskId=`
- 租户隔离：所有子查询带 JWT `tenant_id`

**Non-Goals:**

- 消息正文检索、ES、搜索历史、管理端搜索

## Decisions

### D1：聚合层位置

```text
AppWorkspaceSearchController (infra-biz)
  → WorkspaceSearchService
       ├── MemberUserApi.searchMembers(keyword, limit)     // system-api
       ├── ImConversationApi.searchConversations(...)        // im-api
       └── TaskItemApi.searchTasks(...)                      // task-api
```

- `infra-biz` pom 增加对 `system-api`、`im-api`、`task-api` 的依赖（仅 api）
- 各 `*ApiImpl` 留在各自 `*-biz`，由 server  classpath 注入

**备选**：前端并行调三个 REST → 拒绝；重复鉴权、无统一限流与分组契约。

### D2：各域搜索语义（V1）

| 域 | 匹配字段 | 过滤 |
|----|----------|------|
| 成员 | `nickname`、`mobile`（normalize） | 当前租户 `ACTIVE` 成员 |
| 会话 | 单聊对方昵称、群名 `title` | 当前用户为成员的 `direct`/`group` |
| 任务 | `title` LIKE | `assignee_id = 当前用户` |

每域 **最多返回 5 条**；聚合 API 默认 `limitPerGroup=5`，总响应 < 20 条。

### D3：聚合 REST

`GET /app-api/infra/workspace-search`

| Query | 说明 |
|-------|------|
| `keyword` | 必填，trim 后长度 ≥ 1，≤ 50 |
| `limitPerGroup` | 可选，默认 5，最大 10 |

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
      "items": []
    }
  ]
}
```

空关键词 → `400` 或返回空 groups（实现选 **400 `SEARCH_KEYWORD_REQUIRED`**）。

鉴权：JWT + 有效成员；**不**检查 `sys_permission`。

### D4：跨域 API 形状（`*-api`）

```java
// system-api
List<MemberSearchRespDTO> searchMembers(Long tenantId, String keyword, int limit);

// im-api
List<ConversationSearchRespDTO> searchConversations(Long tenantId, Long userId, String keyword, int limit);

// task-api
List<TaskSearchRespDTO> searchTasks(Long tenantId, Long userId, String keyword, int limit);
```

各域 **同时** 暴露 app REST（便于单域调试与后续独立调用）：

| 方法 | 路径 |
|------|------|
| GET | `/app-api/system/member/search?keyword=&limit=` |
| GET | `/app-api/im/conversation/search?keyword=&limit=` |
| GET | `/app-api/task/item/search?keyword=&limit=` |

聚合层调 `*Api` 接口；Controller 与 ApiImpl 可薄封装同一 Service 方法。

### D5：前端结构

```text
web/src/
├── components/workspace/WorkspaceSearchModal.vue   # UModal + 分组列表
├── composables/useWorkspaceSearchShortcut.ts       # ⌘K / Ctrl+K
├── stores/workspaceSearch.ts
├── api/app/workspace-search.ts
└── components/workspace/WorkspaceRailHeader.vue    # 启用 UInput + 打开 Modal
```

| 交互 | 行为 |
|------|------|
| 点击 Rail 搜索框 | 打开 Modal 并 focus 输入 |
| ⌘K / Ctrl+K | 全局打开 Modal（工作台 layout 注册） |
| 输入防抖 | 300ms debounce 后请求 |
| 空结果 | `UEmpty`「无匹配结果」 |
| 跳转 | `payload.route` 或 item.route → `router.push`；messages/contacts 页读取 query 激活上下文 |

Store：

```ts
search(keyword: string): Promise<SearchGroup[]>
clear()
```

### D6：性能与限流

- 关键词最短 1 字符；中文单字允许
- 后端单用户 **10 req/s** 软限流（可选 V1 用 `@RateLimiter` 或简单 Redis 计数；未就绪则文档注明后续）
- 各域 SQL 必须走索引字段；`task_item` 已有 `assignee` 索引；成员走 `tenant_id` + nickname

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 三域串行慢 | `CompletableFuture` 并行调用 *Api |
| 会话预览需拉最后消息 | V1 subtitle 用会话 title/对方名，不查 `im_message` |
| 大租户 LIKE 慢 | limit 5 + 仅 V1；V2 上 ES |

## Migration Plan

纯增量 API 与前端组件；回滚移除聚合 Controller 并将搜索框设回 disabled。

## 验证

```bash
openspec validate workspace-search-v1 --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 浏览器：⌘K → 输入成员名 → 跳转通讯录
```

## 看板登记

| 切片 | 页面 | 端点 |
|------|------|------|
| workspace-search | Rail + Modal | `GET /app-api/infra/workspace-search` |

## 子 change 边界

| 子 change | 范围 |
|-----------|------|
| `workspace-search-web` | Modal + shortcut + Mock + `openspec/lanes/workspace-search/contract.md` |
| `workspace-search-api` | 三域 search + infra 聚合 |
| `workspace-search-integrate` | store 接 API、query 深链、看板 done |
