# 任务：scaffold-server

## 1. 模块骨架

- [ ] 1.1 根 `pom.xml` 增加 `relayflow-server` module
- [ ] 1.2 创建 `relayflow-server/pom.xml`（依赖三 `*-biz`，spring-boot-maven-plugin）
- [ ] 1.3 验证：`./mvnw -pl relayflow-server -am compile`

## 2. 启动类与配置

- [ ] 2.1 创建 `RelayflowServerApplication.java`
- [ ] 2.2 创建 `application.yml`（application name + port）
- [ ] 2.3 验证：`./mvnw -pl relayflow-server -am spring-boot:run`（启动成功即可停）

## 3. 门禁

- [ ] 3.1 `openspec validate scaffold-server --strict`
