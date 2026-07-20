# API 契约：workspace-task-custom-field

> **状态**：done（integrate 完成；无本地 Mock）  
> **起草**：`workspace-task-custom-field-web`  
> **实现**：`workspace-task-custom-field-api`  
> **联调**：`workspace-task-custom-field-integrate`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md) D12  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-group-by-field`](../workspace-task-group-by-field/contract.md)、[`workspace-task-view-config`](../workspace-task-view-config/contract.md)、[`workspace-task-list`](../workspace-task-list/contract.md)

## 背景

清单侧 **单选自定义字段** 可作为工具栏 `groupBy` 分组源。快捷视图不定义字段。存储对齐母 change **D12**：定义表 + 选项表 + EAV 取值表。

## 鉴权

| 操作 | 角色 |
|------|------|
| 读字段定义 / 取值 | 清单成员 |
| 建改删字段 / 选项 | OWNER \| EDITOR |
| 改任务取值 / 拖拽分桶 | OWNER \| EDITOR（与 `requireEditable` / 清单 mutate 一致） |
| VIEWER | 只读 |

## 数据模型（`-api`）

### `task_list_field`

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `list_id` | 清单 |
| `name` | 展示名 |
| `field_key` | 稳定键（可与 id 同源；API 对外 `groupBy` 用 `custom:{id}`） |
| `field_type` | V1 仅 `SINGLE_SELECT` |
| `rank` | 排序 |

### `task_list_field_option`

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `field_id` | 字段 |
| `value_key` | 稳定桶 key（分桶 / group-move `value`） |
| `label` | 展示名 |
| `rank` | 排序 |
| `color` | 可选 |

### `task_item_field_value`

| 列 | 说明 |
|----|------|
| `tenant_id` | 租户 |
| `item_id` | 任务 |
| `field_id` | 字段 |
| `option_id` | 可空 =「无分组」 |
| UNIQUE | `(item_id, field_id)` |

删选项：引用该选项的取值清空为 null。删字段：级联删选项与取值（不删任务）。

## `groupBy` 约定

```text
{ mode: FIELD, fieldKey: "custom:{fieldId}" }
```

- 桶 key = option.`value_key`；空值 = `__empty__`（展示「无分组」）
- 仅在 **清单上下文** 且字段属于当前 `listId` 时合法
- 与系统字段 `status` / `dueTime` / `assigneeId` 共用 FIELD 模式；自定义字段优先走本契约取值 API / group-move 扩展

## REST

前缀：`/app-api/task/list-field`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list?listId=` | 字段定义 + 选项（可含当前页任务取值，或另端点） |
| `POST` | `/create` | `{ listId, name, fieldType: "SINGLE_SELECT", options: [{ label, valueKey? }] }` |
| `PUT` | `/update` | `{ id, name?, rank? }` |
| `DELETE` | `/delete?id=` | 删字段 |
| `POST` | `/option/create` | `{ fieldId, label, valueKey?, rank? }` |
| `PUT` | `/option/update` | `{ id, label?, rank?, color? }` |
| `DELETE` | `/option/delete?id=` | 删选项；取值清空 |
| `PUT` | `/value` | `{ listId, itemId, fieldId, optionId: string \| null }` |

### 扩展 `PUT /app-api/task/item/group-move`

在既有系统字段之上增加：

```json
{
  "id": "1001",
  "fieldKey": "custom:2002",
  "value": "high",
  "listId": "3003",
  "beforeId": null
}
```

| 字段 | 说明 |
|------|------|
| `fieldKey` | `custom:{fieldId}` |
| `value` | option.`value_key` 或 `null` / `__empty__` |
| `listId` | **自定义字段必填**（多清单取值隔离） |

`value` 写回 `task_item_field_value.option_id`（按 value_key 解析）。

## 错误码

| code | 说明 |
|------|------|
| `TASK_LIST_FIELD_NOT_FOUND` | 字段不存在或不属于清单 |
| `TASK_LIST_FIELD_OPTION_NOT_FOUND` | 选项不存在 |
| `TASK_LIST_FIELD_FORBIDDEN` | 无权改定义 |
| `TASK_LIST_FORBIDDEN` | 非清单成员 |
| `TASK_GROUP_MOVE_INVALID` | fieldKey/value/listId 非法 |
| `TASK_FORBIDDEN` / `TASK_NOT_FOUND` | 任务无权 |

## curl（目标态）

```bash
# 列字段
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/list-field/list?listId=$LIST_ID"

# 创建单选
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"listId":"'"$LIST_ID"'","name":"优先级","fieldType":"SINGLE_SELECT","options":[{"label":"高"},{"label":"中"},{"label":"低"}]}' \
  "$BASE/app-api/task/list-field/create"

# 设值
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"listId":"'"$LIST_ID"'","itemId":"'"$TASK_ID"'","fieldId":"'"$FIELD_ID"'","optionId":"'"$OPT_ID"'"}' \
  "$BASE/app-api/task/list-field/value"

# 拖拽分桶
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"'"$TASK_ID"'","fieldKey":"custom:'"$FIELD_ID"'","value":"high","listId":"'"$LIST_ID"'"}' \
  "$BASE/app-api/task/item/group-move"
```

## `-web` 临时行为（integrate 删除）

~~`USE_LOCAL_CUSTOM_FIELD`~~：已删除；定义/取值/拖拽均走 `/list-field/*` 与 `group-move`。
