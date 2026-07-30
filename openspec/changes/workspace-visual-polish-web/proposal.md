## Why

员工工作台壳层分层与主色已就绪，但界面几乎只有灰阶 + 单一 teal，缺少「身份色 / 状态色 / 空态氛围」。对比常见企业协作产品，默认头像、标签、模块图标与空状态过于单调，整体显得简陋。需要在不污染壳层与正文的前提下，系统化补齐可复用的视觉表达。

## What Changes

- **默认头像渐变**：无头像 URL 时，首字 + 由稳定 ID 哈希选出的双色渐变底；同人跨页同色；明暗主题均可读
- **语义标签体系**：身份 / 角色 / 产品态 / 业务状态用统一 soft 标签组件与对照色 token（同 hue、明暗调亮度）
- **模块微彩**：左侧 Rail 主导航图标按模块固定 tint；未选中可辨、选中清晰；文字与壳层保持冷静
- **对照语义色 token**：在 `--ws-*` 中补充 info/success/warning/danger 及身份类 tint（含 muted），禁止业务页硬编码彩虹色
- **统一空态**：`WorkspaceEmptyState`（轻插画 + 标题 + 可选 CTA）；「即将推出」等占位用活力标签，避免死灰纯文字
- **列表微层次**：标题/元信息对比度与文件类型微着色（仅小图标级）
- **文档**：同步 `docs/dev/workspace-ui-tokens.md`、`workspace-ui-patterns.md`
- **无**后端 / API / 数据库改动（纯 `web/` 视觉切片）

## Capabilities

### New Capabilities

- `workspace-visual-system`：员工工作台视觉系统——渐变默认头像、语义标签、模块 tint、对照色 token、统一空态与「色用在人/态/空、不染框」边界

### Modified Capabilities

- `workspace-owner-chip`：无头像时的字母回退 MUST 使用稳定哈希渐变底（与视觉系统一致），而非中性灰圆

## Impact

| 层 | 路径 / 说明 |
|----|-------------|
| 前端 | `web/src/assets/css/workspace.css`；`web/src/utils/avatar.ts`；`components/workspace/`（OwnerChip、Rail、EmptyState、Badge/Tag）；IM/通讯录/云文档/任务等消费点 |
| 文档 | `docs/dev/workspace-ui-tokens.md`、`workspace-ui-patterns.md` |
| 后端 | 无 |
| 依赖 | 不新增 UI 库；继续 Nuxt UI v4 + 现有 Tailwind |
| 回滚 | 还原 token/组件与接线；壳层结构不变；无迁移 |
| 自部署 | 仅前端静态资源；无数据迁移风险 |
