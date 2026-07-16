## Context

`workspace-profile-card-web` 已将主题入口迁入名片设置迷你面板。参考飞书「设置 → 通用」：独立窗、左分类、主题模式/主题色/会话显示。持久化由 `user-preference-api` 负责；本 change 纯 `-web`。

## Goals / Non-Goals

**Goals:**

- 飞书式设置窗 UI；通用三项可点可切（先本地 store / 默认常量）。
- 起草 `openspec/lanes/user-preference/contract.md`（或 settings 切片 contract）供 `-api` 对齐。
- 更新 `workspace-ui-patterns.md`。

**Non-Goals:**

- Java / Flyway。
- 账号与安全等分类的真实功能。
- 企业级默认偏好表。

## Decisions

### D1. 容器

从名片点「设置」→ 关闭迷你二级面板，打开 **宽 UModal**（左约 200px 分类 + 右内容）。避免 popover 放不下预览缩略图。

### D2. 通用三项（V1 UI）

| 项 | UI | 本地键（对齐后续 JSON） |
|----|-----|------------------------|
| 主题模式 | 跟随系统 / 浅色 / 深色（可含简易预览块） | `general.themeMode`: `auto` \| `light` \| `dark` |
| 主题色 | 色点（复用现有 primary 色板子集） | `general.themeColor`: string |
| 会话显示 | 气泡左对齐 / 左右分布 | `im.chatBubbleLayout`: `left` \| `split` |

主题写入仍先走 `useAdminColorMode` + 本地 preference store；integrate 时改为 API。

### D3. 占位分类

左栏列出账号与安全、通用、隐私、通知等；非「通用」点击 toast「功能即将推出」或展示空占位页。

## Risks / Trade-offs

- [与 api 字段漂移] → contract 草案与 web store 类型同文件或同 lane。
- [气泡布局未进消息页] → tasks 要求消息页至少读 store 应用 class，避免纯展示死控件。

## Migration Plan

前端；无 DB。

## Open Questions

- 无。
