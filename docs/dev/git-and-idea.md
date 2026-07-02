# Git 与 IDE

本文说明 **哪些文件应纳入 Git**、**哪些必须忽略**，以及 **IntelliJ IDEA** 打开本仓库的方式。协作流程见 [CONTRIBUTING.md](../../CONTRIBUTING.md)。

## 纳入版本控制（应提交）

| 类别 | 路径 / 文件 | 原因 |
|------|-------------|------|
| 源码与构建描述 | `pom.xml`、`**/src/**`、`**/pom.xml` | 项目真源 |
| Maven Wrapper | `mvnw`、`mvnw.cmd`、`.mvn/wrapper/*` | 统一 Maven 版本，离线可构建 |
| 规格与文档 | `openspec/`、`docs/`、`README.md`、`AGENTS.md` | 需求与约定 |
| 协作配置 | `CONTRIBUTING.md`、`.editorconfig`、`.gitattributes` | 团队一致 |
| IDEA 共享配置 | `.idea/encodings.xml`、`compiler.xml`、`misc.xml`、`vcs.xml`、`codeStyles/` | JDK 21、UTF-8、编码；**不含**个人窗口布局 |
| Cursor 共享规则 | `.cursor/rules/`、`.cursor/commands/`、`.cursor/skills/` | AI 工作流 |
| 占位目录 | `web/.gitkeep`、`deploy/` 模板（将来） | 保留空目录结构 |

## 不纳入版本控制（必须忽略）

| 类别 | 路径 / 模式 | 原因 |
|------|-------------|------|
| 构建产物 | `target/`、`out/`、`*.class` | 可重新生成 |
| IDEA 个人状态 | `.idea/workspace.xml`、`.idea/shelf/` 等 | 窗口布局、本地 Shelf |
| IDEA 模块文件 | `*.iml` | 由 Maven 导入自动生成，以 `pom.xml` 为准 |
| 密钥与环境 | `.env`、`.env.local` | 含数据库密码、JWT secret |
| 前端依赖与产物 | `web/node_modules/`、`web/dist/` | 由 `pnpm install` / `build` 生成 |
| 本地数据 | `data/`、`*.log` | Docker 卷、运行日志 |
| AI 工具本地缓存 | `.cursor/*`（除 rules/commands/skills） | 个人会话状态 |
| 业务 JAR | `*.jar`（**除** `.mvn/wrapper/maven-wrapper.jar`） | 构建输出 |

完整规则见根目录 [`.gitignore`](../../.gitignore)。

## IntelliJ IDEA

### 环境要求

| 项 | 值 |
|----|-----|
| IDEA | 2024.x+（Ultimate 或 Community 均可） |
| JDK | **21**（Project SDK = 21） |
| Maven | 使用项目 **Maven Wrapper**（见下） |

### 首次打开

1. **File → Open**，选择仓库根目录下的 **`pom.xml`**（不要只打开子模块）。
2. 弹出对话框选 **Open as Project**；Trust Project。
3. 等待 Maven 导入完成（右侧 Maven 工具窗口应出现 `relayflow` 及子模块）。
4. **File → Project Structure → Project**：SDK = 21，Language level = 21。
5. **Settings → Build → Build Tools → Maven**：
   - **Maven home path**：选 **Wrapper**（`mvnw`），不要用本机随意版本的 Maven。
   - **User settings file**：默认 `~/.m2/settings.xml` 即可。
6. **Settings → Editor → Code Style → Scheme**：选 **Project**（与仓库 `.idea/codeStyles` 一致）。

### 推荐插件（后续业务代码时启用）

| 插件 | 用途 |
|------|------|
| Lombok | DO / DTO 注解（引入 Lombok 依赖后） |
| MapStruct Support | DTO 转换（引入 MapStruct 后） |

V1 脚手架阶段尚未引入 Lombok，可暂不安装。

### Maven 操作

在 IDEA 终端或 Maven 窗口执行：

```bash
./mvnw compile
./mvnw -pl relayflow-module-system/relayflow-module-system-biz -am compile
```

`relayflow-server` 脚手架完成后，运行配置将放在 `.run/`（纳入 Git），可直接点 Run。

### 前端 `web/`（脚手架完成后）

- 使用 **内置 Terminal**：`cd web && pnpm install && pnpm dev`
- 或在 IDEA 中 **Attach** 为额外模块；**不要**把 `node_modules` 加入 Git。

### 常见问题

| 现象 | 处理 |
|------|------|
| 模块列表与 `pom.xml` 不一致 | Maven 工具窗口 → **Reload All Maven Projects** |
| 出现大量 `*.iml` 变更 | 正常，已被 `.gitignore` 忽略，勿提交 |
| JDK 报 17/11 | Project Structure 改为 **21** |
| 注解处理器报错（Lombok/MapStruct 阶段） | Settings → Compiler → Annotation Processors → **Enable** |

## 与 OpenSpec 的配合

- 一个 change 对应一次聚焦提交（或一条 feature 分支）。
- 合并前运行 change 要求的验证（如 `./mvnw compile`、`openspec validate <change> --strict`）。
