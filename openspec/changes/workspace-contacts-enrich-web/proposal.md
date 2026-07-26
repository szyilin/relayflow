## Why

工作台通讯录目前只有「部门树 + 直属成员」一页，无法覆盖飞书式目录（组织内按部门浏览、外部联系人、好友申请、星标、我的群组）。需要先把产品信息架构与可交互 UI 定稿，再为后续 `-api` / `-integrate` 冻结契约。

## What Changes

- `/app/contacts` 左栏改为飞书式**分类导航**（含合理图标）：组织内联系人、外部联系人、新的联系人、星标联系人、我的群组；**不做**邮箱联系人。
- **组织内联系人**：主区按部门层级展示（面包屑 + 子部门行 + 本部门成员），保留点人名片与发消息。
- **外部联系人**：按显示名首字母 A–Z 分组列表（本切片 store 内临时数据）。
- **新的联系人**：好友/加联系人申请列表，展示通过/待处理等状态（临时数据）。
- **星标联系人**：星标人员列表，可取消星标（临时数据）。
- **我的群组**：展示当前用户参与的群会话（优先复用已有 IM conversation list；不足时 store 补临时项）。
- 起草 `openspec/lanes/workspace-contacts-enrich/contract.md`；更新 API 对接看板。
- 本 change **仅 `web/`**（`-web` lane）；不新增 Java / Flyway。

## Capabilities

### New Capabilities

- `workspace-contacts-directory`: 工作台通讯录分类目录 IA、各分类主区行为与临时数据约定（邮箱联系人明确不在范围）。

### Modified Capabilities

- （无）组织目录既有 `dept/tree`、`list-by-dept` 行为不变；本切片仅扩展前端目录壳层与新分类 UI。

## Impact

- **前端**：`web/src/pages/app/contacts/`、`web/src/stores/contacts.ts`（及必要拆分组件）、`docs/dev/workspace-ui-patterns.md`、`docs/dev/api-integration-board.md`
- **契约**：新建 lane `workspace-contacts-enrich`（后续 `-api` 实现外部联系人 / 好友申请 / 星标等）
- **后端**：本 change 不改 Maven 模块；后续可能涉及 `relayflow-module-system`（联系人关系）与既有 `im` 会话列表
- **回滚**：还原通讯录页与 store；无 DB 迁移
