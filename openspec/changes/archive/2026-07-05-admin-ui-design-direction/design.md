# 设计：管理端 UI 视觉基调

## Context

**当前状态**

- 技术栈：Vue 3 + Vite + Nuxt UI v4 + Tailwind v4（见 `.cursor/rules/frontend-nuxt-ui.mdc`）
- 壳层：`layouts/admin.vue` 使用 `UDashboardSidebar` + `UNavigationMenu`，品牌区为 Lucide workflow 图标 + 「RelayFlow」
- 主题：`web/src/assets/css/main.css` 仅覆盖 `--font-sans: Public Sans` 与 green 色阶；暗色模式 `--ui-primary` 指向 primary-500
- 模板残留：`web/src/pages/` 下仍有 dashboard-vue 演示页（customers、inbox、settings 等），**不属于 RelayFlow 产品页**，后续应逐步移除或隔离

**约束**

- 主 UI 层必须是 Nuxt UI v4，禁止引入 Element Plus / shadcn-vue 等
- 管理端路由 `/admin/*`；V1 先聚焦管理端，用户端 IM 界面另立 change
- 离线自部署：不依赖外部 CDN 字体（若用 Google Fonts 须本地 fallback 或自托管）
- 目标用户：企业 IT / 管理员；界面应 **专业、可信、信息密度适中**，避免消费级花哨

**产品定位（视觉层面）**

> 自托管企业协作平台的管理控制台 —— 让人感受到「工程严谨 + 产品完成度高」，而非 demo 模板换皮。

---

## Goals / Non-Goals

**Goals:**

1. 提供 **三种可对比的视觉方向**，便于你选定 V1 基调（**阶段 0 — 已完成**）
2. 通过 **`admin-ui-prototype` Mock 全壳层** 在浏览器中确认整体展示效果（**阶段 2 — 见 sibling change**）
3. 从你 **签字确认的原型代码** 中 **反抽** 设计 token、页面模式、组件约定，写入 `docs/dev/` 与 `.cursor/rules/`（**阶段 3 — 本 change tasks 阶段 2–4**）
4. 为 `admin-login-slice` 及后续纵向切片提供 **唯一 UI 工程化真源**（**阶段 4 — 只换 API，不重做展示层**）

> 本 change **不决定后端架构**。design.md 中「方向 B 详细规范」为 **草案**；定稿以原型验收后抽取的 `admin-ui-tokens.md` / `admin-ui-patterns.md` 为准。

**Non-Goals:**

- 完整 Figma 设计稿（V1 以代码 + OpenSpec 文字/结构描述为主；可选后续补 Figma）
- 用户端 IM 聊天 UI（Phase 后续 change）
- 国际化文案体系、无障碍审计（仅遵循 Nuxt UI 默认可访问性）
- 重写 dashboard 模板架构（保留 `UDashboardGroup` 等官方模式）

---

## 三种视觉方向（待你选定）

以下三种均在 Nuxt UI v4 能力内实现，差异主要在 **默认主题、密度、装饰程度**。

### 方向 A：**Slate Command**（深色指挥台）

| 维度 | 说明 |
|------|------|
| 气质 | Linear / Vercel Dashboard 风 —— 偏深色、低饱和、高对比文字 |
| 默认模式 | **Dark 优先**（可切换 Light） |
| 主色 | 冷灰底 + **电蓝/靛蓝** primary（`oklch` 蓝系，非模板绿） |
| 侧边栏 | 近黑 `bg-neutral-950`，1px 细边框分隔，图标线性 Lucide |
| 内容区 | 略浅灰卡片 `bg-neutral-900/50`，圆角 `lg`，几乎无大阴影 |
| 字体 | **Inter** 或 **Geist Sans**（替代 Public Sans） |
| 密度 | 紧凑 —— 表格行高偏小，适合数据型管理页 |
| 适合 | 强调「工程/运维/后端能力」、夜间使用多、偏 tech 审美 |

