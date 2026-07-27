## Why

按母 change `workspace-docs-ia-redesign-v1` 交付飞书式云文档信息架构：主页 Tab、文档库右栏树形列表、云盘滑入浏览器、主页上传进云盘。

## What Changes

- `/app/docs` 默认主页 +「最近」Tab；侧栏收回最近/共享/星标一级入口
- 我的文档库 → 主区树形缩进列表 + 编辑返回
- 云盘悬停 › + 滑入中栏文件夹树 + 右侧文件表
- 主页「上传」进入云盘并拉起上传
- 起草 `openspec/lanes/workspace-docs-ia-redesign/contract.md`
- 依赖 `workspace-owner-chip-web` 的芯片（可并行合入）

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `docs`：对齐母 change Home / Library list / Drive slide-in 的 web 行为

## Impact

| 层 | 路径 |
|----|------|
| 前端 | `web/src/pages/app/docs/index.vue`、docs 组件、stores |
| 契约 | `openspec/lanes/workspace-docs-ia-redesign/contract.md` |
| 回滚 | 恢复旧侧栏一级入口与中栏树 |
