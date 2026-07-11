## Why

`/admin/infra/file` 仍为占位页；⑤ 直传 API 已就绪，需要管理端 UI、`useDirectUpload` 与列表/删除能力完成文件管理闭环。

## What Changes

- 起草 `openspec/lanes/infra-file/contract.md`（列表 + 删除 + 引用 upload contract）
- `web/src/composables/useDirectUpload.ts` Presigned 三阶段直传
- `web/src/api/admin/file.ts` + `stores/file.ts`
- 改造 `/admin/infra/file`：分页列表、上传、删除确认
- 补齐列表/删除后端 API（contract 定义，无独立 `-api` change）
- 导航项增加 `infra:file:list` 权限过滤

## Capabilities

### New Capabilities

（无新 spec 域；本 change 为 UI + 契约，行为增量见 delta spec）

### Modified Capabilities

- `infra`：管理端文件列表分页、逻辑删除、前端直传对接

## Impact

| 范围 | 说明 |
|------|------|
| `web/` | 文件页、composable、api、store、nav |
| `relayflow-module-infra-biz` | `GET /file/page`、`DELETE /file/{id}` |
| 看板 | `infra-file-upload` web 状态 → `ui_ready` |
