## Why

母 change [`workspace-docs-drive-v1`](../workspace-docs-drive-v1/proposal.md) §2：前端优先交付「我的文件夹」浏览/上传形状，并起草 lane contract，供后续 `-api` / `-integrate` 对齐。

## What Changes

- `openspec/lanes/workspace-docs-drive/contract.md` 草案
- `/app/docs` 启用「云盘」：面包屑、文件夹列表、新建文件夹、上传入口
- Pinia 临时本地数据撑 UI（integrate 删除）
- FILE 下载按钮对齐既有 `downloadAuthenticatedFile` / infra download 路径形状
- `pnpm build` + `pnpm typecheck`

## Capabilities

### Modified Capabilities

- `docs`: 工作台云盘 UI 入口与列表交互（API 仍为草案）

## Impact

- `web/` 页面、store、api 类型；lane contract；对接看板 `web → ui_ready`
- 不修改 Java
