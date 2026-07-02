# 任务：scaffold-server

## 1. 模块骨架

- [x] 1.1 根 `pom.xml` 增加 `relayflow-server` module
- [x] 1.2 创建 `relayflow-server/pom.xml`（依赖三 `*-biz`，spring-boot-maven-plugin）
- [x] 1.3 验证：`./mvnw -pl relayflow-server -am compile`

## 2. 启动类与配置

- [x] 2.1 创建 `RelayflowServerApplication.java`
- [x] 2.2 创建 `application.yml`（application name + port）
- [x] 2.3 验证：`./mvnw -pl relayflow-server -am spring-boot:run`（启动成功即可停）

## 3. 门禁

- [x] 3.1 `openspec validate scaffold-server --strict`
