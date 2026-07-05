# 管理端 UI 工作流（文档驱动 · UI 定调）

RelayFlow 管理端前端采用 **「先看见、先确认、再提炼规则、最后接 API」** 的工作流。  
本流程 **只决定前端展示与工程化约定**，不约束后端架构、API 形态或数据库设计。

> 业务纵向切片默认见 [vertical-slice-workflow.md](vertical-slice-workflow.md)。管理端 UI 在接真 API 前，按本文档作为 **UI 先行例外** 执行。

## 目的

| 要做 | 不做 |
|------|------|
| 在浏览器里确认管理端 **整体观感**（壳层、代表页、交互） | 用前端原型反推或改写后端模块划分 |
| 从 **你确认过的原型代码** 中抽取 token、页面模式、组件约定 | 在原型阶段调用 `/admin-api/*` |
| 将抽取结果写入 **文档 + Cursor 规则**，供后续切片与 AI 遵守 | 在每个纵向切片里重新发明 UI |

**完成标志**：你说「这版 UI 可以定调了」→ 规则沉淀归档 → 再进入 `admin-login-slice` 等接 API 的切片。

## 四阶段总览

```text
阶段 1 · 定方向          admin-ui-design-direction（阶段 0）
    ↓ 大方向已选（B · Clean Enterprise）
阶段 2 · 可点击原型      admin-ui-prototype（Mock 全壳层，零后端）
    ↓ 浏览器走查 + 你签字确认
阶段 3 · 规则沉淀        admin-ui-design-direction（阶段 1–4）
    ↓ 从定稿代码反抽 → docs/dev/ + .cursor/rules/
阶段 4 · 纵向切片接 API  admin-login-slice → admin-shell-slice → …
    ↓ 只换数据层，不重做展示层
```

## 阶段 1：定方向（OpenSpec：`admin-ui-design-direction` · 阶段 0）

**产出**：视觉大方向决策（A/B/C 选一），写入 change `design.md`「已确认决策」。

**已完成（2026-07-05）**：

- 方向 **B · Clean Enterprise**（teal 主色、清爽企业 SaaS）
- 主题 **跟随系统** `prefers-color-scheme`
- 登录页 **左右分栏**
- 启用 **`/admin/design-preview`** 组件板（开发期）

**本阶段不写完整规则手册**——详细 token、页面模式以阶段 2 代码为准，阶段 3 再从代码归纳。

## 阶段 2：可点击原型（OpenSpec：`admin-ui-prototype`）

**性质**：Mock 全壳层，**零后端**；`pnpm dev` 即可走查。

**验收对象（你看的是这些，不是 API）**：

- 壳层：sidebar 分组、navbar、租户区、用户菜单
- 代表页：登录、概览、列表、表单、空状态、design-preview
- 交互：登录 → 各页 → 退出 → 未登录拦截
- 观感：颜色、密度、圆角、中文可读性、Light/Dark

**实现顺序**（见 `openspec/changes/admin-ui-prototype/tasks.md`）：

1. 壳层骨架（`layouts/admin.vue`、`layouts/auth.vue`、`useAdminNav`）
2. 设计 token 落地（`main.css`，B 方向）
3. 组件板（`/admin/design-preview`）
4. 代表页占位 + Mock store / 守卫
5. 清理 dashboard 模板 demo
6. `pnpm build` + 浏览器走查
7. **人工签字**：你在 change 或 issue 中确认「UI 定调通过」

**禁止**：

- `web/src/api/admin/*`、Vite proxy、`spring-boot:run`
- 以 design.md 想象代替肉眼验收

**阶段 2 完成 = 你确认定调**，不是仅 build 通过。

## 阶段 3：规则沉淀（OpenSpec：`admin-ui-design-direction` · 阶段 1–4）

**前置**：阶段 2 人工签字通过。

**做法**：从 **已确认的原型代码** 归纳（不是重写一套理想规范），沉淀为 AI 与开发者可执行的约定。

| 提炼项 | 沉淀位置 |
|--------|----------|
| 设计 token（色、字、圆角、间距） | `docs/dev/admin-ui-tokens.md` |
| 页面模式（列表/表单/概览/空状态） | `docs/dev/admin-ui-patterns.md` |
| 组件映射、目录约定、Mock→API 替换 | `.cursor/rules/admin-ui-patterns.mdc` |
| 行为规格 | 归档 `openspec/specs/admin-ui-design/spec.md` |

**规则须标注来源**：例如「摘自 `admin-ui-prototype` 验收版 @ `<commit>`」。

**本阶段仍无后端 API**；完成后 `openspec validate admin-ui-design-direction --strict`，再归档至主 specs。

## 阶段 4：纵向切片接真 API

**前置**：阶段 3 规则已写入 `docs/dev/` 与 `.cursor/rules/`。

| 顺序 | Change | 工作 | 保留 |
|------|--------|------|------|
| 1 | `admin-login-slice` | 真 JWT、`api/admin/auth.ts` | 登录页 layout / 样式 |
| 2 | `admin-shell-api` + `admin-shell-web` | 真租户名、退出 | navbar / 壳层 |
| 3 | `admin-user-list-slice` | 真分页 API | 列表页骨架 |
| … | 各业务切片 | 按 `admin-ui-patterns.md` 加新页 | 页面模式不重设计 |

每个切片：**只换 store / api 层**，template 与布局遵循阶段 3 规则；须 `pnpm build` + 浏览器路径 + 后端联调（见 vertical-slice-workflow）。

## OpenSpec change 对照

| Change | 对应阶段 | 是否写 Java | 是否调 API |
|--------|----------|-------------|------------|
| `admin-ui-design-direction` | 1（决策）+ 3（规则沉淀） | 否 | 否 |
| `admin-ui-prototype` | 2（Mock 原型） | 否 | 否 |
| `admin-login-slice` 及后续 | 4（纵向切片） | 按需 | 是 |

## 验证命令

```bash
# 阶段 2
cd web && pnpm build
pnpm dev   # 浏览器走查，见 admin-ui-prototype/design.md

# 阶段 3
openspec validate admin-ui-design-direction --strict

# 阶段 4（每个切片）
openspec validate <change-name> --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
```

## 与纵向切片的关系

- **默认**：用户可见功能 = 后端 API + `web/` 同批（vertical-slice-workflow）。
- **例外**：管理端 V1 首次定调 = 阶段 2 Mock 原型 → 阶段 3 规则 → 再恢复纵向切片。
- **后端**：登录 API 等可先于阶段 2 存在；前端在阶段 4 才对接，**不因 API 已有而跳过 UI 定调**。

## 参考

- [vertical-slice-workflow.md](vertical-slice-workflow.md)
- [code-style.md](code-style.md) — 管理端路由 `/admin` 前缀
- `.cursor/rules/frontend-nuxt-ui.mdc` — Nuxt UI 技术栈
- `.cursor/rules/admin-ui-patterns.mdc` — 阶段 3 产出（定调前可能不存在）
- `openspec/changes/admin-ui-design-direction/`
- `openspec/changes/admin-ui-prototype/`
