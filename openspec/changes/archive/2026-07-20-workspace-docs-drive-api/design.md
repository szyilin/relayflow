## Context

Contract 已由 `-web` 起草；schema `V0.1.0.35` 已就绪。本切片只实现 Java API。

## Goals / Non-Goals

**Goals:** 合同内文件夹/列表/登记文件/item 变更；所有者隔离；非空文件夹删除拒绝；compile + curl smoke。

**Non-Goals:** `placements/move`、前端去 Mock、共享文件夹。

## Decisions

### D1：File 归属

`FileApi.getFile` + `FileRespDTO.creator == 当前用户`；失败 → `DOC_STORAGE_FILE_INVALID`。`bindFile(bizType=doc_object, bizId=objectId)`。

### D2：列表

实现 `GET /items?folderId=`（含子文件夹 + items）。`GET /folders?parentId=` 同步提供。不做 `/folders/tree`。

### D3：软删

删 item → 软删 `doc_drive_item` + `doc_object`；不删 infra 文件。删文件夹非空拒绝。

## Risks

`creator` 未填的历史文件会登记失败 — V1 可接受（新上传有 creator）。
