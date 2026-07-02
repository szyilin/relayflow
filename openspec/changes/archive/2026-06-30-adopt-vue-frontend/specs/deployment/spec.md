## 新增需求

### 需求：Vue 前端技术栈

`web/` 下前端子工程应使用 Vue 3、TypeScript 与 Vite。

#### 场景：前端工程结构

- 给定 开发者初始化 `web/` 目录
- 当 创建前端工程
- 那么 使用 Vue 3 Composition API
- 并且 启用 TypeScript 严格模式
- 并且 使用 Vite 作为构建工具

### 需求：前端隔离

全部前端源码必须位于 `web/`，且不得使用 React 或其他非 Vue 的 SPA 框架。

#### 场景：仓库中不含 React

- 给定 RelayFlow 仓库
- 当 查看 `web/package.json` 中的前端依赖
- 那么 `vue` 为核心依赖之一
- 并且 不含 `react` 依赖
