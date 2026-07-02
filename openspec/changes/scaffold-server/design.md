# 设计：scaffold-server

## 依赖

```text
relayflow-server
  ├── relayflow-module-system-biz
  ├── relayflow-module-infra-biz
  └── relayflow-module-im-biz
```

本 change 各 `*-biz` 仍为空壳，server 启动后仅暴露 Spring Boot 默认 actuator/health（若未引入则仅空上下文）。

## 配置

```yaml
spring:
  application:
    name: relayflow-server
server:
  port: 8080
```

数据库、Redis、Flyway **不在本 change**（`scaffold-deploy-compose` + 后续业务 change）。

## 验证

```bash
./mvnw -pl relayflow-server -am compile
./mvnw -pl relayflow-server -am spring-boot:run
# Ctrl+C 停止；能启动即通过
```
