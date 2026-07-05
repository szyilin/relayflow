# 并行 Lane 工作流（OpenSpec 前后端拆分 · 前端优先）

用户可见功能在 **契约草案** 后，拆成 **`{slice}-web` → `{slice}-api` → `{slice}-integrate`** 顺序推进（单人 AI 默认顺序；多 Agent 时 `-api` 仍须在 contract 冻结后）。

> 交付标准不变：**integrate 完成后浏览器可验证、Mock 已移除**。  
> 总纲：[frontend-first-workflow.md](frontend-first-workflow.md)

## 何时拆分

| 场景 | 做法 |
|------|------|
| 带 UI 的业务切片 | **`{slice}-web` → `{slice}-api` → `{slice}-integrate`** |
| 纯平台、无 UI | 单个 change `[平台]` |
| 脚手架 | 单个 change |
| 管理端 UI 定调 | `admin-ui-prototype`（仅 web） |

## 命名约定

```text
{slice}-web        ① 前端：UI + Store + Mock + contract 草案
{slice}-api        ② 后端：按 contract 实现
{slice}-integrate  ③ 联调、去 Mock、门禁
```

## 三层信息

| 层 | 路径 | 职责 |
|----|------|------|
| 行为真源 | `openspec/specs/` | `-api` archive 时合并 |
| 切片契约 | `openspec/lanes/{slice}/contract.md` | **`-web` 起草**；`-api` 实现依据 |
| 对接看板 | [api-integration-board.md](api-integration-board.md) | UI/API 进度 |

## 三 Lane 职责与顺序

### `-web`（第一步）

- **编辑**：`web/`、contract 草案、看板
- **产出**：UI 可演示（Mock）、`contract.md` 草案、`pnpm build` 通过
- **禁止**：改 Java（proxy/env 除外）
- **Cursor**：`.cursor/rules/parallel-lane-frontend.mdc`

### `-api`（第二步）

- **前置**：`contract.md` 已读；UI 字段与页面一致
- **编辑**：Java、Flyway
- **验证**：`mvn compile`、curl
- **归档**：看板 `api → archived`；**不必等 integrate**
- **Cursor**：`.cursor/rules/parallel-lane-backend.mdc`

### `-integrate`（第三步）

- **前置**：`-web` ui_ready；`-api` archived
- **职责**：去 Mock、联调、validate、看板 `web → done`

## 执行时间线

```text
T0  -web：UI（Mock）+ contract 草案 + 看板
T1  -web：pnpm build → ui_ready
T2  -api：实现 + curl → api archived
T3  -integrate：去 Mock + 浏览器走查 → done
```

## tasks.md 结构

### `-web`（先做）

```markdown
## 前端（web/）
- [ ] 页面 + store + Mock
- [ ] 起草 openspec/lanes/{slice}/contract.md
- [ ] pnpm build
```

### `-api`（后做）

```markdown
## 前置
- [ ] contract.md 已读

## 后端
- [ ] …

## 归档前
- [ ] 更新 api-integration-board.md
```

## 当前切片

| 功能 | 顺序 | 看板 |
|------|------|------|
| 统一登录 | done | archived / done |
| 管理端壳层 | api 已完成；web integrate 待做 | [看板](api-integration-board.md) |
| 用户列表 | **web 先行** | planned |

## 参考

- [frontend-first-workflow.md](frontend-first-workflow.md)
- [vertical-slice-workflow.md](vertical-slice-workflow.md)
