# 任务：scaffold-maven-parent

> 单次会话建议只做 **1–2 个小节**；每节完成后运行对应验证命令。

## 1. 根工程与 Wrapper

- [x] 1.1 创建根 `pom.xml`（`packaging=pom`，声明 `relayflow-dependencies` 与三域聚合模块）
- [x] 1.2 生成 Maven Wrapper（`mvnw`、`mvnw.cmd`、`.mvn/wrapper/`）
- [x] 1.3 验证：`./mvnw -N validate`（仅根 POM）

## 2. BOM

- [x] 2.1 创建 `relayflow-dependencies/pom.xml`（import Spring Boot 3.4.x BOM）
- [x] 2.2 验证：`./mvnw -pl relayflow-dependencies validate`

## 3. system 域空模块

- [x] 3.1 创建 `relayflow-module-system` 聚合 POM + `*-api` + `*-biz` POM
- [x] 3.2 `*-api`、`*-biz` 各添加 `package-info.java`
- [x] 3.3 验证：`./mvnw -pl relayflow-module-system/relayflow-module-system-biz -am compile`

## 4. infra 域空模块

- [x] 4.1 创建 `relayflow-module-infra` 聚合 POM + `*-api` + `*-biz` POM
- [x] 4.2 `*-api`、`*-biz` 各添加 `package-info.java`
- [x] 4.3 验证：`./mvnw -pl relayflow-module-infra/relayflow-module-infra-biz -am compile`

## 5. im 域空模块

- [x] 5.1 创建 `relayflow-module-im` 聚合 POM + `*-api` + `*-biz` POM
- [x] 5.2 `*-api`、`*-biz` 各添加 `package-info.java`
- [x] 5.3 验证：`./mvnw -pl relayflow-module-im/relayflow-module-im-biz -am compile`

## 6. 全量门禁

- [x] 6.1 运行 `./mvnw compile`
- [x] 6.2 运行 `openspec validate scaffold-maven-parent --strict`
