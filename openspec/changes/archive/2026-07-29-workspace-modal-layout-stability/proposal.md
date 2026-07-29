## Why

工作台大量 `UModal` 在 loading / 空态 / 展开选择器 / 切换表单项时内容高度骤变，弹窗外壳随之上下跳动，打断操作。应统一用「外壳限高 + 内容区内滚 + 关键态预留高度」消除闪动，且不牺牲内容展示。

## What Changes

- 新增工作台弹窗内容区稳定布局约定（共享组件或统一 `ui`/`class` 模式）
- 改造高频弹窗：创建群组、邀请成员、添加机器人、MemberPicker 宿主等，使内容增减时外壳位置/高度稳定
- 文档化模式（`workspace-ui-patterns` 或本 change design），供后续弹窗复用
- 不改变各弹窗业务逻辑与 API

## Capabilities

### New Capabilities

- `workspace-modal-layout`: 工作台模态框布局稳定（限高、内滚、min-height 防塌缩）

### Modified Capabilities

- （无规格级业务行为变更）

## Impact

- **领域**：前端 `web/`（`components/workspace/*` Modal）
- **Maven**：无
- **API / DB**：无
- **回滚**：纯前端样式/结构；回退组件即可，无迁移
