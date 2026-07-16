# 前端优先工作流（单人 AI · 无原型图）

RelayFlow 当前为 **单人 + AI 编码、无 Figma/产品原型图**。在此约束下，**用户可感知功能** 默认采用 **「先前端界面、后后端 API」** 的纵向切片顺序。

> 与 [vertical-slice-workflow.md](vertical-slice-workflow.md) 的关系：交付标准不变（浏览器可验证）；**实施顺序** 从「后端先行」改为 **「前端 UI 先行」**。  
> 管理端 UI 定调（`admin-ui-prototype`）仍是 **更早期的 UI 例外**，本工作流从 **业务切片** 起适用。

## 为什么前端优先

| 约束 | 后端先行的问题 | 前端优先的做法 |
|------|----------------|----------------|
| 无原型图 | 接口形状由 AI 猜，页面再迁就 API | **页面即规格**：列表列、表单字段、空状态先定稿 |
| 单人 AI | 并行 `-api`/`-web` 收益低，上下文切换多 | 一个会话先把 UI 做完整（Mock），下一会话再补 Java |
| 产品形态 | 用户看不到东西，难以纠偏 | `pnpm dev` 即可点选、改布局，再冻结契约给后端 |

## 切片三 Lane（顺序）

命名不变，**执行顺序** 调整如下：

```text
{slice}-web        ① 前端：页面 + Store +（可选）临时 Mock + 起草 contract.md
{slice}-api        ② 后端：按 contract 实现 API
{slice}-integrate  ③ 联调：去掉临时 Mock、双端 validate、看板 done
```

```text
T0  -web：UI 可演示 + openspec/lanes/{slice}/contract.md 草案
T1  -web：pnpm build + pnpm typecheck；看板 web → in_progress / ui_ready
T2  -api：读 contract → mvn compile + curl
T3  -api：看板 api → archived；archive -api
T4  -integrate：store 只走真实 API；spring-boot:run + pnpm dev
T5  archive -integrate、-web；看板 web → done
```

**禁止**（与纵向切片相同，仍适用）：

- 只有 `-api` 长期无 `-web`（`[平台]` 无 UI 除外）
- `-web` 未 UI 可演示就 archive
- 页面直接 `import mocks/`（若切片仍用临时 Mock，仅允许 store 内引用，且 integrate 必须删除）

## `-web` Lane 职责（第一步）

1. 读 [`admin-ui-patterns.md`](admin-ui-patterns.md) / [`workspace-ui-patterns.md`](workspace-ui-patterns.md)
2. 实现 **页面 + 路由 + Pinia Store**；数据走 `api/*`。API 未就绪时，可在 **store 内** 用临时本地数据撑 UI；**不要**再引入已废弃的 `isApiUnavailable` 全局回退约定，**不要**长期保留 `web/src/mocks/`
3. **起草** [`openspec/lanes/{slice}/contract.md`](../openspec/lanes/README.md)：路径、请求/响应字段、鉴权、curl 示例（后端按此实现）
4. 更新 [`api-integration-board.md`](api-integration-board.md)：`web: in_progress`，`api: planned`
5. `cd web && pnpm build && pnpm typecheck`；写明浏览器路径

## `-api` Lane 职责（第二步）

1. 读 **已定稿的** `contract.md`（若与 UI 冲突，先改 contract 并通知 integrate）
2. 实现 Java / Flyway；**不得** 擅自改字段名让前端迁就
3. curl 或 `.relayflow/api-tests/{slice}/` 验收
4. 看板 `api → archived`；archive `-api`

## `-integrate` Lane 职责（第三步）

1. store **移除** 该切片的临时 Mock / 本地假数据
2. 本地联调；401/403 行为与 [api.md](api.md)、`web/src/api/request.ts` 一致
3. validate + 看板 `web → done`

## 单 change 模式（小切片）

仍可用 **一个 change** 的 `tasks.md`，但任务顺序 MUST 为：

```markdown
### 前端（web/）— 先做
### 后端 — 后做
### 联调
```

## 认证与入口（产品约定）

详见 [`product-permission-model.md`](product-permission-model.md)。

- **唯一登录页**：`/app/login`（已接 `POST /admin-api/system/auth/login`）
- **未登录**：`/app/**`（除 login）、`/admin/**` 必须 redirect login；禁止未登录展示 app/admin 壳层
- 登录成功 → **员工工作台** `/app/messages`（有效成员即可，**不**用 RBAC 区分产品面）
- **管理后台** `/admin/*`：同一账号、同一 JWT；须 **管理身份**（角色含后台 permission）；进入后 RBAC 控菜单/API
- **禁止** 再新增 `/admin/login` 等产品级第二登录入口

## 验证命令

```bash
# -web 阶段
cd web && pnpm build && pnpm typecheck

# -api 阶段
./mvnw -pl relayflow-server -am compile

# -integrate
openspec validate <slice>-integrate --strict
```

## 参考

- [vertical-slice-workflow.md](vertical-slice-workflow.md)
- [parallel-lane-workflow.md](parallel-lane-workflow.md)
- [api-integration-board.md](api-integration-board.md)
- [admin-ui-workflow.md](admin-ui-workflow.md) — 管理端定调仍先于业务切片
