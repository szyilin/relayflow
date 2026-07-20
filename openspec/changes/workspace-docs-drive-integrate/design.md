## Context

`-web` Mock + `-api` 已就绪。本切片去 Mock 并联调；跨容器移动同会话交付。

## Goals / Non-Goals

**Goals:** 真 API 浏览/上传/下载/删；Library↔Drive 移动（`objectId` 不变、单 Placement）；验证通过。

**Non-Goals:** 共享文件夹、Wiki、云盘内新建 RICH_DOC。

## Decisions

### D1：上传

`uploadPrivateFile` → `POST /drive/files`。

### D2：移动

`POST /drive/placements/move`：软删源 Placement、建目标 Placement，不删 `doc_object`。`FILE`→Library 拒绝（`DOC_TYPE_UNSUPPORTED`）。

### D3：面包屑

根列表用 `GET /items`；进入子文件夹时缓存路径或再拉父链——V1 用前进时压栈的本地 breadcrumb（仅 id/name），refresh 后从当前 folderId 向上需可选 `GET /folders` 链；简单实现：`folderPath` 栈随 `enterFolder` 维护，`loadListing` 只刷当前层。
