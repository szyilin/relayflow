# 纵向切片工作流（Full-Stack Vertical Slice）

RelayFlow **业务功能**默认按 **纵向切片** 推进，且采用 **[前端优先](frontend-first-workflow.md)**：每个切片先做出 **可点击 UI（Mock）**，再实现后端 API，最后联调去 Mock。

> 脚手架类 change 仍按原流程；**自 `tenant-ready-foundation` 完成之后**，用户可感知的功能一律走本工作流。

## 原则

| 项 | 规则 |
|----|------|
| 切片粒度 | 一个 **用户可感知** 的能力（如「用户列表」） |
| **实施顺序** | **`-web`（UI+Mock+contract 草案）→ `-api` → `-integrate`** |
| 交付物 | 浏览器可验证的完整功能（integrate 后无 Mock） |
| 禁止 | 连续多会话只堆后端、长期无 UI（`[平台]` 除外） |
| 禁止 | 单次会话实现「整个 V1」 |
| 验证 | 有 UI：**`-web` 阶段** 须 `pnpm build` + 浏览器路径；integrate 须联调 |

## 切片标准结构（任务顺序 MUST 前端在前）

```markdown
## 切片：<名称>

### 前端（web/）— 先做
- [ ] 页面 + 路由 + Store（Mock 回退在 store 内）
- [ ] 起草 openspec/lanes/{slice}/contract.md
- [ ] pnpm build

### 后端 — 后做
- [ ] 按 contract 实现 API
- [ ] curl 验证

### 联调
- [ ] store 去 Mock；spring-boot:run + pnpm dev
- [ ] 浏览器路径说明
```

## 认证与入口

| 项 | 约定 |
|----|------|
| 唯一登录页 | `/app/login`（`POST /admin-api/system/auth/login`） |
| 登录后 | `/app/messages` 工作台 |
| 管理后台 | `/admin/*`；从工作台进入；同一 JWT；RBAC 控权限 |

## 目录与 API 对齐

| 后端 | 前端 |
|------|------|
| `POST /admin-api/system/auth/login` | `api/admin/auth.ts` + `/app/login` |
| `GET /admin-api/system/tenant/default` | 壳层租户名 |
| `GET /admin-api/system/user/page` | `/admin/system/user` |

## 当前进度与推荐顺序

| 顺序 | 切片 | 说明 |
|------|------|------|
| ✅ | 统一登录 | `unified-login-slice` |
| 🔄 | 管理端壳层租户名 | `admin-shell-web` / integrate |
| ⏭ | **用户列表** | **先** 完善 UI → **再** `-api` 分页 |
| 可选 | 首次管理员引导 | 无用户向导 |
| `[平台]` | 租户 Redis/MinIO/WS | 无 UI |

## OpenSpec 编写约定

1. 带 UI 切片：`-web`、`-api`、`-integrate`；contract 由 **`-web` 起草**
2. 看板：[`api-integration-board.md`](api-integration-board.md)
3. 并行 Lane 细节：[`parallel-lane-workflow.md`](parallel-lane-workflow.md)

## 参考

- [frontend-first-workflow.md](frontend-first-workflow.md) — **默认实施顺序（必读）**
- [parallel-lane-workflow.md](parallel-lane-workflow.md)
- [admin-ui-workflow.md](admin-ui-workflow.md) — 管理端 UI 定调（更早阶段）
