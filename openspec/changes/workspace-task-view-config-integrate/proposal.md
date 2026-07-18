## Why

view-config-web/api 已就绪；需前端去掉 localStorage 临时，改为调用 `GET/PUT /app-api/task/view-config/*`。

## What Changes

- 新增 `web/src/api/app/taskViewConfig.ts`
- Store 读写真 API；删除 `USE_LOCAL_VIEW_CONFIG` 与本地持久
- 清理旧 localStorage 键；contract/看板 → done；母 change §3.3

## Capabilities

### Modified Capabilities

- `task`：ViewConfig 前端联调去 Mock

## Impact

`web/`、contract、看板
