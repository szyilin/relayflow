## Why

`workspace-task-view-config-web` 已定稿工具栏与 contract；需后端持久化个人/清单 ViewConfig。

## What Changes

- Flyway：`task_view_config`
- `GET/PUT /app-api/task/view-config/*` 按 contract
- LIST 共享默认：成员可读，OWNER/EDITOR 可写；个人 context 仅本人
- 更新 contract / 看板；**不改** `web/`

## Capabilities

### Modified Capabilities

- `task`：ViewConfig 持久化 API

## Impact

`relayflow-module-task-*`、`relayflow-server` Flyway、contract、看板
