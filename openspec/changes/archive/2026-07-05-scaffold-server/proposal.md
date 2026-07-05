# 提案：relayflow-server 启动模块（scaffold-server）

## Why

三域空模块与 framework 就绪后，需要可运行的 Spring Boot 入口，验证「一个 server 加载各 *-biz」的 V1 部署形态。

## What Changes

- 新增 `relayflow-server` 模块（`spring-boot-maven-plugin`）
- 主类 `RelayflowServerApplication`
- 最小 `application.yml`（端口、应用名，无数据库连接）
- `pom.xml` 依赖 `system-biz`、`infra-biz`、`im-biz`
- 根 POM 增加 module

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无）

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-server/` | 新建 |
| 根 `pom.xml` | 增加 module |

**前置**：`scaffold-maven-parent`、`scaffold-framework`
