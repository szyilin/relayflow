# 任务：scaffold-web-nuxt-ui

> 模板拉取较重，**分 3 节**执行，每节单独验证。

## 1. 模板初始化

- [x] 1.1 从 dashboard-vue 官方模板生成 `web/` 目录（degit 或 shallow clone）
- [x] 1.2 清理模板自带 git 历史/无关文件
- [x] 1.3 验证：目录存在 `web/package.json`、`web/vite.config.ts`

## 2. 依赖与构建

- [x] 2.1 `pnpm install`
- [x] 2.2 确认 `@nuxt/ui` 在 dependencies
- [x] 2.3 验证：`cd web && pnpm build`

## 3. 路由占位

- [x] 3.1 确认或添加 `/admin` 占位路由（AdminLayout 壳层）
- [x] 3.2 验证：`pnpm dev` 可访问（手动或 smoke）

## 4. 门禁

- [x] 4.1 `openspec validate scaffold-web-nuxt-ui --strict`
