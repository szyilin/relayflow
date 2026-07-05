# 并行 Lane 工作流（OpenSpec 前后端拆分）

用户可见功能在 **契约冻结** 后，可拆成 **两个（或三个）OpenSpec change** 并行推进，以提升前后端 AI / 开发者效率。

> 单 change 纵向切片（`tasks.md` 内后端→前端→联调）仍有效；本工作流是其 **并行变体**，交付标准不变：**集成完成后浏览器可验证**。

## 何时拆分

| 场景 | 做法 |
|------|------|
| 带 UI 的业务切片（登录、壳层、用户列表…） | **推荐** `{slice}-api` + `{slice}-web` + `{slice}-integrate` |
| 纯平台、无 UI（Redis 前缀等） | 单个 change，标题 `[平台]` |
| 脚手架、`scaffold-*` | 单个 change |
| 管理端 UI 定调（Mock 原型） | 单个 `admin-ui-prototype`（仅 `web/`） |

## 命名约定

```text
{slice}-api        后端 lane（relayflow-module-*、framework、server）
{slice}-web        前端 lane（web/）
{slice}-integrate  集成 lane（联调、双端 validate、归档前门禁）
```

示例：`admin-shell-api`、`admin-shell-web`、`admin-shell-integrate`。

**禁止** 再用单个 `{slice}` change 同时写前后端（新切片一律按上表拆分；已归档的单 change 如 `admin-login-slice` 保留历史）。

## 共享契约

并行前必须先有 **API 契约**（字段、路径、鉴权、错误码）：

| 文件 | 职责 |
|------|------|
| `openspec/changes/_lanes/{slice}/contract.md` | 前后端 **只读握手**；由 `-api` lane 起草，`-web` lane 确认 |
| `{slice}-api/design.md` | 后端实现细节；须与 contract 一致 |
| `{slice}-web/design.md` | 前端对接细节；须引用 contract |

契约变更流程：**仅** `-api` 或人工修改 `contract.md` → 通知另一 lane 刷新 tasks。

## 三 Lane 职责

### `-api`（后端）

- **编辑范围**：`relayflow-module-*`、`relayflow-framework`、`relayflow-server`、Flyway、`deploy/`（若需要）
- **禁止**：改 `web/`（CORS/Security 白名单等联调必要项除外，须在 tasks 写明）
- **验证**：`./mvnw -pl relayflow-server -am compile`；涉及接口时 curl
- **Cursor 规则**：`.cursor/rules/parallel-lane-backend.mdc`

### `-web`（前端）

- **编辑范围**：`web/`
- **禁止**：改 Java 模块（`vite.config` proxy、`VITE_*` 除外）
- **验证**：`cd web && pnpm build`
- **Cursor 规则**：`.cursor/rules/parallel-lane-frontend.mdc`
- **前置**：可读 `-api` 的 contract；API 未就绪时可暂用 mock，但 integrate 前必须接真 API

### `-integrate`（集成）

- **前置**：`-api` 与 `-web` 的 **代码 tasks 均已完成**（checkbox 勾完）
- **职责**：本地联调、补跨端小改（CORS、proxy、env）、双端 validate、记录浏览器路径
- **禁止**：大规模新功能；若发现契约错误，回写 `contract.md` 并打回对应 lane
- **完成标志**：`openspec validate {slice}-integrate --strict` + 人工浏览器走查

## 并行执行方式

```text
T0  人工或 -api：写入 _lanes/{slice}/contract.md
    ↓
T1  ┌─ Agent A：openspec apply {slice}-api  （worktree 可选）
    └─ Agent B：openspec apply {slice}-web   （worktree 可选）
    ↓
T2  Agent C：openspec apply {slice}-integrate
    ↓
T3  依次 archive：-integrate → -web → -api（或 merge 后一次归档，见 OpenSpec 流程）
```

**Git 建议**：两 lane 并行写代码时使用 **不同 worktree / 分支**（如 `feat/admin-shell-api`、`feat/admin-shell-web`），在 integrate 阶段 merge。

## tasks.md 结构

### `-api`

```markdown
## 前置
- [ ] `_lanes/{slice}/contract.md` 已冻结

## 后端
- [ ] …

## 验证
- [ ] mvn compile
- [ ] curl …
```

### `-web`

```markdown
## 前置
- [ ] `_lanes/{slice}/contract.md` 已冻结
- [ ] （可选）`-api` 后端 tasks 已完成

## 前端（web/）
- [ ] …

## 验证
- [ ] pnpm build
```

### `-integrate`

```markdown
## 前置
- [ ] `-api` 全部 tasks 完成
- [ ] `-web` 全部 tasks 完成

## 联调
- [ ] spring-boot:run + pnpm dev
- [ ] 浏览器路径 …

## 门禁
- [ ] validate 三个 change
```

## 与纵向切片的关系

| 项 | 单 change 纵向切片 | 并行 Lane |
|----|-------------------|-----------|
| 交付标准 | 浏览器可验证 | 相同（经 `-integrate`） |
| OpenSpec 数量 | 1 | 2～3 |
| 并行度 | 低 | 高 |
| 契约 | design.md 内 | `_lanes/.../contract.md` |

**仍禁止**：只有 `-api` 长期无 `-web`；`-integrate` 未通过就 archive `-web`。

## 当前切片对照

| 功能 | API change | Web change | Integrate |
|------|------------|------------|-----------|
| 管理端登录 | —（历史单 change `admin-login-slice`） | — | — |
| 管理端壳层 | `admin-shell-api` | `admin-shell-web` | `admin-shell-integrate` |
| 用户列表 | `admin-user-list-api`（待建） | `admin-user-list-web`（待建） | 待建 |

## 验证命令

```bash
openspec validate admin-shell-api --strict
openspec validate admin-shell-web --strict
openspec validate admin-shell-integrate --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
```

## 参考

- [vertical-slice-workflow.md](vertical-slice-workflow.md)
- [admin-ui-workflow.md](admin-ui-workflow.md)
- `.cursor/rules/parallel-lane-frontend.mdc`
- `.cursor/rules/parallel-lane-backend.mdc`
