## Why

`quick-views-web` + `quick-views-api` 已就绪；需去掉 store 临时合并/Mock，前端只走真实 `scope=ALL` / `ASSIGNED_BY_ME`。

## What Changes

- 删除 `USE_LOCAL_QUICK_VIEWS` 与 `quickViewsLocal.ts`
- Store 直接调用 page API
- 看板 / contract → done；母 change §2.3 勾选
- 不改 Java（api 已交付）

## Capabilities

### Modified Capabilities

- `task`：快速访问前端联调去 Mock

## Impact

`web/src/stores/tasks`、对接看板、contract 状态
