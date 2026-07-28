## Why

需要独立 Maven 域承载 Agent 产品与 Spring AI Runtime，避免逻辑渗入 `im-biz` / `system-biz`。本切片为 **[平台]** 脚手架：模块可编译、包结构就位、BOM 引入 Spring AI，**不**实现业务 API。

## What Changes

- 新增 `relayflow-module-agent`（`api` + `biz`），`relayflow-server` 加载 `agent-biz`
- `relayflow-dependencies` BOM：Spring AI 版本与起步依赖
- 包结构空壳对齐架构：`definition` / `builder` / `runtime` / `skill` / `session` / `channel`
- 表前缀约定 `agent_`（本切片可不建表，或仅占位注释）

## Capabilities

### New Capabilities

- （无行为规格义务；平台脚手架）

### Modified Capabilities

- （无；或 `deployment` 提及 server 模块列表——深化时可选）

## Impact

- 根 `pom.xml`、server 依赖、BOM；**无**用户可见功能
- 母：`agent-platform-foundation`；解锁 `agent-runtime-core-v1`

## 非目标

- AgentDef CRUD、Chat、UI
