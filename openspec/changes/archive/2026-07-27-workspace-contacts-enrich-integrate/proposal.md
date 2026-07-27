## Why

`workspace-contacts-enrich-api` 交付外部联系人 / 申请 / 星标 REST 后，前端仍读 store seed。需要联调去 Mock，使 `/app/contacts` 三分类与真实后端一致并关闭本切片。

## What Changes

- `web/src/api/app/` 增加 contact enrich 客户端，对齐冻结 contract
- `stores/contacts.ts`：外部 / 申请 / 星标改为调用真实 API；删除 `seedExternal` / `seedRequests` / `seedStarred` 及纯本地 accept/star 真源
- 同意申请、设/取消星标走后端；成功后刷新列表
- 「我的群组」确认仅 IM 会话（无临时群项真源）
- 更新 `docs/dev/workspace-ui-patterns.md`、对接看板 `web → done`
- 浏览器验收 `/app/contacts` 三分类 + 同意/星标路径

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-contacts-directory`：去掉「`-web` 可用临时数据 / 本地同意」例外；外部 / 申请 / 星标 MUST 以 API 为唯一真源；Purpose 补全为正式说明

## Impact

- **前端**：`web/src/api/app/contacts*`（或新建 `contact.ts`）、`stores/contacts.ts`、通讯录相关组件（若需 loading/error）
- **文档**：`api-integration-board.md`、`workspace-ui-patterns.md`、lane contract 状态
- **依赖**：须先完成并可用 `workspace-contacts-enrich-api`（本地 spring-boot + 迁移）
- **不改**：组织内既有 dept/user API；Java（除非联调发现 contract 缺口，另开修补）
- **回滚**：恢复 store seed（不推荐）；或回退本 integrate 提交
