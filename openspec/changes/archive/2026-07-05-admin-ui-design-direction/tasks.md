# 任务：admin-ui-design-direction

> **性质**：管理端 UI **定方向 + 规则沉淀** —— **不写后端**；**原型代码在 `admin-ui-prototype`**，本 change 负责决策与从定稿原型 **反抽** 工程化规则。  
> 全流程见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)。

## 阶段 0：定方向（人工 — ✅ 已完成）

- [x] 0.1 阅读 `design.md` 三种方向 —— **已选 B · Clean Enterprise**
- [x] 0.2 已确认：主题跟随系统、登录左右分栏、启用 design-preview（见 design.md「已确认决策」）

## 阶段 1：前置 — 原型验收（人工 — ✅ 已完成）

> 实现工作全部在 **`admin-ui-prototype`**；本 change **在此项通过前不写规则文档终稿**。

- [x] 1.1 `admin-ui-prototype` 全部 tasks 完成（含 `pnpm build`、浏览器走查）
- [x] 1.2 **人工签字**：在 `design.md`「验收记录」或 PR 评论中确认「UI 定调通过」（记录日期与 commit）

## 阶段 2：从定稿原型抽取规则（docs/ + rules）

> 规则 **必须** 对照已签字原型代码归纳，不得与实现脱节。

- [x] 2.1 新增 `docs/dev/admin-ui-tokens.md`：自 `main.css` 与 design-preview 归纳 primary、语义色、圆角、字体、Light/Dark
- [x] 2.2 新增 `docs/dev/admin-ui-patterns.md`：登录、壳层、概览、列表、表单、空状态各一页模式（对照代表页 Vue 文件）
- [x] 2.3 新增 `.cursor/rules/admin-ui-patterns.mdc`：Nuxt UI 组件映射、禁止项、`components/admin/` 与 store 替换约定
- [x] 2.4 在 `docs/dev/code-style.md` 增加一节链接上述文档（≤10 行）

## 阶段 3：规格与 design 对齐

- [x] 3.1 更新 `design.md`「方向 B 详细规范」：与 2.x 文档一致；标注规则来源 commit
- [x] 3.2 更新 `specs/admin-ui-design/spec.md`：补充「规则来自原型验收版」场景
- [x] 3.3 `openspec validate admin-ui-design-direction --strict`

## 阶段 4：归档准备（规则沉淀完成后）

- [x] 4.1 确认 `admin-login-slice` proposal 已引用 `admin-ui-workflow.md` 与 patterns 文档
- [x] 4.2 运行 `openspec archive admin-ui-design-direction` 合并至 `openspec/specs/admin-ui-design/`（可与 login 切片并行，但 login **正式 UI 交付** 须等 4.2 或等效 spec 就绪） — **关闭（路线重置，不再作为当前 backlog）**

## 不在本 change

- Mock 壳层、占位页、Pinia Mock store（→ `admin-ui-prototype`）
- `api/admin/*`、JWT 联调（→ `admin-login-slice`）
- 后端 API / Flyway

## 后续衔接

| Change | 前置条件 |
|--------|----------|
| `admin-ui-prototype` | 阶段 0 ✅；**当前优先实现** |
| `admin-login-slice` | 阶段 1 签字 + 阶段 2–3 规则文档就绪；**只换数据层，不重做 UI** |
| `admin-shell-slice` | 同上 |
