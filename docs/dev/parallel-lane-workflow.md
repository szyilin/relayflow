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

## 三层信息（解耦归档）

| 层 | 路径 | 职责 |
|----|------|------|
| **行为真源** | `openspec/specs/` | 后端 `-api` **archive 时**合并 delta spec；系统保证什么 API |
| **切片契约** | `openspec/lanes/{slice}/contract.md` | **永久保留**；路径、字段、鉴权、curl；不随 `-api` archive 删除 |
| **对接看板** | [`api-integration-board.md`](api-integration-board.md) | **前端第一入口**；API/Web 就绪状态、待建 `web/` 文件 |

契约变更流程：**仅** `-api` 或人工修改 `contract.md` → 更新看板 → 通知 `-web` lane。

## 三 Lane 职责

### `-api`（后端）

- **编辑范围**：`relayflow-module-*`、`relayflow-framework`、`relayflow-server`、Flyway、`deploy/`（若需要）
- **禁止**：改 `web/`（CORS/Security 白名单等联调必要项除外，须在 tasks 写明）
- **验证**：`./mvnw -pl relayflow-server -am compile`；涉及接口时 curl 或 `.relayflow/api-tests/{slice}/`
- **归档**：验收通过后 **立即** archive **`-api`**（**不必等 `-web`**）；archive 前 **必须** 更新 [`api-integration-board.md`](api-integration-board.md) 的 `api` 状态
- **Cursor 规则**：`.cursor/rules/parallel-lane-backend.mdc`

### `-web`（前端）

- **编辑范围**：`web/`
- **禁止**：改 Java 模块（`vite.config` proxy、`VITE_*` 除外）
- **验证**：`cd web && pnpm build`
- **必读**：[`api-integration-board.md`](api-integration-board.md) → `openspec/lanes/{slice}/contract.md` → `{slice}-web/tasks.md`
- **Cursor 规则**：`.cursor/rules/parallel-lane-frontend.mdc`

### `-integrate`（集成）

- **前置**：看板该切片 **`api: archived/ready`**；`-web` **代码 tasks 均已完成**
- **职责**：本地联调、补跨端小改（CORS、proxy、env）、双端 validate、记录浏览器路径
- **禁止**：大规模新功能；若发现契约错误，回写 `contract.md` 并打回对应 lane
- **完成标志**：`openspec validate {slice}-integrate --strict` + 人工浏览器走查；看板 **`web` → `done`**

## 并行执行与归档顺序

```text
T0  -api：写入 openspec/lanes/{slice}/contract.md + 看板加一行
    ↓
T1  ┌─ Agent A：{slice}-api
    └─ Agent B：{slice}-web（读看板 + contract；api 未 ready 时可 mock）
    ↓
T2  -api 验收 → 更新看板 api=archived → archive {slice}-api  → 后端 AI 释放
    ↓
T3  -web 继续 / 完成 → {slice}-integrate 联调
    ↓
T4  archive：-integrate → -web（-api 已在 T2 归档）
```

**Git 建议**：两 lane 并行写代码时使用 **不同 worktree / 分支**（如 `feat/admin-shell-api`、`feat/admin-shell-web`），在 integrate 阶段 merge。

## tasks.md 结构

### `-api`

```markdown
## 前置
- [ ] openspec/lanes/{slice}/contract.md 已冻结

## 后端
- [ ] …

## 验证
- [ ] mvn compile
- [ ] curl …

## 归档前
- [ ] 更新 api-integration-board.md（api → archived）
```

### `-web`

```markdown
## 前置
- [ ] api-integration-board.md 该切片 api 为 archived/ready
- [ ] openspec/lanes/{slice}/contract.md 已读

## 前端（web/）
- [ ] …

## 验证
- [ ] pnpm build
```

### `-integrate`

```markdown
## 前置
- [ ] 看板 api: archived/ready
- [ ] -web 全部 tasks 完成

## 联调
- [ ] spring-boot:run + pnpm dev
- [ ] 浏览器路径 …

## 门禁
- [ ] validate -integrate；看板 web → done
```

## 与纵向切片的关系

| 项 | 单 change 纵向切片 | 并行 Lane |
|----|-------------------|-----------|
| 交付标准 | 浏览器可验证 | 相同（经 `-integrate`） |
| OpenSpec 数量 | 1 | 2～3 |
| 并行度 | 低 | 高 |
| 契约 | design.md 内 | `openspec/lanes/.../contract.md` |
| 前端如何知 API 就绪 | — | **`api-integration-board.md`** |

**仍禁止**：只有 `-api` 长期无 `-web`（看板长期 `web: pending`）；`-integrate` 未通过就 archive `-web`。

## 当前切片对照

| 功能 | API | Web | Integrate | 看板 |
|------|-----|-----|-----------|------|
| 管理端登录 | —（历史 `admin-login-slice`） | done | — | archived / done |
| 管理端壳层 | **archived** | `admin-shell-web` | `admin-shell-integrate` | [看板](api-integration-board.md) |
| 用户列表 | 待建 | 待建 | 待建 | planned |

## 验证命令

```bash
openspec validate admin-shell-web --strict
openspec validate admin-shell-integrate --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
bash .relayflow/api-tests/admin-shell/run.sh   # 本地 API 验收（不提交 Git）
```

## 参考

- [api-integration-board.md](api-integration-board.md)
- [vertical-slice-workflow.md](vertical-slice-workflow.md)
- [admin-ui-workflow.md](admin-ui-workflow.md)
- `.cursor/rules/parallel-lane-frontend.mdc`
- `.cursor/rules/parallel-lane-backend.mdc`
