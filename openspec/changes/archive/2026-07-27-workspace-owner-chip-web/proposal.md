## Why

列表中的「所有者 / 负责人」仍是纯文本（甚至单独「我」），无法点开通讯录名片。需要可复用芯片组件，作为云文档 IA 改版与任务列表的前置能力。

## What Changes

- 新增 `WorkspaceOwnerChip`：头像 + 显示名；点击打开 `WorkspaceBusinessCard`
- 接入云文档最近 / 与我共享 / 星标列表所有者列
- 接入任务列表负责人展示（多负责人：最多 3 chip + 溢出）
- 更新 `workspace-ui-patterns.md` 约定
- **无**后端改动（本切片纯 web；头像优先既有 avatar URL / contacts / auth）

## Capabilities

### New Capabilities

- （无独立新 capability 文件名；行为对齐母 change `workspace-owner-chip`）

### Modified Capabilities

- `workspace-owner-chip`：交付可复用芯片与列表接线（相对母 change 规格落地到 web）

## Impact

| 层 | 路径 |
|----|------|
| 前端 | `web/src/components/workspace/WorkspaceOwnerChip.vue`；docs 列表；`/app/tasks` |
| 文档 | `docs/dev/workspace-ui-patterns.md`；看板 |
| 回滚 | 卸组件，列表恢复纯文本 |
