# Git 与 IDE

本文说明 **哪些文件应纳入 Git**、**哪些必须忽略**，以及 **IntelliJ IDEA** 打开本仓库的方式。协作流程见 [CONTRIBUTING.md](../../CONTRIBUTING.md)。

## `.idea/` 要不要提交？

网上常见两种做法，**JetBrains 官方两种都认可**（见 [KB 206544839](https://intellij-support.jetbrains.com/hc/en-us/articles/206544839-How-to-manage-projects-under-Version-Control-Systems)）：

| 策略 | 做法 | 适用 |
|------|------|------|
| **A. 选择性提交** | 提交 `.idea/`，用 `.idea/.gitignore` 排除 `workspace.xml`、`shelf/` 等 | 需要共享 Run Configuration、Inspection 配置，且团队 **全员 IDEA** |
| **B. 整体忽略（本仓库采用）** | 根 `.gitignore` 写 `.idea/`，不提交任何 IDEA 工程文件 | **Maven/Gradle 多模块**：`pom.xml` 为真源，Clone 后 **Open pom.xml** 即可一致导入 |

JetBrains 原文要点：

- Maven/Gradle 项目：**`*.iml` 与 `.idea/modules.xml` 应在导入时自动生成**，提交它们容易造成无意义 diff。
- 若项目 **能从 Maven 干净导入**，另一种可行方案是：**不把任何 `.idea/` 文件放进版本库**。
- [GitHub `Global/JetBrains.gitignore`](https://github.com/github/gitignore/blob/master/Global/JetBrains.gitignore) 对 Maven auto-import 场景，也把忽略 `modules.xml`、`*.iml` 等列为推荐选项（模板内注释说明）。

**RelayFlow 选 B 的原因**：根工程是 Maven 多模块 + `mvnw`；编码与缩进用 **`.editorconfig`**（IDE 无关）；运行配置将来放 **`.run/`**（在项目根，不在 `.idea/` 里），仍可共享且不污染 IDE 私有目录。

---

## 纳入版本控制（应提交）

| 类别 | 路径 / 文件 | 原因 |
|------|-------------|------|
| 源码与构建描述 | `pom.xml`、`**/src/**`、`**/pom.xml` | **Maven 项目真源** |
| Maven Wrapper | `mvnw`、`mvnw.cmd`、`.mvn/wrapper/*` | 统一 Maven 版本 |
| 规格与文档 | `openspec/`、`docs/`、`README.md`、`AGENTS.md` | 需求与约定 |
| 跨 IDE 格式 | `.editorconfig`、`.gitattributes` | UTF-8、缩进；IDEA / Cursor 均可用 |
| 共享运行配置（将来） | `.run/*.xml` | Spring Boot 启动配置，**不**放在 `.idea/` |
| Cursor 共享规则 | `.cursor/rules/`、`.cursor/commands/`、`.cursor/skills/` | AI 工作流 |
| 占位目录 | `web/.gitkeep` 等 | 保留空目录结构 |

## 不纳入版本控制（必须忽略）

| 类别 | 路径 / 模式 | 原因 |
|------|-------------|------|
| **整个 IDEA 工程** | **`.idea/`** | 本地/用户相关，Maven 导入可重建 |
| IDEA 模块文件 | `*.iml`、`*.ipr`、`*.iws` | 随 Maven 导入生成 |
| IDEA 编译输出 | `out/` | 可重新编译 |
| 构建产物 | `target/`、`*.class` | 可 `./mvnw compile` 再生 |
| 密钥与环境 | `.env`、`.env.local` | 含密码、JWT secret |
| 前端依赖与产物 | `web/node_modules/`、`web/dist/` | `pnpm install` / `build` 生成 |
| 本地数据 | `data/`、`*.log` | Docker 卷、运行日志 |
| AI 工具本地缓存 | `.cursor/*`（除 rules/commands/skills） | 个人会话 |
| 业务 JAR | `*.jar`（**除** `.mvn/wrapper/maven-wrapper.jar`） | 构建输出 |

完整规则见根目录 [`.gitignore`](../../.gitignore)（IDEA 段对齐 GitHub JetBrains 模板思路）。

---

## IntelliJ IDEA

### 环境要求

| 项 | 值 |
|----|-----|
| IDEA | 2024.x+（Ultimate 或 Community 均可） |
| JDK | **21**（Project SDK = 21） |
| Maven | **Wrapper**（`mvnw`） |

### 首次打开

1. **File → Open** → 选仓库根目录的 **`pom.xml`**（不要只打开子模块）。
2. **Trust Project** → 等待 Maven 导入（右侧 Maven 窗口出现各模块）。
3. **File → Project Structure → Project**：SDK = **21**，Language level = **21**。
4. **Settings → Build → Build Tools → Maven**：
   - **Maven home path** → **Wrapper**
   - 勾选 **Reload project after changes in the build scripts**（可选）
5. **Settings → Editor → Code Style**：启用 **Enable EditorConfig support**（读取仓库 `.editorconfig`）。

首次打开后本地会生成 `.idea/` 与 `*.iml`，**已在 `.gitignore` 中，不要提交**。

### 推荐插件（引入 Lombok/MapStruct 后）

| 插件 | 用途 |
|------|------|
| Lombok | DO / DTO |
| MapStruct Support | DTO 转换 |

### Maven 操作

```bash
./mvnw compile
./mvnw -pl relayflow-module-system/relayflow-module-system-biz -am compile
```

### 共享 Run Configuration（`scaffold-server` 之后）

在 IDEA 中可将运行配置存到项目根 **`.run/`** 目录（Store as project file），该目录 **可以提交 Git**，且不依赖 `.idea/`。例如：

```text
.run/RelayflowServerApplication.run.xml
```

### 前端 `web/`（脚手架完成后）

```bash
cd web && pnpm install && pnpm dev
```

### 常见问题

| 现象 | 处理 |
|------|------|
| Git 里出现 `.idea/` 或 `*.iml` | 勿 add；确认 `.gitignore` 含 `.idea/` |
| 模块与 `pom.xml` 不一致 | Maven → **Reload All Maven Projects** |
| JDK 不是 21 | Project Structure → Project SDK → 21 |
| 想共享代码风格又不想提交 `.idea/` | 用 `.editorconfig`；或团队统一 IDEA Code Style 导出为 XML 放 `docs/`（可选） |

## 与 OpenSpec 的配合

- 一个 change 对应一次聚焦提交。
- 合并前运行 change 要求的验证（如 `./mvnw compile`、`openspec validate <change> --strict`）。
