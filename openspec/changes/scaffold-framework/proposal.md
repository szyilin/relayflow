# 提案：framework 空 Starter 模块（scaffold-framework）

## Why

Maven 父工程与三域空模块就绪后，需要 `relayflow-framework` 层承载公共 Starter（web、security、mybatis 等）。本 change **只建空模块与 POM 依赖关系**，不实现业务逻辑。

## What Changes

- 新增 `relayflow-framework` 聚合 POM
- 新增 7 个子模块空壳：`relayflow-common`、`starter-web`、`starter-security`、`starter-mybatis`、`starter-redis`、`starter-websocket`、`starter-oss`
- 根 POM 增加 `relayflow-framework` module
- 各 starter 仅声明对 Spring Boot 与 common 的最小依赖，含 `package-info.java`

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无）

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-framework/` | 新建 |
| 根 `pom.xml` | 增加 module |
| `*-biz` 模块 | 本 change 不依赖 framework |

**前置 change**：`scaffold-maven-parent` 已完成
