# 提案：web/ 前端脚手架（scaffold-web-nuxt-ui）

## Why

V1 前端规格已定（Vue 3 + Vite + Nuxt UI v4），需从官方模板初始化 `web/`，禁止 AI 从零手写完整 Vite 配置。

## What Changes

- 基于 [dashboard-vue 模板](https://github.com/nuxt-ui-templates/dashboard-vue) 初始化 `web/`
- 按 [Nuxt UI Vue 安装指南](https://ui.nuxt.com/docs/getting-started/installation/vue) 集成
- 最小占位页；**不**实现登录、管理端业务

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无）

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | 新建整个前端工程 |

**与后端 scaffold 可并行**，无 Maven 依赖。
