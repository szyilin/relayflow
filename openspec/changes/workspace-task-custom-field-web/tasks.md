## 1. 契约与看板

- [ ] 1.1 起草 `openspec/lanes/workspace-task-custom-field/contract.md`（字段/选项 CRUD、取值、`groupBy`=`custom:{fieldId}`、`group-move` 扩展、错误码；对齐母 change D12）
- [ ] 1.2 更新 `docs/dev/api-integration-board.md`：登记 `workspace-task-custom-field` → `ui_ready`（planned→）

## 2. 前端 Mock 与 Store

- [ ] 2.1 Pinia：按 `listId` 隔离 field 定义 + options；按 `(listId, itemId, fieldId)` 存取值；种子数据
- [ ] 2.2 清单上下文「自定义字段」管理 UI：新建单选、改名、增删改排序选项；VIEWER 只读
- [ ] 2.3 详情面板：当前清单单选字段读写 / 清空

## 3. groupBy 与分区

- [ ] 3.1 工具栏分组选项加入清单自定义字段（`custom:{fieldId}`）
- [ ] 3.2 列表/看板按选项分桶 +「无分组」；拖拽更新 Mock 取值（`USE_LOCAL_CUSTOM_FIELD`）
- [ ] 3.3 `cd web && pnpm build && pnpm typecheck`

## 4. 收尾

- [ ] 4.1 写明浏览器验证路径（清单 → 建字段 → 设 groupBy → 拖拽 / 详情改值）
- [ ] 4.2 母 change `workspace-task-view-model-v1` §10.2 进度说明（web 完成；api/integrate 待开）
