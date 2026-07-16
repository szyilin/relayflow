# Specs 归档同步清单（bpm-v1）

归档 `openspec archive bpm-v1` 前核对：

## 新建 main spec

- [ ] `openspec/specs/bpm/spec.md` — 从 `changes/bpm-v1/specs/bpm/spec.md` delta 合并

## 修改 main spec

- [ ] `openspec/specs/im/spec.md` — 确认 `approval-bot` / `ImBotApi` 产方约定与实现一致（触达已不走 infra notify）
- [ ] **勿**再向 `openspec/specs/infra` 同步 `APPROVAL_PENDING` 类型目录（已 SUPERSEDED）

## 配置与文档

- [ ] `openspec/config.yaml` — `relayflow-module-bpm` 改为 V1.1 已启用
- [ ] `README.md` — 模块表 bpm 行状态
- [ ] `docs/dev/api-integration-board.md` — `bpm-approval` → done

## 依赖

- [ ] `im-bot-notify-foundation` 已落地 `ImBotApi` + `approval-bot` 种子
- [ ] `openspec/lanes/bpm-approval/contract.md` 与实现一致
