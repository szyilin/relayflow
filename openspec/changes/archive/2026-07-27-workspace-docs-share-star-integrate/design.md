## Context

母 change §3：去 Mock、接真实 API。`-api` 已 `ready`（Flyway `0.1.0.39`）。

## Goals / Non-Goals

**Goals:** store 真源；E2E 可跑分享/星标/最近/leave；build + typecheck；看板 done。

**Non-Goals:** 改后端契约、FILE 分享、链接分享、实时 CRDT。

## Decisions

### D1：列表真源

`sharedItems` / `starredItems` / `recentItems` 为 API 拉取的 `ref`，不再合成 local seed。

### D2：分享缓存

打开分享对话框时 `loadShares(objectId)`；授予/改权/撤销后刷新该 object 的 shares。

### D3：VIEW 只读

`permission === 'VIEW'` 时编辑器 `editable=false`，标题不可改；保存按钮路径不触发。

### D4：墓碑打开

recent 行 `accessState != OK` 时不调 GET（或 GET 失败映射友好文案）；leave 后刷新 shared + recent。
