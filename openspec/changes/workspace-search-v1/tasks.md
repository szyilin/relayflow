# Tasks：workspace-search-v1（母 change · 执行路线图）

> **用法**：本文件是工作台全局搜索 V1 的 **总路线图**。实际编码按 **子切片** 分批；每次会话 ≤10 条 checkbox。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）。  
> **设计真源**：`design.md`；**行为**：`specs/workspace-search` 及各域 delta。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate workspace-search-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `workspace-search`（planned）
- [x] 0.4 `specs-sync-checklist.md`（归档前核对清单）

---

## 1. workspace-search-web（前端 lane · 第一步）

**目标**：⌘K Modal + 启用 Rail 搜索 + contract 草案。

- [x] 1.1 起草 `openspec/lanes/workspace-search/contract.md`（聚合 API、分组形状、深链 query）
- [x] 1.2 `WorkspaceSearchModal.vue` + `useWorkspaceSearchShortcut.ts`
- [x] 1.3 `stores/workspaceSearch.ts` + `api/app/workspace-search.ts`（Mock 分组数据）
- [x] 1.4 `WorkspaceRailHeader`：移除 `disabled`；点击打开 Modal
- [x] 1.5 `workspace` layout 注册全局快捷键
- [x] 1.6 `cd web && pnpm build`
- [ ] 1.7 浏览器：⌘K → Mock 结果 → 点击跳转路径（可先 console 验证 route）

**验证**：`pnpm build` + 浏览器路径。

**完成后**：看板 web → `ui_ready`。

---

## 2. workspace-search-api（后端 lane）

**依赖**：§1 contract 就绪

- [ ] 2.1 `system-api` + `MemberUserApi.searchMembers` + `GET .../member/search`
- [ ] 2.2 `im-api` + `ImConversationApi.searchConversations` + `GET .../conversation/search`
- [ ] 2.3 `task-api` + `TaskItemApi.searchTasks` + `GET .../item/search`
- [ ] 2.4 `infra-biz`：`WorkspaceSearchService` 并行聚合 + `GET /app-api/infra/workspace-search`
- [ ] 2.5 `infra-biz` pom 依赖三域 `*-api`；禁止直连 Mapper
- [ ] 2.6 curl 验收三域 + 聚合；单测关键词必填
- [ ] 2.7 `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`。

---

## 3. workspace-search-integrate（联调）

- [ ] 3.1 store 去 Mock，接真实聚合 API
- [ ] 3.2 `/app/contacts`、`/app/messages`、`/app/tasks` 读取 query 并激活上下文
- [ ] 3.3 E2E：搜成员名 → 进通讯录；搜会话 → 进消息；搜任务 → 进任务
- [ ] 3.4 `openspec validate workspace-search-v1 --strict`
- [ ] 3.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`
- [ ] 3.6 看板 `workspace-search` → **done**

---

## 4. 母 change 归档前

- [ ] 4.1 确认 specs 与实现一致
- [ ] 4.2 `openspec archive workspace-search-v1`
- [ ] 4.3 `./mvnw verify`（如适用）

---

## 执行顺序速查

```text
Session 1   §0 + §1 web
Session 2   §2 api（可与 §1 并行，contract 冻结后）
Session 3   §3 integrate + §4 归档
```

## 后续 change

| Change | 说明 |
|--------|------|
| `workspace-search-v2` | ES 全文 |
| `bpm-v1` | 审批纳入搜索（V2） |

---

## 会话开场白模板

```text
Using change: workspace-search-web（workspace-search-v1 子切片 · 前端 lane）
Read: openspec/changes/workspace-search-v1/design.md §D5
Tasks: workspace-search-v1/tasks.md §1
```
