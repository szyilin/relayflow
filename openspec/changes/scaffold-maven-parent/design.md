# 设计：scaffold-maven-parent

## 1. 范围

**In scope**

- 根 POM + Maven Wrapper
- `relayflow-dependencies` BOM
- system / infra / im 三域 `*-api` + `*-biz` 空模块

**Out of scope**

- `relayflow-framework/*` → `scaffold-framework`
- `relayflow-server` → `scaffold-server`
- `relayflow-module-bpm`（V1.1）
- 任何 Controller、Service、Flyway、Spring Boot 插件配置

## 2. 版本与坐标

| 项 | 值 |
|----|-----|
| `groupId` | `com.relayflow` |
| `artifactId`（根） | `relayflow` |
| `version` | `1.0.0-SNAPSHOT` |
| Java | 21 |
| Spring Boot（BOM） | 3.4.x |

## 3. 模块树（本 change 产出）

```text
pom.xml
relayflow-dependencies/pom.xml
relayflow-module-system/pom.xml
relayflow-module-system/relayflow-module-system-api/pom.xml
relayflow-module-system/relayflow-module-system-biz/pom.xml
relayflow-module-infra/...
relayflow-module-im/...
```

## 4. 依赖规则（POM 层 enforce）

```text
*-biz → 本域 *-api（同 version）
禁止 *-biz → 其他 *-biz（本 change 各 biz 无跨域依赖）
```

## 5. 生成方式

1. 手写最小根 POM（符合 archive design 模块列表）
2. 运行 `mvn wrapper:wrapper` 生成 Wrapper（需本机已装 Maven；或从 Spring 官方 wrapper 文件复制）
3. BOM 通过 `import` Spring Boot BOM；其余版本后续 change 追加
4. 每 jar 模块一个 `package-info.java` 占位

## 6. 验证

```bash
./mvnw -pl relayflow-module-system/relayflow-module-system-biz -am compile
```

或全量：

```bash
./mvnw compile
```

## 7. 参考

- `openspec/changes/archive/2026-06-30-bootstrap-v1-foundation/design.md` §3–§5
- `docs/dev/architecture.md`
