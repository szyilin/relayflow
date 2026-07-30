## 1. Token 与头像工具（人）

- [x] 1.1 在 `workspace.css` 的 `:root` / `.dark` 增加 `--ws-tint-*`（info/success/warning/danger + official/bot/agent/external/coming-soon 及 muted）；扩展 `avatar.ts`：`avatarGradientFromId`（哈希双色渐变）供字母头像复用
- [x] 1.2 接线渐变字母头像：`WorkspaceOwnerChip`、`WorkspaceRailHeader`，并 grep 扫 `UAvatar` / `avatarText` 高频表面（IM 列表、通讯录、名片）统一走同一 util

## 2. 标签与模块 tint（态）

- [x] 2.1 新增 `WorkspaceSemanticBadge`（或 UBadge 薄封装）映射 kind → `--ws-tint-*` soft；将云文档等「即将推出」及 IM/通讯录身份标替换为该组件
- [x] 2.2 为 Rail 主导航配置模块 tint（消息/通讯录/文档/日历/任务/会议/智能体）；未选中可辨、选中清晰；文字保持冷静

## 3. 空态、文档与验证（空）

- [x] 3.1 新增 `WorkspaceEmptyState`（插画 slot + 标题 + 可选描述/CTA）；接入至少：设置占位分类、一处列表空（如文档最近/任务空列表）、一处主区无内容
- [x] 3.2 同步 `docs/dev/workspace-ui-tokens.md` 与 `workspace-ui-patterns.md`（tint 表、渐变头像、Badge kind、Rail tint、EmptyState）
- [x] 3.3 验证：`openspec validate workspace-visual-polish-web --strict`；`cd web && pnpm build && pnpm typecheck`；浏览器走查 `/app` 浅色+深色（Rail、无头像用户、即将推出标签、空列表）
