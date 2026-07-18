## Context

接 group-by-field-web/api。

## Goals / Non-Goals

**Goals:** 去 Mock；拖拽落库；刷新后分组正确。

**Non-Goals:** 删后端 `board-move`；个人自定义组 / 清单组。

## Decisions

1. 统一 `moveGroupedField` → `PUT /group-move`；空桶传 `value: null`。
2. 清单 status 拖拽也走 `group-move`（`beforeId`），不再从页面调 `board-move`。
3. `boardMoveTask` / `moveBoardTask` 可留作兼容，页面不再调用。
