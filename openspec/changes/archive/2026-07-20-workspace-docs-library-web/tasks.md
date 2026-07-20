# Tasks：workspace-docs-library-web（前端 lane）

> **范围**：仅 `web/` + lane contract；对应母 change [`workspace-docs-library-v1`](../workspace-docs-library-v1/tasks.md) §2。  
> **前置**：`docs-schema-v1` 完成。  
> **后续**：`workspace-docs-library-api` → `workspace-docs-library-integrate`。

---

## 2. workspace-docs-library-web（前端 lane · 第一步）

- [x] 2.1 起草 `openspec/lanes/workspace-docs-library/contract.md`（树 / 文档 body+bodyFormat+version / 最近 / MD 导出 / 错误码 / curl）
- [x] 2.2 `api/app/docs.ts` + docs store（临时数据仅 store；禁止常驻 `mocks/`）
- [x] 2.3 `/app/docs`：侧栏「我的文档库」树 + 「最近」；云盘 / 知识库 / 与我共享 / 星标占位或 disabled
- [x] 2.4 接入 **TipTap**（已拍板可加 npm）：懒加载；V1 最小块集见 design D2；标题 + 防抖保存形状与 contract 一致；深链 `?docId=`
- [x] 2.5 新建 / 重命名 / 树内移动 / 删除的 UI（对临时数据）
- [x] 2.6 MD 导出入口（调用导出 API 形状；`-web` 阶段可用客户端临时序列化撑演示，integrate 必须走 API）
- [x] 2.7 `cd web && pnpm build && pnpm typecheck`
- [x] 2.8 浏览器：`/app/docs` 可演示树、编辑、导出入口

**验证**：`pnpm build` + `pnpm typecheck` 已通过（2026-07-20）。浏览器路径：登录 → `/app/docs` → 新建 / 编辑 / 导出 Markdown（当前为 store 临时数据，刷新会丢）。

**完成后**：看板 web → `ui_ready`；可开 `-api`。
