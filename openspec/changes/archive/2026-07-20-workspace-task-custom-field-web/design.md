## Context

母 change `workspace-task-view-model-v1` P8；D12 已拍板 **定义表 + 选项表 + EAV**。系统字段 `groupBy`（status / dueTime / assigneeId）与清单内组（C）已交付；本切片仅 **前端 + contract**。

## Goals / Non-Goals

**Goals:**

- 清单侧单选字段定义 / 选项 CRUD（Mock）
- 任务在当前清单上下文的单选取值编辑
- `groupBy.fieldKey = custom:{fieldId}` 驱动列表/看板分桶与拖拽 Mock
- 冻结 `openspec/lanes/workspace-task-custom-field/contract.md`

**Non-Goals:**

- Flyway / Java / 真 EAV 落库（`-api`）
- 多选、文本、数字、公式、关联字段
- 快捷视图定义自定义字段
- 字段级权限、跨清单字段库

## Decisions

1. **存储合同（对齐 D12，本切片只写 contract）：** `task_list_field` + `task_list_field_option` + `task_item_field_value`；前端 Mock 镜像该形状。
2. **`fieldKey`：** `custom:{fieldId}`，与系统 `status` / `dueTime` / `assigneeId` 区分；桶 key = option.`valueKey` 或 `__empty__`。
3. **权限（UI 先行）：** OWNER/EDITOR 可改字段定义与取值；VIEWER 只读。与清单成员角色一致。
4. **与 LIST_GROUP：** 当 `groupBy.mode=FIELD`（含自定义）时字段分组优先于 C；详情仍可改 C 归属与自定义取值。
5. **多清单：** 字段定义按 `listId` 隔离；详情展示「当前清单」字段集；Mock 按 `(listId, itemId, fieldId)` 存值。
6. **临时开关：** `USE_LOCAL_CUSTOM_FIELD`（或 store 内等价）；`-integrate` 删除。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Mock 与 EAV contract 漂移 | `-web` 先写死 contract；Mock 字段名对齐 |
| 与系统字段 group-move UI 分叉 | 复用现有分区/拖拽壳，仅扩展 fieldKey 解析 |
| 删选项后取值悬空 | contract：删选项 → 取值清空为「无分组」 |

## Open Questions

（无——D12 已关闭；选项是否带色码留给 contract 可选字段。）
