# Specs 归档同步清单（bpm-v1）

归档 `openspec archive bpm-v1` 前核对：

## 新建 main spec

- [ ] `openspec/specs/bpm/spec.md` — 从 `changes/bpm-v1/specs/bpm/spec.md` delta 合并

## 修改 main spec

- [ ] `openspec/specs/infra/spec.md` — `APPROVAL_PENDING` 类型目录（若 notify-inbox-v2 已归档则可能已存在，避免重复 ADDED）

## 配置与文档

- [ ] `openspec/config.yaml` — `relayflow-module-bpm` 改为 V1.1 已启用
- [ ] `README.md` — 模块表 bpm 行状态
- [ ] `docs/dev/api-integration-board.md` — `bpm-approval` → done

## 依赖

- [ ] `notify-inbox-v2` 已归档或 `APPROVAL_PENDING` 类型已在 `infra-api` 实现
- [ ] `openspec/lanes/bpm-approval/contract.md` 与实现一致
