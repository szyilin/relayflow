# 提案：Maven 父工程与三域空模块（scaffold-maven-parent）

## Why

架构与规格已定，但仓库尚无 `pom.xml` 与 Java 模块目录。本变更是 **V1 后端写代码的第一步**：建立 Maven 多模块骨架，使 `./mvnw compile` 可通过，为后续 framework、server 与业务域 change 提供构建基础。

## What Changes

- 新增根 `pom.xml`（`packaging=pom`）与 Maven Wrapper（`mvnw`）
- 新增 `relayflow-dependencies` BOM（Spring Boot 3.4.x 等版本集中管理）
- 新增三域聚合模块及空子模块：
  - `relayflow-module-system` → `*-api`、`*-biz`
  - `relayflow-module-infra` → `*-api`、`*-biz`
  - `relayflow-module-im` → `*-api`、`*-biz`
- 各 `*-biz` 仅依赖本域 `*-api`；**不**创建 framework、server、Gateway
- 各 jar 模块含最小 `package-info.java` 以保证 `compile` 通过

## Capabilities

### New Capabilities

（无 — 本 change 为实现已有 deployment 规格中的 Maven 布局，不新增行为需求）

### Modified Capabilities

（无 — 不修改 `openspec/specs/` 行为真源）

## Impact

| 区域 | 影响 |
|------|------|
| 仓库根目录 | 新增 `pom.xml`、`mvnw`、`.mvn/` |
| `relayflow-dependencies/` | 新建 BOM |
| `relayflow-module-*/` | 新建 3 聚合 + 6 子模块空 POM |
| `relayflow-framework/`、`relayflow-server/` | **不在本 change** |
| `web/`、`deploy/` | 无 |
