# Tasks：workspace-docs-library-v1（母 change · 执行路线图）

> **用法**：本文件是云文档「我的文档库」的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（通常 ≤10 条）。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）；`[平台]` 可先行。  
> **拍板**：三大容器粗规划；只做文档库 + `RICH_DOC`；**真源 = TipTap JSON**；**V1 仅 MD 导出**；**无 `doc_embed` 表**；无分享；云盘/知识库另开母 change。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / `specs/docs/spec.md` / 本 `tasks.md` 齐备
- [x] 0.2 `openspec validate workspace-docs-library-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md`：登记 `workspace-docs-library`（planned）与建议切片；注明云盘 / 知识库后续母 change
- [x] 0.4 `docs/dev/database.md` 表前缀表增补 `doc_`
- [x] 0.5 拍板写入文档：方案 B 真源、TipTap、仅 MD 导出、`doc_embed` 延后（2026-07-20）

**验证**：strict validate 通过；看板与 database 文档已登记。

**完成后**：可开子 change；**不**在本母 change 内直接堆全部业务代码。

---

## 1. [平台] docs-schema-v1

**目标**：`doc_*` 表 + Maven 模块 + server 可加载。  
**范围**：Java / Flyway；无 `web/` 业务页；**禁止**创建 `doc_embed`。

- [x] 1.1 根 `pom.xml` + `relayflow-module-docs`（api + biz）脚手架（复制 `task` / `calendar` 模块模式）
- [x] 1.2 Flyway：`doc_object`、`doc_library_node`（字段见 design D2）
- [x] 1.3 `./scripts/codegen.sh --module docs --tables doc_object,doc_library_node` → diff 合入 `src/`
- [x] 1.4 `relayflow-server/pom.xml` 引入 `relayflow-module-docs-biz`；模块可启动
- [x] 1.5 `./mvnw -pl relayflow-server -am compile`

**验证**：compile + 迁移成功（`V0.1.0.34__docs_schema.sql`）。

**完成后**：可开 `workspace-docs-library-web`。

---

## 2. workspace-docs-library-web（前端 lane · 第一步）

- [x] 2.1 起草 `openspec/lanes/workspace-docs-library/contract.md`（树 / 文档 body+bodyFormat+version / 最近 / MD 导出 / 错误码 / curl）
- [x] 2.2 `api/app/docs.ts` + docs store（临时数据仅 store；禁止常驻 `mocks/`）
- [x] 2.3 `/app/docs`：侧栏「我的文档库」树 + 「最近」；云盘 / 知识库 / 与我共享 / 星标占位或 disabled
- [x] 2.4 接入 **TipTap**（已拍板可加 npm）：懒加载；V1 最小块集见 design D6；标题 + 防抖保存形状与 contract 一致
- [x] 2.5 新建 / 重命名 / 树内移动 / 删除的 UI（对临时数据）
- [x] 2.6 MD 导出入口（调用导出 API 形状；`-web` 阶段可用客户端临时序列化撑演示，integrate 必须走 API）
- [x] 2.7 `cd web && pnpm build && pnpm typecheck`
- [x] 2.8 浏览器：`/app/docs` 可演示树、编辑、导出入口

**验证**：`pnpm build` + `pnpm typecheck` + 浏览器路径。

**完成后**：看板 web → `ui_ready`；可开 `-api`。

---

## 3. workspace-docs-library-api（后端 lane）

**依赖**：`docs-schema-v1` 完成；`workspace-docs-library-web` contract 就绪。

- [x] 3.1 Library tree GET + node create/update/delete（仅所有者；防环；级联软删 object）
- [x] 3.2 Document GET + body PUT（校验/保存 `body_format`；`contentVersion` 乐观锁）
- [x] 3.3 Open / recent：更新 `last_opened_at`；`GET /recent`
- [x] 3.4 `GET …/export?format=md`：TipTap JSON → Markdown；`docx`/`pdf` 返回 `DOC_EXPORT_FORMAT_UNSUPPORTED`
- [x] 3.5 Security：`/app-api/docs/**` JWT + 有效成员；无管理面 permission
- [x] 3.6 curl 或 `.relayflow/api-tests/workspace-docs-library/` + `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`；开 `-integrate`。

---

## 4. workspace-docs-library-integrate（联调）

- [x] 4.1 store 去临时数据；MD 导出走真实 API（去掉仅前端假导出）
- [x] 4.2 E2E：新建 → 编辑保存 → 刷新仍在 → 树移动 → 删除后树与最近均不可见 → 版本冲突可复现 → 导出 `.md` 可读
- [x] 4.3 `openspec validate workspace-docs-library-v1 --strict`
- [x] 4.4 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build && pnpm typecheck`
- [x] 4.5 看板 `workspace-docs-library` → **done**

**验证**：联调路径可写进子 change 备注。

---

## 5. 母 change 收尾

- [x] 5.1 归档本母 change（或子 change 全部 archive 后同步 `openspec/specs/docs`）— **2026-07-20 归档**
- [x] 5.2 AGENTS.md / api-integration-board「下一优先」更新：建议 `workspace-docs-drive-v1`（**届时再写详细 design/tasks**）

---

## 明确不在本路线图编码

| 项 | 去向 |
|----|------|
| 云盘文件夹 / 上传 | `workspace-docs-drive-v1` |
| 知识库空间 / 成员 | `workspace-docs-wiki-v1` |
| `doc_embed` / 流程图等重型块 | 首个 embed 切片（见 design D4.1） |
| DOCX / PDF 导出 | Office 导出切片 |
| 分享 / 与我共享 / 星标 | 后续独立切片 |
| 独立 SHEET / SLIDES object type | 更后 |
| IM 文档卡片 / 搜索 doc 分组 | 文档库 done 后再开 |
