# Tasks：workspace-docs-share-star-v1（母 change · 执行路线图）

> **用法**：本文件是云文档「与我共享 / 星标 / 对象级分享」的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（通常 ≤10 条）。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）；Schema 建议并入 `-api`。  
> **前置**：请先确认 `design.md` Open Questions Q1–Q5；未拍板前不要开 `-web` 写死权限档位。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / `specs/docs` / `specs/im` / 本 `tasks.md` 齐备
- [x] 0.2 `openspec validate workspace-docs-share-star-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md`：登记 `workspace-docs-share-star`（planned）
- [x] 0.4 `docs/dev/database.md` 表前缀说明增补 `doc_share`、`doc_star`（若尚未列出）
- [x] 0.5 产品拍板写入 design：Q1 权限档位、Q2 FILE 范围、Q3 Bot 同交付、Q4 leave、Q5 Recent 足迹+墓碑、Q6 owner 墓碑、Q7 墓碑 TTL
- [x] 0.6 （若接受 D7）`database.md` / proposal 已含 `doc_user_visit`；子 change tasks 含 Recent API `accessState`

**验证**：strict validate 通过；看板已登记；Q1–Q5 有明确结论。

**完成后**：可开子 change；**不**在本母 change 内直接堆全部业务代码。

---

## 1. workspace-docs-share-star-web（前端 lane · 第一步）

**依赖**：§0 完成（含 Q1–Q5 拍板）。

- [x] 1.1 起草 `openspec/lanes/workspace-docs-share-star/contract.md`（share CRUD、shared-with-me、star、错误码、curl、Bot 深链字段说明）
- [x] 1.2 扩展 `api/app/docs.ts` + docs store（分享/星标临时数据仅 store）
- [x] 1.3 `/app/docs`：启用「与我共享」「星标」；「最近」多列（标题/位置/所有者/创建时间/最近访问/`accessState`）+ 行菜单（从最近移除；owner 可删）
- [x] 1.4 所有者分享对话框：选成员、VIEW/EDIT（若 Q1 仅 VIEW 则去掉 EDIT）、协作者列表、撤销
- [x] 1.5 各面板菜单按 design D10：文档库补分享/星标；与我共享含离开；星标含取消星标；正文顶栏「…」对齐
- [x] 1.6 `cd web && pnpm build && pnpm typecheck`
- [x] 1.7 浏览器：最近列与移除/删除分流、分享列表、星标、墓碑提示（Mock）

**验证**：`pnpm build` + `typecheck` + 浏览器路径。  
**完成后**：看板 web → `ui_ready`；可开 `-api`。

---

## 2. workspace-docs-share-star-api（后端 lane）

**依赖**：contract 就绪。

- [x] 2.1 Flyway：`doc_share`、`doc_star`、`doc_user_visit`；种子 `docs-bot`（`type=system`）
- [x] 2.2 codegen → diff 合入 DO/Mapper；`DocAccessService` 集中鉴权
- [x] 2.3 Share API：授予 / 改权限 / 撤销 / 协作者列表；`system-api` 校验 ACTIVE 成员；仅 `RICH_DOC`
- [x] 2.4 Shared-with-me 列表 + grantee leave；文档 GET/PUT/export 走 ACL；打开时 upsert visit
- [x] 2.5 Recent API 改为 visit + `accessState`；Star API：加/取消/列表（过滤无权限）
- [x] 2.6 分享成功后 best-effort `ImBotApi.send(docs-bot, SINGLE)`；`docs-biz` 引入 `im-api`
- [x] 2.7 curl 或 `.relayflow/api-tests/workspace-docs-share-star/` + `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`；开 `-integrate`。

---

## 3. workspace-docs-share-star-integrate（联调）

- [x] 3.1 store 去临时数据；只走真实 API
- [x] 3.2 E2E：A 分享给 B → B 与我共享可开 → 进最近 → Bot 可见 → 星标 → leave/撤销后最近为无权限墓碑、正文不可读 → A 删除后 B 最近为已删除墓碑 → EDIT 冲突可复现（若启用 EDIT）
- [x] 3.3 `openspec validate workspace-docs-share-star-v1 --strict`
- [x] 3.4 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build && pnpm typecheck`
- [x] 3.5 看板 `workspace-docs-share-star` → **done**

---

## 4. 母 change 收尾

- [x] 4.1 子 change 全部完成后 archive 本母 change，并同步主规格 `openspec/specs/docs`、`openspec/specs/im`
- [x] 4.2 看板 / AGENTS「下一优先」按需更新（如 Drive FILE 分享或 Wiki）

---

## 明确不在本路线图编码

| 项 | 去向 |
|----|------|
| 创建副本 / 快捷方式 / 置顶 / 离线 / 关注 | 后置（design D10 已标 ❌） |
| 链接分享 / 外链 | 后续切片 |
| 部门/群组授权、文件夹继承 ACL | Drive 协作或独立 ACL 切片 |
| Drive `FILE` 分享 | 确认 Q2 后置后另开 |
| 实时 CRDT 协作 | 更后 |
| 评论、快捷方式 | 更后 |
| 管理端文档治理 | 不在工作台本切片 |
| Wiki 空间成员 | `workspace-docs-wiki-v1` |
