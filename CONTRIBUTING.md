# 贡献指南

本文档约定 RelayFlow 的 Git 协作、提交与版本发布习惯。编码与 API 细节见 [`docs/dev/`](docs/dev/)。

## Git 协作

### 版本控制范围

哪些文件应提交、哪些必须忽略，以及 **IntelliJ IDEA 打开方式**（含 **为何不提交 `.idea/`**），见 **[`docs/dev/git-and-idea.md`](docs/dev/git-and-idea.md)**。

要点：

- **提交**：源码、`pom.xml`、Maven Wrapper、OpenSpec、`.editorconfig`、将来 `.run/` 运行配置
- **不提交**：**整个 `.idea/`**、`*.iml`、`target/`、`.env`、`node_modules/`（Maven 导入可重建 IDE 工程）

详见 **[`docs/dev/git-and-idea.md`](docs/dev/git-and-idea.md)**（含 JetBrains / GitHub 官方依据说明）。

| 分支 | 用途 |
|------|------|
| `main` | 主分支，当前可部署/可集成的代码 |
| `feature/<change-name>` | 可选：对应一个 OpenSpec change 的功能分支 |

**现阶段**：允许直接向 `main` push（便于早期快速迭代）。后续若引入 PR review，再收紧为「仅通过 PR 合并」。

### 与 OpenSpec 的关系

- 一个 active change 尽量对应一条 feature 分支，避免单个 PR 跨多个 change。
- 实现范围以 `openspec/changes/<change>/tasks.md` 为准；合并前运行该 change 要求的验证命令。

## Commit Message

**使用中文**，推荐 [Conventional Commits](https://www.conventionalcommits.org/) 前缀：

```text
<type>(<scope>): <简述>

[可选正文]
```

| type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `refactor` | 重构（无行为变化） |
| `docs` | 文档 |
| `chore` | 构建、依赖、脚手架 |
| `test` | 测试 |

**scope** 示例：`system`、`infra`、`im`、`web`、`deploy`、`openspec`。

示例：

```text
feat(system): 实现 JWT 登录接口
fix(im): 修复群聊消息分页重复
docs: 补充 API 响应格式约定
chore(deploy): 更新 compose 中 Redis 镜像版本
```

## 版本号

**当前阶段**：产品尚未正式上线，Maven 版本为 **`0.x`**（当前 `0.1.0-SNAPSHOT`）。

| 类型 | 规则 | 示例 |
|------|------|------|
| 产品 / Maven | SemVer；**开发期 major 固定为 `0`** | `0.1.0`、`0.1.0-SNAPSHOT` |
| Git / Docker 镜像 tag | 与发布版本对齐 | `v0.1.0` → `relayflow:0.1.0` |
| Flyway 迁移 | 前三段跟 **产品 SemVer**，第四段为同版本内序号 | `V0.1.0.1__init_tenant.sql`、`V0.2.0.1__add_im_message.sql` |

Flyway 文件名格式：`V{major}.{minor}.{patch}.{seq}__{description}.sql`。  
同一 Release 多条 SQL 时使用 `V0.1.0.1`、`V0.1.0.2`…；详细规则见 [`docs/dev/database.md`](docs/dev/database.md)。

### 开发期版本纪律（AI 与人类贡献者必读）

- **禁止** AI 或贡献者自行将 **major 从 `0` 升到 `1`**（即不得改为 `1.0.0` / `1.x`）。**只有产品负责人明确宣布「准备正式上线」后**，才允许切到 `1.0.0`。
- **minor / patch** 可按开发进度自行调整（例如 `0.1.0` → `0.2.0` 或 `0.1.1`），须同步 Maven `pom.xml` 与 Flyway 文件名前三段。
- 架构文档中的 **「V1 单体 / Phase 2 微服务」** 指部署形态代号，**不是**产品 SemVer 的 major；勿与 `0.x` / `1.x` 产品版本混淆。
- **`0.x` 数据库**：允许在征得产品负责人同意后，合并/拆分/删除表且 **不做旧数据适配**；细则见 [`docs/dev/database.md`](docs/dev/database.md) §「开发期（`0.x`）schema 拆分与合并」。**禁止** AI 未经确认擅自破坏性改表。

## Pull Request（可选）

现阶段不强制 PR。若使用 PR：

- 标题与 commit 规范一致（中文 + type/scope）
- 说明关联的 OpenSpec change 名称
- 后端改动：`./mvnw test` 或 `./mvnw verify` 通过
- 前端改动：`cd web && pnpm build` 通过
- 规格改动：`openspec validate <change> --strict` 通过

## 禁止事项

- 提交密钥、`.env` 真实凭据、JWT secret 到仓库
- 未经讨论扩大 OpenSpec change 的 tasks 范围
- 跳过验证命令后声称「已完成」
