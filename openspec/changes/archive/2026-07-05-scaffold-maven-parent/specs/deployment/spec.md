## ADDED Requirements

### Requirement: 业务域 api/biz 双模块骨架

已启用的业务域（system、infra、im）SHALL 各包含独立的 `relayflow-module-{domain}-api` 与 `relayflow-module-{domain}-biz` Maven 子模块。

#### Scenario: 三域模块结构存在

- **WHEN** 开发者查看 Maven 模块树
- **THEN** system、infra、im 各存在 `-api` 与 `-biz` 子模块
- **AND** 各 `-biz` 模块在脚手架阶段仅依赖本域 `-api`

#### Scenario: BOM 集中管理版本

- **WHEN** 开发者查看依赖版本定义
- **THEN** 存在 `relayflow-dependencies` BOM 模块
- **AND** Spring Boot 版本由 BOM 统一声明
