## Context

接 mine-groups；母 change D6。

## Goals / Non-Goals

**Goals:** 详情多选清单；移出不删任务；本地 Mock 直至 `-api`。

**Non-Goals:** `task_list_item` 表；清单内组（P7）；真 API。

## Decisions

1. `listIds[]` + `listId` = first（兼容）。
2. `USE_LOCAL_MULTI_LIST`：Store 乐观改缓存；active list 移出则从 `listItems` 去掉。
3. 可选清单来源：`myLists`（已加载的我的清单）。
4. 清单内 `groupId` 延后 P7。
