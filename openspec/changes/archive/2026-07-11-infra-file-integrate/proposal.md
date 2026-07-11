## Why

infra 存储切片 ③–⑦ 已实现，需全链路联调、看板收尾并 archive 子 change。

## What Changes

- 文件页增加下载入口（admin presigned 302）
- API 看板 `infra-file*` → **done**
- `openspec validate` 各子 change
- archive ③④⑤⑥⑦
- 勾选 `tenant-ready-foundation` §5.4（MinIO 租户前缀已由 objectKey 实现）

## Impact

`web/`、`docs/dev/api-integration-board.md`、`openspec/changes/archive/`
