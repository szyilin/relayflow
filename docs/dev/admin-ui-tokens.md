# 管理端 UI 设计 Token

> **来源**：摘自 `admin-ui-prototype` 验收版（B · Clean Enterprise），对照 `web/src/assets/css/main.css` 与 `pages/admin/design-preview.vue`。  
> 真源代码变更时须同步更新本文档。

## 视觉方向

**B · Clean Enterprise** — 明亮留白、teal 主色、企业 SaaS 可信度。

| 项 | 值 |
|----|-----|
| 主色 | **teal**（`vite.config.ts` → `ui.colors.primary: 'teal'`） |
| 中性色 | **zinc** |
| 默认主题 | **跟随系统** `prefers-color-scheme`（`useColorMode({ initialValue: 'auto' })`） |
| 字体 | Inter + 中文系统栈 |

## CSS Token（`web/src/assets/css/main.css`）

```css
@theme static {
  --font-sans: 'Inter', ui-sans-serif, system-ui, 'PingFang SC', 'Microsoft YaHei', sans-serif;

  /* Primary: teal 色阶 */
  --color-teal-50 … --color-teal-900

  --radius-sm: 0.375rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;
}

:root {
  --ui-radius: var(--radius-md);
}
```

## 语义用法

| 用途 | Light / Dark 通用 class | 禁止 |
|------|-------------------------|------|
| 页面背景 | `bg-muted/30`、`bg-default` | 页面级 hex |
| 卡片 | `UCard`（默认 ring + shadow） | 自定义大阴影 |
| 主文字 | `text-highlighted` | — |
| 次要文字 | `text-muted` | — |
| 边框 | `border-default` | — |
| 主操作 | `color="primary"` | 手写 primary hex |
| 侧边栏 | `bg-elevated/25` | — |
| Active 导航 | 左 2px primary 条 + primary 12% 背景 | 整块高亮色 |

## 圆角

| Token | 值 | 用途 |
|-------|-----|------|
| `--radius-sm` | 0.375rem | 小控件 |
| `--radius-md` | 0.5rem | 默认（`--ui-radius`） |
| `--radius-lg` | 0.75rem | 卡片、统计图标容器 |

## 品牌

| 元素 | 规范 |
|------|------|
| Logo 图标 | Lucide `i-lucide-workflow` |
| 产品名 | **RelayFlow** |
| 登录副标题 | 「企业协作平台 · 管理控制台」 |
| 登录左栏 | `from-primary-600 via-primary-600 to-primary-800` 渐变 |

## 组件板

开发期验收见 `/admin/design-preview`（色板、Button、Alert、Form、Table、Empty、Skeleton）。

## 参考

- [admin-ui-patterns.md](admin-ui-patterns.md) — 页面模式
- [admin-ui-workflow.md](admin-ui-workflow.md) — UI 定调工作流
- `openspec/changes/admin-ui-design-direction/design.md` — 方向决策
