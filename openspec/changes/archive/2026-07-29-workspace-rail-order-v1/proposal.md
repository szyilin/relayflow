## Why

工作台左侧导航顺序写死在 `useWorkspaceNav`，所有用户相同且无法调整。用户希望产品有统一的默认顺序，同时支持个人拖拽自定义排序，并在拖拽时有清晰的落点提示。

## What Changes

- 全站硬编码 **产品默认 rail 顺序**（对所有未自定义用户生效）
- 个人可通过 **拖拽** 调整 rail 项顺序；拖拽过程展示 **插入线** 落点提示
- 个人顺序写入既有 `sys_user_preference.settings.workspace.railOrder`（C 类偏好）；未设置时回落代码默认
- 合并规则：未知 id 忽略；缺漏的已注册 rail id 按默认序追加到末尾
- **不**做管理端/租户级「改全员默认」（标 Phase 2）
- **不**引入新 DnD 依赖库（复用原生 HTML5 drag + 任务看板同类落点语义）

## Capabilities

### New Capabilities

- `workspace-rail-order`: 工作台左侧导航支持产品默认序与个人拖拽自定义序；偏好持久化于 user-preference

### Modified Capabilities

- `user-preference`: `settings` 增加 `workspace.railOrder`（string[]）；代码默认与 sanitize/merge

## Impact

- **web/**：`useWorkspaceNav`、`WorkspaceRail.vue`、`stores/userPreference`、`api/app/userPreference`
- **Java**：`UserPreferenceDefaults`（codeDefaults + sanitize）
- **API**：沿用 `GET/PUT /app-api/system/user/preference`，无新端点
- **Flyway**：无（JSONB 扩展键）
- **回滚**：前端去掉拖拽与偏好读；后端 sanitize 忽略未知键已有行为，去掉 `workspace` 命名空间即可
