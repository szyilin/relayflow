## 1. API 层与开关

- [x] 1.1 新增 `web/src/api/app/taskListField.ts`
- [x] 1.2 扩展 `groupMoveTask` 支持 `listId` + `custom:` fieldKey
- [x] 1.3 删除 `USE_LOCAL_CUSTOM_FIELD` 与本地种子路径

## 2. Store / UI

- [x] 2.1 `customFieldsStore` 改走 API（fetch/create/update/delete/option/value/move）
- [x] 2.2 清单切换时 fetch；详情与拖拽失败 toast
- [x] 2.3 `pnpm build` + `pnpm typecheck`

## 3. 收尾

- [x] 3.1 contract → done；看板 done
- [x] 3.2 母 change §10.3 勾选；浏览器路径

### 浏览器验证

1. 起 `relayflow-server`（Flyway `V0.1.0.33`）+ `pnpm dev`
2. `/app/login` → `/app/tasks?listId=…` →「字段」新建单选 → 刷新仍在
3. 工具栏分组选该字段 → 拖拽跨桶 → 刷新保桶
4. 详情改/清空取值 → 分区同步
