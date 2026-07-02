## ADDED Requirements

### Requirement: relayflow-server 可启动骨架

`relayflow-server` SHALL 作为 Spring Boot 启动模块，依赖已启用的各 `*-biz` 模块。

#### Scenario: 空壳应用可启动

- **WHEN** 开发者执行 `./mvnw -pl relayflow-server -am spring-boot:run`
- **THEN** Spring Boot 应用成功启动并监听配置端口
- **AND** 无需数据库连接即可完成启动（脚手架阶段）