**登录页意象**：全屏深色渐变背景 + 居中窄卡片（`max-w-sm`），Logo 单色，无插图。

---

### 方向 B：**Clean Enterprise**（清爽企业 SaaS）★ 推荐

| 维度 | 说明 |
|------|------|
| 气质 | Notion Admin / Stripe Dashboard 风 —— 明亮、留白、克制 |
| 默认模式 | **Light 优先**（完整 Dark 支持） |
| 主色 | 保留/微调 **青绿/teal** 系（与 RelayFlow「流/协作」隐喻一致），primary 600 用于按钮 |
| 侧边栏 | 白/浅灰 `bg-elevated`，active 项左侧 2px primary 条 + 浅 primary 背景 |
| 内容区 | 白卡片 + 轻 shadow-sm，页面背景 `bg-muted/30` |
| 字体 | **Inter** + 中文系统栈 `-apple-system, "PingFang SC", "Microsoft YaHei"` |
| 密度 | 标准 —— 行高舒适，适合长时间配置操作 |
| 适合 | 企业管理员白天使用、兼顾「现代」与「稳重」、与现有 green token 迁移成本最低 |

**登录页意象**：左右分栏 —— 左侧品牌区（渐变 + 产品一句话），右侧 `UAuthForm`；移动端单列。

---

### 方向 C：**Soft Glass**（柔和玻璃质感）

| 维度 | 说明 |
|------|------|
| 气质 | 近年 premium SaaS（Arc / Raycast 设置页）—— 半透明、大圆角、柔和阴影 |
| 默认模式 | Light，Dark 下玻璃效果减弱 |
| 主色 | **紫罗兰/蓝紫** gradient accent + 中性灰正文 |
| 侧边栏 | 半透明 blur 背景（`backdrop-blur-xl`），浮动感 |
| 内容区 | 大圆角 `2xl`，多层 soft shadow，卡片边框 `border-white/20` |
| 字体 | **Plus Jakarta Sans** |
| 密度 | 偏宽松 —— 更多 padding，视觉「高级」但信息密度较低 |
| 适合 | 最强「好看」优先；需注意性能与长期维护（玻璃态易过时） |

**登录页意象**：全屏浅色 mesh 渐变背景 + 玻璃卡片居中。

---

### 对比与推荐

|  criteria | A Slate | B Clean ★ | C Glass |
|-----------|---------|-----------|---------|
| 现代感 | ★★★★ | ★★★★ | ★★★★★ |
| 企业可信度 | ★★★★ | ★★★★★ | ★★★ |
| 实现/维护成本 | 中 | **低** | 高 |
| 与现有模板差异 | 大改色 | **小改** | 中改 + 自定义 CSS 多 |
| 后端能力展示气质 | 强（tech） | **强（产品完整）** | 中（偏视觉） |
| 中文 UI 友好 | 好 | **最好** | 好 |

**推荐方向 B（Clean Enterprise）**：在「看起来高级」与「企业控制台可信度」之间平衡最好，且与当前 green 主题、Nuxt UI dashboard 模式兼容，后续列表/表单页扩展成本最低。若你更想突出个人后端/工程审美，可选 A。

---

## 方向 B 详细规范（默认草案；你确认方向后可整段替换为 A 或 C）

> 以下按 **方向 B** 展开，作为 spec 与 tasks 的实现基准。选定 A/C 后仅替换 token 与登录/壳层描述，结构不变。

### 1. 设计 Token

> **定稿真源**：[`docs/dev/admin-ui-tokens.md`](../../docs/dev/admin-ui-tokens.md)（摘自 `admin-ui-prototype` 验收版 @ 工作区未提交）

