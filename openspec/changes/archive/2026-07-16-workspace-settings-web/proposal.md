## Why

工作台资料名片已有「设置」入口，但仍是迷你面板 + 本地主题开关，与飞书式「左侧分类 + 右侧通用（主题 / 主题色 / 会话显示）」差距大。需要先交付可交互设置窗 UI（前端优先），偏好持久化由后续 `user-preference-api` 承接。

## What Changes

- 将名片「设置」改为打开 **独立设置窗**（UModal 或等价宽面板）：左栏分类、右栏内容。
- **通用** 分类可交互：主题模式（跟随系统 / 浅色 / 深色 + 简易预览）、主题色色点、会话气泡布局（左对齐 / 左右分布）。
- 其余分类（账号与安全、隐私、通知等）显式占位。
- Store 内用默认常量撑 UI；契约草案指向后续 preference API；本切片可不写 Java。
- 替换/收敛当前 `WorkspaceSettingsPanel` 迷你面板。

## Capabilities

### New Capabilities

- `workspace-settings`: 工作台飞书式设置窗（通用三项 + 分类占位）的产品行为

### Modified Capabilities

- （无）不改已归档主规格 REQUIREMENTS；名片菜单行为由既有 profile 切片覆盖。

## Impact

- **前端 `web/`**：`WorkspaceSettingsPanel` / 新 Settings 模态、消息页气泡布局消费（若本期接上本地 store）、`workspace-ui-patterns.md`。
- **后端**：无（本 change）；API 见 `user-preference-api`。
- **回滚**：纯前端 revert。
