## ADDED Requirements

### Requirement: framework Starter 模块骨架

仓库 SHALL 包含 `relayflow-framework` 聚合模块及 common、web、security、mybatis、redis、websocket、oss 等 Starter 子模块占位。

#### Scenario: framework 模块可编译

- **WHEN** 开发者执行 `./mvnw -pl relayflow-framework -am compile`
- **THEN** 各 Starter 空模块编译成功
- **AND** 版本依赖来自 `relayflow-dependencies` BOM
