# 纵向切片工作流（Full-Stack Vertical Slice）

RelayFlow **业务功能**默认按 **纵向切片** 推进：每个切片在同一 OpenSpec change（或同一 change 内连续 tasks 组）内，**后端 API 与前端页面同批交付**，使功能可在浏览器中可见、可点击、可调试。

> 脚手架类 change（Maven 父工程、空 Starter、Compose、web 模板初始化）仍可按纯后端/纯前端拆分；**自 `tenant-ready-foundation` 完成之后**，用户可感知的功能一律走本工作流。

## 原则

| 项 | 规则 |
|----|------|
| 切片粒度 | 一个 **用户可感知** 的能力（如「管理端登录」「用户列表」） |
| 交付物 | 后端 API + `web/` 对接 + 路由/页面（若该能力有 UI） |
| 禁止 | 连续多个会话只堆后端 API、长期无对应前端（除非 tasks 明确标注「纯平台/无 UI」） |
| 禁止 | 单次会话实现「整个 V1 / 全部模块」 |
| OpenSpec | 每个切片独立 change，或在一个 change 的 `tasks.md` 中按 **切片** 分组 |
| 验证 | 切片完成前必须 **界面或 curl 二选一** 有证据；有 UI 的切片 **必须** `pnpm build` + 浏览器路径说明 |

## 切片标准结构

每个 **带 UI** 的切片，tasks 建议按以下四段书写（可合并 checkbox，但四段都要有）：

```markdown
## 切片：<名称>（例：管理端登录）

### 后端（relayflow-module-* / framework）
- [ ] Flyway / DO（若需要）
- [ ] Service + Controller（路径对齐 docs/dev/api.md）
- [ ] curl 或集成测试验证

### 前端（web/）
- [ ] `web/src/api/admin/*.ts`（或 `app/`）封装
- [ ] 页面 `web/src/pages/admin/...`（路由 `/admin/...`）
- [ ] Pinia / 路由守卫（若涉及登录态）
- [ ] `pnpm build` 通过

### 联调
- [ ] 本地：`spring-boot:run` + `pnpm dev`，按 README 步骤可走通
- [ ] 记录验证路径（如 `/admin/login` → 登录后进 `/admin`）

### 文档（若本切片改变协作方式则跳过）
- [ ] 无需改 spec 时仅勾选 tasks；归档前 `openspec validate --strict`
```

**纯平台切片**（无 UI，如 Redis key 前缀、MyBatis 插件）：tasks 可仅含后端 + 测试，但须在 task 标题标注 **`[平台]`**，且不得与用户可见切片混在同一组 tasks 里超过 5 条。

## 单次会话建议范围

- **1 个纵向切片**，或
- **1 个切片的后端段 + 前端段**（同一功能），或
- **≤10 条** tasks checkbox（与 `implementation-workflow.mdc` 一致）

## 目录与 API 对齐

| 后端 | 前端 |
|------|------|
| `POST /admin-api/system/auth/login` | `web/src/api/admin/auth.ts` + `pages/admin/login.vue` |
| `GET /admin-api/system/tenant/default` | `api/admin/tenant.ts` + 壳层展示默认企业名 |
| `GET /admin-api/system/user/page` | `pages/admin/system/user/index.vue` |

管理端路由 **必须** `/admin` 前缀；API 层统一处理 `{ code, msg, data }`（`code === 0` 成功）。详见 [api.md](api.md)、[code-style.md](code-style.md)。

## 验证命令（按切片类型）

```bash
# 每个切片至少
openspec validate <change-name> --strict
./mvnw -pl relayflow-server -am compile

# 含 web/ 的切片 additionally
cd web && pnpm install && pnpm build

# 联调（人工）
./mvnw -pl relayflow-server -am spring-boot:run
cd web && pnpm dev
# 浏览器打开文档中写的路径
```

## 当前进度与推荐切片顺序

脚手架已完成（Maven、framework、server、compose、web 模板、system 表结构、租户 Starter、登录 API 等）。

### 管理端 UI 定调（先于接 API 的纵向切片）

管理端首次落地须按 **[admin-ui-workflow.md](admin-ui-workflow.md)** 执行（文档驱动，**不决定后端**）：

| 阶段 | OpenSpec change | 产出 |
|------|-----------------|------|
| 1 定方向 | `admin-ui-design-direction` 阶段 0 | B · Clean Enterprise 等决策 ✅ |
| 2 可点击原型 | `admin-ui-prototype` | Mock 全壳层 + **你签字 UI 定调** |
| 3 规则沉淀 | `admin-ui-design-direction` 阶段 2–4 | `admin-ui-tokens.md`、`admin-ui-patterns.md`、Cursor 规则 |
| 4 接 API | `admin-login-slice` → … | 只换数据层，不重做 UI |

> 登录 API 可先于阶段 2 存在；**不得**因 API 已有而跳过 UI 定调。

### 纵向切片（UI 定调完成后）

| 顺序 | 切片 | 后端状态 | 前端待做 |
|------|------|----------|----------|
| 1 | **管理端登录** | ✅ 登录 API | Mock → 真 JWT（保留原型 UI） |
| 2 | **管理端首页壳层** | ✅ 默认租户 API | `admin-shell-web`（并行 `admin-shell-api`） |
| 3 | **首次管理员引导** `[可选]` | ✅ 用户创建 API | 无用户时的向导页 |
| 4 | **租户平台能力** `[平台]` | §5 Redis/MinIO/WS 隔离 | 无 UI；可单独 change |
| 5 | **用户管理列表** | 待建分页 API | `/admin/system/user` 接 API |

已存在的 **仅后端** 工作（如 `tenant-ready-foundation` §4）应在后续切片中 **补前端**，而不是再开新的纯后端 change 重复造登录 API。

## OpenSpec 编写约定

新建 change 时：

1. `proposal.md` 写明 **是否含 web/** 与 **用户可见路径**（如 `/admin/login`）。
2. **带 UI 的新切片**：拆为 `{slice}-api`、`{slice}-web`、`{slice}-integrate`，契约写 `_lanes/{slice}/contract.md`（见 [parallel-lane-workflow.md](parallel-lane-workflow.md)）。
3. 单 change 模式：`tasks.md` 使用 **切片标准结构**；不要按「先全部 Controller、再全部 Vue」分组。
4. `design.md` 增加 **「联调与演示」** 小节：本地端口、示例账号、浏览器路径。
5. `openspec/config.yaml` 的 `tasks` 规则已引用本工作流；AI 实现前必读本文档。

## 与旧流程的关系

- **保留**：小步 OpenSpec change、Flyway 真源、codegen CLI、禁止跨 change 实现。
- **调整**：「禁止前后端一起搭完」→ **禁止一次搭完整产品**；**允许且鼓励**同一切片内前后端一起交付。
- **并行 Lane**（推荐）：新切片拆为 `{slice}-api` + `{slice}-web` + `{slice}-integrate`，见 [parallel-lane-workflow.md](parallel-lane-workflow.md)。
- **脚手架 change**（`scaffold-*`）不适用纵向切片，按原 `implementation-workflow.mdc` 执行。

## 参考

- [.cursor/rules/implementation-workflow.mdc](../../.cursor/rules/implementation-workflow.mdc)
- [.cursor/rules/vertical-slice-workflow.mdc](../../.cursor/rules/vertical-slice-workflow.mdc)
- [AGENTS.md](../../AGENTS.md)
