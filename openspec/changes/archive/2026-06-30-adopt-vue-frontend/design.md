# 设计：前端技术栈（Vue）

> **UI 更新（2026-06）**：UI 组件库由 Element Plus 调整为 **Nuxt UI v4**（独立 Vue + Vite，非 Nuxt 框架）。详见 `.cursor/rules/frontend-nuxt-ui.mdc` 与 `openspec/specs/deployment/spec.md`。

## 选型

| 类别 | 选型 | 说明 |
|------|------|------|
| 框架 | Vue 3 | Composition API |
| 语言 | TypeScript | strict 模式 |
| 构建 | Vite | 开发与生产构建 |
| 路由 | Vue Router | 4.x |
| 状态 | Pinia | 全局状态 |
| UI 组件库 | Nuxt UI v4 | `@nuxt/ui` Vite 插件；底层 Reka UI + Tailwind CSS v4 |
| HTTP | Axios | REST 调用 |
| WebSocket | 原生 WebSocket API 或封装 composable | 对接 `/infra/ws` |
| 包管理 | pnpm | 与常见前端工程一致 |

## 目录约定

```text
web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/
    ├── stores/
    ├── api/
    ├── views/
    ├── components/
    └── composables/      # 含 useWebSocket 等
```

## 与后端协作

- 管理端页面调用 `/admin-api/...`
- 用户端 IM 页面调用 `/app-api/...` 与 WebSocket `/infra/ws`
- 开发环境通过 Vite proxy 转发 API（配置于 `vite.config.ts`）

## 明确不使用

- React / Next.js
- Element Plus、Semi Design、daisyUI、shadcn-vue 作为主 UI 层
- 前端代码不得放在仓库根目录，仅存在于 `web/`

## 参考

- [Nuxt UI 文档](https://ui.nuxt.com/docs/getting-started/installation/vue)
- [dashboard-vue 官方模板](https://github.com/nuxt-ui-templates/dashboard-vue)
