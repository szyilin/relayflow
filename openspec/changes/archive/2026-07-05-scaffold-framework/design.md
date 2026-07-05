# 设计：scaffold-framework

## 模块列表

```text
relayflow-framework/
├── relayflow-common/
├── relayflow-spring-boot-starter-web/
├── relayflow-spring-boot-starter-security/
├── relayflow-spring-boot-starter-mybatis/
├── relayflow-spring-boot-starter-redis/
├── relayflow-spring-boot-starter-websocket/
└── relayflow-spring-boot-starter-oss/
```

## 原则

- 本 change **不**写 AutoConfiguration、Filter、Interceptor
- 版本一律来自 `relayflow-dependencies` BOM
- 每个 jar 模块一个 `package-info.java`

## 验证

```bash
./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-web -am compile
./mvnw compile
```

## 前置

- `scaffold-maven-parent` ✓
