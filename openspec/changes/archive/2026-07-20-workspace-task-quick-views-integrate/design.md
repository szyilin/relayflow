## Context

联调切片；contract 已 api ready。

## Goals / Non-Goals

**Goals:** 去临时数据；`pnpm build` + `typecheck`；浏览器路径可验证。

**Non-Goals:** ViewConfig、多负责人、清单文件夹。

## Decisions

1. 删除 `quickViewsLocal.ts` 整文件。
2. 「已完成」改用 `scope=ALL&status=DONE`，与 contract COMPLETED 可见并集对齐。

## Risks / Trade-offs

无。
