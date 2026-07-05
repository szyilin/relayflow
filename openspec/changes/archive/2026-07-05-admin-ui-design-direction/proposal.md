# 提案：管理端 UI 视觉基调与规则沉淀（admin-ui-design-direction）

## Why

RelayFlow 管理端即将进入大量页面开发。若在每个纵向切片里临时决定样式与组件用法，会导致 UI 不一致、返工多，且 AI 缺少可执行的 frontend 约定。

本 change **不决定后端架构**；其职责是：

1. **阶段 0（已完成）**：选定视觉大方向，供原型实现参考；
2. **阶段 1–4（原型验收后）**：从 **你确认过的 Mock 原型代码** 中 **反抽** token、页面模式、组件与目录约定，写入 `docs/dev/` 与 Cursor 规则，作为后续所有管理端页面的真源。

> 可点击原型在 `admin-ui-prototype` 中实现；**本 change 在原型签字确认前不写 token/壳层实现代码**（避免「文档想象」与「肉眼验收」两套标准）。

## What Changes

### 阶段 0 — 定方向（✅ 已完成）

- 三种候选方向对比（design.md）；已选 **B · Clean Enterprise**
- 已确认：跟随系统主题、登录左右分栏、启用 design-preview

### 阶段 1–4 — 规则沉淀（阻塞：待 `admin-ui-prototype` 人工验收）

- 从定稿原型归纳 **`docs/dev/admin-ui-tokens.md`**、**`docs/dev/admin-ui-patterns.md`**
- 新增 **`.cursor/rules/admin-ui-patterns.mdc`**（组件映射、页面模式、Mock→API 替换约定）
- 更新本 change spec delta，校验后归档至 `openspec/specs/admin-ui-design/`

**不涉及**：Java、Flyway、`/admin-api/*`、新业务逻辑。

## Capabilities

### New Capabilities

- `admin-ui-design`：管理端视觉方向、设计 token、壳层与页面模式、组件使用规范（**规则内容以阶段 3 从原型抽取为准**）

### Modified Capabilities

- （无）— 不修改 `system`/`deployment` 等领域 API 规格

## Impact

| 范围 | 影响 |
|------|------|
| `web/` | **阶段 0**：无代码；**阶段 1–4**：无（规则写入 docs/rules，不改原型 unless 抽取时发现必须修正） |
| `docs/dev/` | 新增 `admin-ui-tokens.md`、`admin-ui-patterns.md`；`admin-ui-workflow.md` 描述全流程 |
| `.cursor/rules/` | 新增 `admin-ui-patterns.mdc` |
| Java / deploy | 无 |
| 后续 change | `admin-login-slice` 及以后 **MUST** 遵循阶段 3 沉淀的规则；**MUST NOT** 在规则归档前作为正式 UI 交付开始 |

## 工作流位置

详见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)。

```text
admin-ui-design-direction（阶段 0 决策 ✅）
  → admin-ui-prototype（Mock 原型 + 你签字）
  → admin-ui-design-direction（阶段 1–4 规则沉淀）
  → admin-login-slice / admin-shell-slice / …
```
