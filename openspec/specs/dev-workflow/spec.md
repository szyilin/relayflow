# dev-workflow Specification

## Purpose
TBD - created by archiving change frontend-first-workflow. Update Purpose after archive.
## Requirements
### Requirement: 前端优先纵向切片顺序

带 UI 的业务 OpenSpec 切片 MUST 默认按 **`{slice}-web` → `{slice}-api` → `{slice}-integrate`** 顺序实施；`-web` lane MUST 起草 `openspec/lanes/{slice}/contract.md`。

#### Scenario: 新切片从 UI 开始

- **WHEN** 创建带 UI 的新业务切片
- **THEN** 第一个 change MUST 为 `{slice}-web` 或 tasks 中前端段位于后端段之前
- **AND** UI 须可在浏览器中以 Mock 数据演示

#### Scenario: contract 由前端起草

- **WHEN** `-web` lane 完成 UI 与 store
- **THEN** MUST 在 `openspec/lanes/{slice}/contract.md` 记录 API 路径与字段草案
- **AND** `-api` lane 按该 contract 实现后端

