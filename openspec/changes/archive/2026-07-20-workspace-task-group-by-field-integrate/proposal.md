## Why

group-by-field web/api 已就绪；需前端去掉本地拖拽 Mock，统一走 `PUT /group-move`。

## What Changes

- `groupMoveTask` API 客户端
- Store 乐观更新 + 真 API；删除 `USE_LOCAL_GROUP_MOVE` / `applyLocalFieldGroupMove`
- 看板拖拽（含清单 status）优先 `group-move`；`board-move` 可保留但不作为前端主路径
- contract/看板 → done；母 change §4.3

## Capabilities

### Modified Capabilities

- `task`：字段分组拖拽前端联调

## Impact

`web/`、contract、看板
