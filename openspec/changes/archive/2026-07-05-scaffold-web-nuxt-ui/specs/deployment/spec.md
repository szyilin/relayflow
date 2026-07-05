## ADDED Requirements

### Requirement: web 前端工程脚手架

`web/` SHALL 基于 Vue 3 + TypeScript + Vite 初始化，并以 Nuxt UI v4 作为主 UI 库。

#### Scenario: 前端可构建

- **WHEN** 开发者在 `web/` 执行 `pnpm install && pnpm build`
- **THEN** 构建成功产出静态资源
- **AND** `package.json` 包含 `@nuxt/ui` 依赖
