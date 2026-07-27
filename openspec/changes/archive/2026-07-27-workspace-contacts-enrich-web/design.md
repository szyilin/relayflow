## Context

工作台 `/app/contacts` 现为左栏部门树 + 右栏直属成员（`workspace-contacts` 已联调）。产品要对齐飞书式通讯录目录：分类导航 + 各分类主区交互。本 change 为 **`-web`**：UI + store 临时数据 + contract 草案；组织内仍复用既有 `dept/tree` 与 `list-by-dept`。

## Goals / Non-Goals

**Goals:**

- 左栏改为分类导航（图标 + 文案）：组织内联系人、外部联系人、新的联系人、星标联系人、我的群组
- 组织内：主区按部门层级（面包屑、子部门行、本部门成员），点人名片 / 发消息不变
- 外部：按显示名首字母分组列表（临时数据）
- 新的联系人：好友申请列表 + 待处理/已通过等状态（临时数据，可本地「同意」）
- 星标：列表 + 取消星标（临时数据）
- 我的群组：优先展示 IM `type=group` 会话；不足时 store 补临时项；点击进消息页
- 起草 `openspec/lanes/workspace-contacts-enrich/contract.md`；看板 `web → in_progress / ui_ready`
- 更新 `docs/dev/workspace-ui-patterns.md` 通讯录小节

**Non-Goals:**

- 邮箱联系人、服务台
- 本切片不写 Java / Flyway
- 不引入新 npm 依赖（中文首字母无拼音库时：拉丁字母 A–Z，其余归 `#`，整体按 `zh-CN` 排序）
- 不改组织内既有 REST 语义（仍为直属主部门成员）

## Decisions

1. **左栏 IA**：分类导航取代部门树；部门钻取仅在「组织内」主区（对齐飞书图二）。
2. **数据分层**：组织内继续 `api/app/contacts`；外部 / 申请 / 星标 仅 store 临时数组（integrate 删除）；群组优先 `useImStore` 会话列表。
3. **组件拆分**：`components/workspace/contacts/*` 按分类拆面板，页面只做壳与分类切换，避免单文件膨胀。
4. **契约**：新 lane `workspace-contacts-enrich` 描述后续外部联系人 / 好友申请 / 星标 API；组织内端点引用既有 `workspace-contacts` contract。

## Risks / Trade-offs

- [Risk] 中文名首字母分组粗糙（无拼音）→ Mitigation：文档与 contract 标明；`-api` 可返回 `sortLetter`
- [Risk] 临时数据与真实 API 字段漂移 → Mitigation：contract 先定字段；integrate 删 mock
- [Risk] 群组依赖 IM 会话是否已拉取 → Mitigation：进入「我的群组」时 `fetchConversations`

## Migration Plan

- 仅前端；回滚还原 `/app/contacts` 与 store 扩展即可
- 后续：`workspace-contacts-enrich-api` → `-integrate`
