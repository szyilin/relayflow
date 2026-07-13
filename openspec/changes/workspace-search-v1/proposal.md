# 提案：工作台全局搜索 V1（workspace-search-v1 · 母 change · 执行路线图）

## Why

工作台 Rail 顶部已有 **「搜索 (⌘K)」** 输入框（[`WorkspaceRailHeader.vue`](../../../web/src/components/workspace/WorkspaceRailHeader.vue)），但当前为 **disabled 占位**。消息、通讯录、任务均已接真实 API，用户却只能在各模块内分别查找，无法像飞书一样 **一处搜索人 / 会话 / 任务并跳转**。

本 change 交付 V1 最小全局搜索：**关键词聚合查询 + 分组结果 + 键盘唤起**；不引入 Elasticsearch（全文检索属 V2，见 bootstrap design）。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。

1. **聚合搜索 API**：`GET /app-api/infra/workspace-search` — 单次请求返回分组结果（成员、会话、任务）
2. **跨域编排**：`infra-biz` 通过 `system-api` / `im-api` / `task-api` 并行查询，**禁止** `infra-biz` 直连他域表
3. **各域搜索契约**：为 system / im / task 增加轻量 `search` 端点或 `*Api.search*` 方法（供聚合层调用；实现细节见 design）
4. **前端**：启用 Rail 搜索框；`⌘K` / `Ctrl+K` 打开 `WorkspaceSearchModal`；点击结果 `router.push` 到 `/app/contacts`、`/app/messages`、`/app/tasks`
5. **看板**：`api-integration-board` 登记 `workspace-search` 切片

## Capabilities

### New Capabilities

- `workspace-search`：工作台全局搜索聚合 API、UI 与跨模块搜索契约

### Modified Capabilities

- `system`：成员关键词搜索（供聚合层）
- `im`：会话列表关键词搜索（供聚合层）
- `task`：任务标题关键词搜索（供聚合层）
- `infra`：新增 workspace-search 聚合 REST 端点

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| 后端 | `relayflow-module-infra-biz` | `WorkspaceSearchController` + 聚合 Service |
| 后端 | `system-biz` / `im-biz` / `task-biz` | 各域 search 实现（或扩展现有 page API） |
| 后端 | `*-api` | 可选 `XxxSearchApi` 跨域接口 |
| 前端 | `WorkspaceRailHeader`、`WorkspaceSearchModal`、`stores/workspaceSearch.ts` | 去 disabled、Modal、store |
| 规格 | `openspec/specs/workspace-search`（新建）、delta `system`/`im`/`task`/`infra` | 归档时同步 |

**回滚**：前端恢复 disabled 搜索框；聚合 API 可下线而不影响各域原有 CRUD。无破坏性迁移。

## 非目标

- Elasticsearch / OpenSearch / 消息正文全文检索
- 云文档、审批单、文件内容搜索
- 搜索历史同步、拼音/模糊分词、搜索建议热词
- 管理端全局搜索
- 独立 `relayflow-module-search` Maven 域（V1 聚合放 `infra-biz`）

## 前置

- [`workspace-contacts`](../archive/2026-07-12-workspace-contacts/proposal.md)、[`im-direct-chat`](../archive/2026-07-12-im-direct-chat-web/proposal.md)、[`workspace-tasks-v1`](../archive/2026-07-12-workspace-tasks-v1/proposal.md) 已归档并联调
- 无硬依赖 `notify-inbox-v2` / `bpm-v1`（可并行规划）

## 子 change 切片（实现顺序）

```text
workspace-search-v1              ← 本 change（规划母版）
├── workspace-search-web         UI + Mock + contract + ⌘K
├── workspace-search-api         各域 search + infra 聚合 REST
└── workspace-search-integrate   去 Mock、端到端、看板 done
```

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `workspace-search-v2` | ES 全文、消息内容、文档 |
| `workspace-search-history` | 最近搜索、服务端记录 |
