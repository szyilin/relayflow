# Tasks：agent-module-scaffold `[平台]`

## 1. BOM + Maven 模块

- [x] 1.1 `relayflow-dependencies`：property `spring-ai.version=1.1.8`；import `spring-ai-bom`；登记 `agent-api` / `agent-biz`
- [x] 1.2 创建 `relayflow-module-agent`（parent pom + api + biz），复制 task 域模式
- [x] 1.3 biz 依赖：own-api、web/security/mybatis/tenant starters、`spring-ai-openai`、`spring-ai-client-chat`（无 openai starter）
- [x] 1.4 根 `pom.xml` modules 增加 `relayflow-module-agent`；`relayflow-server` 依赖 `agent-biz`

## 2. 包结构与约定文档

- [x] 2.1 创建 `package-info.java`（根 + definition/builder/runtime/skill/session/channel/llm）
- [x] 2.2 `agent-api`：`ErrorCodeConstants` 占位（域号 009）
- [x] 2.3 `docs/dev/database.md` 增加 `agent_` 前缀行

## 3. 验证

- [x] 3.1 `./mvnw -pl relayflow-server -am compile -q`
- [x] 3.2 `openspec validate agent-module-scaffold --strict`
