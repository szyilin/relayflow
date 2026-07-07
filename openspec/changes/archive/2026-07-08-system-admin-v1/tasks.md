# 任务：system-admin-v1

> **性质**：规划型史诗 — **本 change 不写业务代码**。勾选表示子 change 文档/实施完成。

## 1. 史诗文档

- [x] 1.1 编写 `proposal.md`、`design.md`
- [x] 1.2 编写 `specs/system/spec.md` 增量
- [x] 1.3 `openspec validate system-admin-v1 --strict`

## 2. 子 change 立项（按顺序 propose，不写代码）

- [x] 2.1 `system-rbac-kernel-api` + `system-rbac-kernel-web` 已 propose（见 `openspec/changes/system-rbac-kernel-*`）
- [x] 2.2 `openspec new change admin-dept-slice` 并完成 proposal/design/tasks/spec（实现已合并，见 commit `37e2ed4`）
- [x] 2.3 `openspec new change admin-role-slice` 并完成 proposal/design/tasks/spec（实现已合并，见 commit `6ecc3d5`）
- [x] 2.4 `openspec new change admin-user-mutate-slice` 并完成 proposal/design/tasks/spec（已归档）

## 3. 史诗归档（全部子 change 实施后）

- [x] 3.1 更新 `docs/dev/api-integration-board.md` 增加 system-admin 切片行
- [x] 3.2 更新 `AGENTS.md`「下一优先」指向 `tenant-platform-slice`
- [x] 3.3 `openspec archive system-admin-v1` 合并 spec 至主 specs

## 不在本 change

- Java / `web/` 实现（→ 各子 change）
- `tenant-ready-foundation` §5/§7（→ `tenant-platform-slice`）
