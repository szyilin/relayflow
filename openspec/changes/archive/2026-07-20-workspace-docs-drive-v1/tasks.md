# Tasks：workspace-docs-drive-v1（母 change · 执行路线图）

> **用法**：云盘「我的文件夹」总路线图。每次会话只做一个子 change 内一组 task（通常 ≤10）。  
> **顺序**：默认 **前端优先**；`[平台]` 可先行。  
> **拍板默认**：仅我的文件夹；复用 app-api infra 上传下载；Library↔Drive 移动要做但可拆子切片；共享文件夹 / Wiki / 云盘内新建 RICH_DOC 不做。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / `specs/docs/spec.md` / 本 `tasks.md` 齐备
- [x] 0.2 `openspec validate workspace-docs-drive-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md`：登记 `workspace-docs-drive`（planned）与建议切片
- [x] 0.4 （可选）`docs/dev/database.md` 注明 `doc_drive_*` 归属 docs 域

**完成后**：可开子 change；本母 change 不直接堆全部业务代码。

---

## 1. [平台] docs-drive-schema-v1

- [x] 1.1 Flyway：`doc_drive_folder`、`doc_drive_item`；`doc_object` 支持 `FILE` + `storage_file_id`（约束/索引见 design）
- [x] 1.2 codegen 合入 DO/Mapper；`./mvnw -pl relayflow-server -am compile`

**验证**：migrate + compile。→ 见子 change `docs-drive-schema-v1`（`V0.1.0.35`）。

---

## 2. workspace-docs-drive-web

- [x] 2.1 起草 `openspec/lanes/workspace-docs-drive/contract.md`
- [x] 2.2 `/app/docs` 启用云盘面板：面包屑、文件夹列表、新建文件夹、上传入口（临时 store）
- [x] 2.3 FILE 下载按钮形状对齐既有 infra download
- [x] 2.4 `pnpm build` + `pnpm typecheck`；浏览器可演示（假数据）

**完成后**：看板 web → `ui_ready`。→ 见子 change `workspace-docs-drive-web`。

---

## 3. workspace-docs-drive-api

- [x] 3.1 文件夹 CRUD + 列表 items（仅所有者；非空删拒绝）
- [x] 3.2 `POST /drive/files` 绑定 `FileApi` + 创建 FILE object + drive_item
- [x] 3.3 item 重命名/移动/删除（软删策略按 design）
- [x] 3.4 Security + curl/smoke；compile

**完成后**：看板 api → `ready`。→ 见子 change `workspace-docs-drive-api`。

---

## 4. workspace-docs-drive-integrate

- [x] 4.1 store 去临时数据；上传走真实 infra + docs 登记
- [x] 4.2 E2E：建文件夹 → 上传 → 刷新仍在 → 下载 → 删除
- [x] 4.3 validate + build/typecheck；看板 drive 核心 → **done**（若移动未做则看板注明 move pending）

**完成后**：见子 change `workspace-docs-drive-integrate`（本会话含 §5）。

---

## 5. 跨容器移动（可拆 `-web`/`-api`/`-integrate` 或单 change 三段）

- [x] 5.1 contract 增量：`POST /placements/move`
- [x] 5.2 UI：从文档库「移动到云盘」/ 从云盘「移回文档库」（仅 RICH_DOC→Library；FILE→Library 拒绝）
- [x] 5.3 API 实现 + 联调：移后仅一处可见；`objectId` 不变
- [x] 5.4 看板移动能力勾完

**完成后**：与 integrate 同切片交付。

---

## 6. 母 change 收尾

- [x] 6.1 archive（用户 commit 后）+ 同步 `openspec/specs/docs`
- [x] 6.2 下一优先：知识库 `workspace-docs-wiki-v1` **暂缓**；`AGENTS.md` 已改为待定

---

## 明确不在本路线图

| 项 | 去向 |
|----|------|
| 共享文件夹 | 后续母 change |
| 知识库 | `workspace-docs-wiki-v1` |
| 云盘内新建 RICH_DOC | 后置 |
| `doc_embed` | 重型块切片 |
