## Why

multi-assignee web/api 已就绪；需前端去掉本地 Mock，改走 `PUT /assignees`。

## What Changes

- `replaceTaskAssignees` API 客户端
- Store `setAssignees` 真读写；删除 `USE_LOCAL_MULTI_ASSIGNEE`
- contract/看板 → done；母 change §5.3

## Capabilities

### Modified Capabilities

- `task`：多负责人前端联调

## Impact

`web/`、contract、看板
