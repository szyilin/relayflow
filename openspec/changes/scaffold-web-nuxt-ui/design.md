# 设计：scaffold-web-nuxt-ui

## 生成方式

1. 使用 `degit` 或 `git clone --depth 1` 拉取 dashboard-vue 模板到 `web/`
2. 删除模板 `.git`（若有）
3. `pnpm install`
4. 确认 `@nuxt/ui`、Tailwind v4、Vue Router、Pinia 就绪
5. 管理端路由占位 `/admin`（与 `docs/dev/code-style.md` 对齐）

## 禁止

- Element Plus 作为主 UI
- 在 `web/` 外放前端代码
- 手写完整 `vite.config` 而不参考模板

## 验证

```bash
cd web && pnpm install && pnpm build
```
