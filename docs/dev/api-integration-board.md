# API 对接看板

> **前端 AI / 开发者第一入口**：查各切片 **UI 与 API 进度**。  
> **默认顺序（前端优先）**：`-web`（UI+Mock+contract 草案）→ `-api` → `-integrate`。见 [`frontend-first-workflow.md`](frontend-first-workflow.md)。

行为真源（归档后）：[`openspec/specs/`](../../openspec/specs/) · 切片契约（永久）：[`openspec/lanes/`](../../openspec/lanes/)

## 状态说明

| API 状态 | 含义 |
|----------|------|
| `planned` | 契约草案或后端未实现 |
| `ready` | 后端已实现、curl 通过 |
| `archived` | `-api` change 已归档 |

| Web 状态 | 含义 |
|----------|------|
| `pending` | UI 未开始 |
| `in_progress` | `-web` 进行中（**可先 Mock 演示**） |
| `ui_ready` | UI + store + contract 草案完成，待 `-api` |
| `done` | integrate 通过，Mock 已移除 |

## 当前切片

| 切片 | API 状态 | Web 状态 | 端点 / 页面 | 契约 | Active change |
|------|----------|----------|-------------|------|---------------|
| **统一登录** | archived | done | `POST …/auth/login` · `/app/login` | — | `unified-login-slice` |
| admin-shell | archived | in_progress | `GET …/tenant/default` · 壳层租户名 | [contract](../../openspec/lanes/admin-shell/contract.md) | `admin-shell-web` · `admin-shell-integrate` |
| admin-user-list | planned | pending | `GET …/user/page` · `/admin/system/user` | 待 `-web` 起草 | 待建 |

## 前端 lane 怎么用（**第一步**）

1. 读本表 → 找 **`web: pending`** 或用户指定的切片  
2. 实现 **页面 + store + Mock**；起草 **`openspec/lanes/{slice}/contract.md`**  
3. 打开 **`openspec/changes/{slice}-web/tasks.md`**  
4. `pnpm build`；看板 **`web → ui_ready`**  
5. 后端 `-api` 完成后再 `-integrate`；看板 **`web → done`**

## 后端 lane 怎么用（**第二步**）

1. 读 **`-web` 已起草的** `contract.md`  
2. 实现 + 验收（`mvn compile`、curl）  
3. 更新本表：`api → archived`  
4. **`openspec archive {slice}-api`**

## 维护规则

- 新切片：**`-web` 先加看板行**，contract 由前端 lane 起草  
- `-integrate` 通过后 `web → done`，并移除该切片 store 内 Mock  
- 禁止删除已归档切片行  

## 参考

- [frontend-first-workflow.md](frontend-first-workflow.md)
- [parallel-lane-workflow.md](parallel-lane-workflow.md)
- [vertical-slice-workflow.md](vertical-slice-workflow.md)
