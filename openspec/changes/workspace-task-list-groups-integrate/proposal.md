## Why

去掉 `USE_LOCAL_LIST_GROUPS`，清单内自定义分组改走 `/app-api/task/list-group`。

## What Changes

- API 客户端 + Store 真读写（按 listId）
- 删除本地 Mock 标志与「本地暂存，待 API」文案
- contract/看板 done；母 change §9.3

## Capabilities

### Modified Capabilities

- `task`：清单内分组前端联调

## Impact

`web/`、contract、看板、母 change
