## Context

前置：`docs-drive-schema-v1` 表已就绪。本切片仅 UI + contract + store 临时数据。

## Goals / Non-Goals

**Goals:** `/app/docs` 云盘面板可演示；contract 供 `-api`；下载按钮形状对齐 infra。

**Non-Goals:** 真实 docs drive API、跨容器移动、共享文件夹、Wiki。

## Decisions

### D1：临时数据

Drive 状态放 `stores/docsDrive.ts`（与 library 的 `docs` store 分离）。上传在 web 阶段用本地 Blob Map 模拟 `storageFileId`；不强制打真实 MinIO（integrate 再接）。

### D2：主区信息架构

选「云盘」后主区展示浏览器（面包屑 + 工具栏 + 列表）；左侧仅容器导航。点击 FILE → 下载；点击 RICH_DOC（若有）→ 复用既有 `openDocument`。

### D3：下载形状

UI 调用 `downloadAuthenticatedFile(\`/app-api/infra/file/download?fileId=${id}\`, title)`；mock 阶段若无真实 fileId，则回退为本地 Blob 下载（同按钮入口）。

## Risks

临时 store 若残留到 integrate → tasks 明确删除。