```css
/* web/src/assets/css/main.css — 与 admin-ui-tokens.md 一致 */
@theme static {
  --font-sans: 'Inter', ui-sans-serif, system-ui, 'PingFang SC', 'Microsoft YaHei', sans-serif;

  /* Primary: teal — RelayFlow 协作/流动 */
  --color-teal-500: oklch(65% 0.14 175);
  --color-teal-600: oklch(55% 0.13 175);

  --radius-sm: 0.375rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;
}

:root {
  --ui-radius: var(--radius-md);
}
```

| Token | Light | Dark |
|-------|-------|------|
| 页面背景 | `bg-muted/40` | `bg-neutral-950` |
| 卡片 | `bg-default` + `ring-1 ring-default` | `bg-neutral-900` + `ring-neutral-800` |
| 主文字 | `text-highlighted` | `text-highlighted` |
| 次要文字 | `text-muted` | `text-muted` |
| 边框 | `border-default` | `border-neutral-800` |

**禁止**：页面级随意 hex 色；业务组件应使用 semantic class（`text-primary`、`bg-elevated` 等）。

### 2. 布局与壳层（`/admin/*`）

```
┌─────────────────────────────────────────────────────────┐
│ [Sidebar 240px]  │  [Navbar: 标题 | 租户名 | 用户菜单]   │
│  Logo            │  ───────────────────────────────────  │
│  ─────────       │  [Page body: padding 6, max-w-none]   │
│  Nav groups      │    UDashboardPanel / UCard / Table    │
│  · 概览          │                                       │
│  · 系统          │                                       │
│  · 基础设施      │                                       │
└─────────────────────────────────────────────────────────┘
```

- **Sidebar**：可折叠、可拖拽宽度（保留现有 `UDashboardSidebar`）；分组导航（系统 / 基础设施 / IM 预留）
- **Navbar**：左侧页面标题 + breadcrumb（深层页面）；右侧 **租户名**、通知占位、`UserMenu`（头像 + 退出）
- **内容区**：统一 `p-4 sm:p-6`；列表页满宽；表单页 `max-w-2xl` 居中

### 3. 页面模式

| 模式 | 路由示例 | 组件模式 |
|------|----------|----------|
| 认证 | `/admin/login` | 全屏 layout（无 sidebar）；`UAuthForm` 或 `UCard`+表单；底部版本号 |
| 概览 | `/admin` | `UDashboardPanel` + 统计卡片行（`HomeStats` 风格，数据后续接 API） |
| 列表 | `/admin/system/user` | Navbar + 筛选栏（`UInput`/`USelect`）+ `UTable` + 分页 |
| 表单 | `/admin/system/user/create` | 分组 `UCard` + `UForm` + 底部 sticky 操作栏 |
| 空状态 | 任意 | `UEmpty` + 主按钮引导 |
| 错误 | 401/403 | 壳层内 `UAlert` 或独立简洁页 |

### 4. 组件约定（Nuxt UI）

| 用途 | 组件 |
|------|------|
| 主按钮 | `UButton` color="primary" |
| 危险操作 | `UButton` color="error" variant="soft" + `UModal` 确认 |
| 表格 | `UTable` + `@tanstack/table-core`（模板已有） |
| 表单 | `UForm` + `UFormField` + Zod |
| 反馈 | `useToast()` |
| 加载 | `USkeleton` / 按钮 `loading` |
| 图标 | Lucide（`i-lucide-*`），线宽一致，避免 emoji |

### 5. 动效与微交互

- 页面切换：无全屏 transition；sidebar 折叠 150ms ease
- 按钮/链接：使用 Nuxt UI 默认 hover/active，**禁止**大面积 scale 动画
- 列表行 hover：`bg-elevated/50`
- 尊重 `prefers-reduced-motion`

### 6. 品牌元素

| 元素 | 规范 |
|------|------|
| Logo | Lucide `workflow` 或自定义 SVG（V1 图标即可） |
| 产品名 | **RelayFlow** — sidebar 与登录页一致 |
| 副标题 | 登录页：「企业协作平台 · 管理控制台」 |
| 租户 | Navbar 展示默认租户名（接 API 后） |

