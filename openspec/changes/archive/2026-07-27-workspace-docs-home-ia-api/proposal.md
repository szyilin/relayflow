## Why

按 `workspace-docs-ia-redesign` contract 补齐列表 VO / 可选 UserBasic 头像；默认无 Flyway。

## What Changes

- 对齐 recent/shared/starred/drive/library 列表字段与头像
- 可选扩展 `UserBasicDTO.avatarFileId`

## Capabilities

### Modified Capabilities

- `docs`：列表元数据 API 对齐 contract

## Impact

`relayflow-module-docs-*`、可能 `system-api`；默认无新表。
