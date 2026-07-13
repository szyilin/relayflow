# Specs 归档同步清单（notify-inbox-v2）

归档 `openspec archive notify-inbox-v2` 前核对：

## 修改 main spec

- [ ] `openspec/specs/infra/spec.md`
  - ADDED：类型目录、dedupe_key、read-all、按 type 筛选、payload deep link
  - MODIFIED：`Notify WebSocket domain` 从 optional V1 升级为必做
- [ ] `openspec/specs/task/spec.md`
  - ADDED：TASK_DUE 生产方、lazy list 补偿

## 配置与文档

- [ ] `docs/dev/api-integration-board.md` — `notify-inbox-v2` → done
- [ ] `openspec/lanes/notify-inbox-v2/contract.md` — 状态改为已对接

## 下游 change

- [ ] `bpm-v1` §3.4 / integrate 可联调 `APPROVAL_PENDING`
- [ ] `workspace-tasks-assign` 可复用 `TASK_ASSIGNED` 类型