### 7. 与模板演示页的关系

- V1 不对外暴露 customers/inbox/settings 等 demo 路由（删除或移入 `pages/_demo/` 并不注册菜单）
- 管理端菜单仅展示 RelayFlow 真实模块入口

---

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| UI 框架 | 保持 Nuxt UI v4 | 项目硬约束；dashboard 组件齐全 |
| 默认视觉方向 | **B Clean Enterprise** ✅ 已确认 | 平衡现代感、企业可信、实现成本 |
| 字体 | Inter + 中文系统栈 | 可读性、免费、与 Nuxt UI 生态一致 |
| 暗色模式 | **跟随系统** `prefers-color-scheme` ✅ 已确认 | Nuxt UI color mode；须同时打磨 Light/Dark |
| 登录页布局 | **左右分栏** ✅ 已确认 | 左品牌区 + 右 `UAuthForm` |
| 设计预览页 | **启用** `/admin/design-preview` ✅ 已确认 | 开发期组件板，不进生产菜单 |
| 登录页壳层 | 独立 layout，无 sidebar | 安全与视觉焦点 |

---

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 方向选择拖延阻塞 login 切片 | 本 change tasks 首项为「确认方向」；确认后立即 apply |
| 过度定制偏离 Nuxt UI 升级路径 | token 优先改 `@theme`；组件用 `ui` prop 而非全局 CSS 覆盖 |
| 模板 demo 页混淆产品 | tasks 含清理/隔离 demo 路由 |
| 玻璃态/重动效性能差（若选 C） | 默认不推荐 C；若选 C 限制 blur 层数 |
| 字体 CDN 离线部署失败 | 使用 `fontsource` 自托管或 system-ui fallback |

---

## Migration Plan（文档驱动四阶段）

详见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)。

1. **阶段 0** — 确认视觉方向（A/B/C）→ **已完成 B**
2. **阶段 2** — Apply `admin-ui-prototype`：Mock 全壳层 + 浏览器走查 + **人工签字**
3. **阶段 3** — Apply 本 change tasks 阶段 2–4：从定稿原型抽取规则 → `docs/dev/admin-ui-*.md` + `.cursor/rules/admin-ui-patterns.mdc`
4. **阶段 4** — Apply `admin-login-slice` 等：保留页面结构，替换 Mock 为真 API

回滚：阶段 2 还原 `web/` 原型文件；阶段 3 删除新增 docs/rules 即可，无数据迁移。

---

## 已确认决策（2026-07-05）

| 项 | 决定 |
|----|------|
| 视觉方向 | **B · Clean Enterprise**（teal 主色、清爽企业 SaaS） |
| 默认主题 | **跟随系统** `prefers-color-scheme` |
| 登录页 | **左右分栏** — 左品牌/slogan，右表单 |
| 设计预览 | **启用** `/admin/design-preview` |
| Logo | V1 暂用 Lucide `workflow` + RelayFlow 文案（无自定义 SVG） |

**阶段 0 已完成** → **`admin-ui-prototype` 已实现** → **阶段 3 规则沉淀已完成**（见 `docs/dev/admin-ui-*.md`）。

---

## 验收记录（阶段 2 人工签字）

| 项 | 内容 |
|----|------|
| 原型 change | `admin-ui-prototype` |
| 签字人 | 用户（会话确认继续） |
| 日期 | 2026-07-05 |
| 定稿 commit | 工作区未提交（基于 `bebe9bc` 之上 admin-ui-prototype 实现） |
| 结论 | ☑ UI 定调通过 |

**阶段 3 规则沉淀**（2026-07-05）：`docs/dev/admin-ui-tokens.md`、`admin-ui-patterns.md`、`.cursor/rules/admin-ui-patterns.mdc` 已从上述原型代码归纳。
