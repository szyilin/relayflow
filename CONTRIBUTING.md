# 贡献指南

本文档约定 RelayFlow 的 Git 协作、提交与版本发布习惯。编码与 API 细节见 [`docs/dev/`](docs/dev/)。

## Git 协作

### 分支

| 分支 | 用途 |
|------|------|
| `main` | 主分支，当前可部署/可集成的代码 |
| `feature/<change-name>` | 可选：对应一个 OpenSpec change 的功能分支 |

**现阶段**：允许直接向 `main` push（小团队快速迭代）。后续若引入 PR review，再收紧为「仅通过 PR 合并」。

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

| 类型 | 规则 | 示例 |
|------|------|------|
| 产品 / Maven | SemVer | `1.0.0`、`1.0.0-SNAPSHOT` |
| Git / Docker 镜像 tag | 与发布版本对齐 | `v1.0.0` → `relayflow:1.0.0` |
| Flyway 迁移 | 前三段跟 **产品 SemVer**，第四段为同版本内序号 | `V1.0.0.1__init_tenant.sql`、`V1.1.0.1__add_im_message.sql` |

Flyway 文件名格式：`V{major}.{minor}.{patch}.{seq}__{description}.sql`。  
同一 Release 多条 SQL 时使用 `V1.0.0.1`、`V1.0.0.2`…；详细规则见 [`docs/dev/database.md`](docs/dev/database.md)。

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
