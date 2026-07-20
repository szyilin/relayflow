## Context

接 view-config；母 change D4。

## Goals / Non-Goals

**Goals:** 字段分组 UI；无分组；拖拽 Mock。

**Non-Goals:** 真 API 落库；个人组/清单组实现。

## Decisions

1. 通用桶模型 `{ key, label, items }`；`TaskBoardView` 改为按桶渲染。
2. `dueTime` 按自然日 `YYYY-MM-DD` 分桶；空 → 无分组。
3. `USE_LOCAL_GROUP_MOVE`：拖拽只改内存中的 task 字段。
4. `groupBy=null`：列表扁平；看板单列「全部」。

## Risks / Trade-offs

拖拽未落库 → integrate/`-api` 解决。
