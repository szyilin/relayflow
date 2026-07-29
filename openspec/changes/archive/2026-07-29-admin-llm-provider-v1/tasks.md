# Tasks：admin-llm-provider-v1

> 前置：`agent-module-scaffold` 已完成并可 compile。

## 1. Schema + codegen

- [x] 1.1 Flyway `V0.1.0.53__agent_llm_connection.sql`（表 + 权限种子）
- [x] 1.2 codegen.yml / CLI 支持 `--module agent`；生成 DO/Mapper 并 diff 合入
- [x] 1.3 `docs/dev/database.md` 已含 `agent_`（scaffold 已加）

## 2. 后端

- [x] 2.1 Service：CRUD、加密、保留空 Key、test 连通性
- [x] 2.2 Admin Controller + App list-enabled
- [x] 2.3 ErrorCodeConstants 补齐
- [x] 2.4 `./mvnw -pl relayflow-server -am compile`

## 3. 前端

- [x] 3.1 `api/admin/llmConnection.ts` + store
- [x] 3.2 页面 `/admin/infra/llm`；`useAdminNav` 入口
- [x] 3.3 `pnpm build` + `pnpm typecheck`

## 4. 契约与看板

- [x] 4.1 `openspec/lanes/admin-llm-provider/contract.md`
- [x] 4.2 更新 api-integration-board 状态
- [x] 4.3 `openspec validate admin-llm-provider-v1 --strict`
