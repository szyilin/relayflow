# Tasks：workspace-docs-share-star-integrate

## 1. Store 去临时层

- [x] 1.1 删除 `USE_COLLAB_TEMP` / seed / local overlay；shared / starred / recent / shares 接 API
- [x] 1.2 `openDocument` / save / export / leave / star / removeRecent 全异步真源；错误码友好提示
- [x] 1.3 `resetLocal` 清空 API 缓存

## 2. UI 联调

- [x] 2.1 分享对话框授予/改权/撤销走 API；打开时加载 shares
- [x] 2.2 VIEW 只读编辑器；离开分享 / 星标 / 从最近移除；`ownedByMe` 替代 `isLocalObject`
- [x] 2.3 面板切换可刷新 shared / starred / recent

## 3. 验证与看板

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`
- [x] 3.2 母 change §3 勾选；看板 → **done**；`openspec validate` strict
