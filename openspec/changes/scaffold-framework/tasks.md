# 任务：scaffold-framework

> 每节独立可验证；建议一次会话完成 1–2 节。

## 1. 聚合与 common

- [x] 1.1 根 `pom.xml` 增加 `relayflow-framework` module
- [x] 1.2 创建 `relayflow-framework/pom.xml` 聚合 POM
- [x] 1.3 创建 `relayflow-common` POM + `package-info.java`
- [x] 1.4 验证：`./mvnw -pl relayflow-framework/relayflow-common -am compile`

## 2. starter-web

- [ ] 2.1 创建 `relayflow-spring-boot-starter-web` POM（依赖 common + spring-boot-starter-web）
- [ ] 2.2 添加 `package-info.java`
- [ ] 2.3 验证：`./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-web -am compile`

## 3. starter-security

- [ ] 3.1 创建 starter-security POM + `package-info.java`
- [ ] 3.2 验证：`./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-security -am compile`

## 4. starter-mybatis

- [ ] 4.1 创建 starter-mybatis POM + `package-info.java`
- [ ] 4.2 验证：`./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-mybatis -am compile`

## 5. starter-redis

- [ ] 5.1 创建 starter-redis POM + `package-info.java`
- [ ] 5.2 验证：`./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-redis -am compile`

## 6. starter-websocket

- [ ] 6.1 创建 starter-websocket POM + `package-info.java`
- [ ] 6.2 验证：`./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-websocket -am compile`

## 7. starter-oss

- [ ] 7.1 创建 starter-oss POM + `package-info.java`
- [ ] 7.2 验证：`./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-oss -am compile`

## 8. 全量门禁

- [ ] 8.1 `./mvnw compile`
- [ ] 8.2 `openspec validate scaffold-framework --strict`
