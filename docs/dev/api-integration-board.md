# API 对接看板

> **前端 AI / 开发者第一入口**：查哪些后端 API **已就绪可对接**、哪些 **前端尚未接线**。  
> **后端 AI**：`-api` lane 验收通过、**archive 前必须更新本表**。

行为真源（归档后）：[`openspec/specs/`](../../openspec/specs/) · 切片契约（永久）：[`openspec/lanes/`](../../openspec/lanes/)

## 状态说明

| API 状态 | 含义 |
|----------|------|
| `planned` | 契约未冻结或后端未开始 |
| `ready` | 后端已实现、curl/本地 api-tests 通过，**可开始 `-web`** |
| `archived` | `-api` change 已归档，spec 已合并 |

| Web 状态 | 含义 |
|----------|------|
| `pending` | 未开始 |
| `in_progress` | `-web` change 进行中 |
| `done` | `-web` tasks 完成且 integrate 浏览器通过 |

## 当前切片

| 切片 | API 状态 | Web 状态 | 可对接端点 | 前端文件（✅ 已接 / ❌ 待建） | 契约 | Active change |
|------|----------|----------|-----------|------------------------------|------|---------------|
| admin-login | archived | done | `POST /admin-api/system/auth/login` | `api/admin/auth.ts` ✅ · `stores/auth.ts` ✅ | —（历史单 change） | — |
| **admin-shell** | **archived** | **in_progress** | `GET /admin-api/system/tenant/default` | `api/admin/tenant.ts` ❌ · `stores/tenant.ts` ❌（仍用 mock） · `layouts/admin.vue` mount 拉取 ❌ | [contract](../../openspec/lanes/admin-shell/contract.md) | `admin-shell-web` · `admin-shell-integrate` |
| admin-user-list | planned | pending | `GET /admin-api/system/user/page`（待建） | 整页待建 | 待起草 | 待建 |

## 前端 lane 怎么用

1. 读本表 → 找 **`api: archived/ready`** 且 **`web: pending/in_progress`** 的行  
2. 读对应 **`openspec/lanes/{slice}/contract.md`**  
3. 打开 **`openspec/changes/{slice}-web/tasks.md`** 只完成前端 tasks  
4. 完成后由 **`{slice}-integrate`** 联调；看板 **`web` → `done`**

## 后端 lane 怎么用

1. 冻结 **`openspec/lanes/{slice}/contract.md`**  
2. 实现 + 验收（`mvn compile`、curl 或 `.relayflow/api-tests/{slice}/`）  
3. **更新本表**：`api` → `archived`，填写端点与 spec 路径  
4. **`openspec archive {slice}-api`**（**不必等 `-web`**）  
5. 继续下一个 `{slice}-api` change

## 维护规则

- 新增带 UI 切片：先在 **`openspec/lanes/{slice}/contract.md`** 起草，再在本表加一行  
- `-api` archive **前**更新 API 状态；`-integrate` 通过后更新 Web 状态为 `done`  
- 禁止删除已归档切片行（改状态，保留历史）  
- 本地验收脚本：`.relayflow/api-tests/`（不提交 Git）

## 参考

- [parallel-lane-workflow.md](parallel-lane-workflow.md)
- [vertical-slice-workflow.md](vertical-slice-workflow.md)
