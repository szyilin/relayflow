## Context

- 已交付：`task_item`、清单 `task_list` / `task_list_member`、可选单 `list_id`、看板固定列 TODO / IN_PROGRESS / DONE。
- 2026-07-18 对照飞书截图拍板：左栏 **个人入口 / 快速访问 / 任务清单** 三类；快速访问 = 预设筛选快捷方式；「我负责的」除工具栏字段分组外，还有**个人逻辑自定义分组（计划 B）**；「我分配的」需 **分配人**；配置私有；自定义字段仅清单。
- 约束：`task_` 表前缀；跨域仅 `*-api`；`/app/tasks`；前端优先；开发期 `0.x`。

## Goals / Non-Goals

**Goals:**

- 固化快捷视图、ViewConfig、字段分组、我负责的个人组、分配人、多负责人、多清单、清单内组的目标态与切片顺序。
- 明确三套「组」语义，避免实现混淆。
- 固定三列看板改为过渡态。

**Non-Goals:**

- 本母 change 不写业务代码（实现开子 change）。
- 不一次性交付自定义字段全引擎、甘特、清单文件夹导航、跨清单大看板。
- 不引入 Gateway / 新 Maven 域。

## Decisions

### D0 — 三套「分组」语义（必读）

| 代号 | 名称 | 作用域 | 是否共享 | 何时驱动列表/看板分区 |
|------|------|--------|----------|------------------------|
| **A** | 工具栏「按字段分组」 | 任意视图上下文的 ViewConfig | 个人上下文私有；清单为共享默认 | `groupBy = fieldKey` |
| **B** | 「我负责的」个人自定义分组 | `(tenant, user)` 仅 MINE | **仅本人** | MINE 且 `groupBy = PERSONAL_CUSTOM`（或等价） |
| **C** | 清单内组 | 某 `list_id` | 清单成员可见 | LIST 且 `groupBy` 为空（或显式 list-group 模式） |

侧栏把多个清单放进文件夹 = **导航层清单文件夹**，本母 change **后置**，勿与 B/C 混名。

### D1 — 上下文与快速访问

| contextType | 默认筛选种子（V1） | 配置可见性 |
|-------------|-------------------|------------|
| `MINE` | 负责人包含当前用户 | 私有 |
| `FOLLOWING` | 当前用户已关注 | 私有 |
| `ALL` | 当前用户可见任务（权限规则 contract 定；默认可再叠未完成） | 私有 |
| `CREATED` | 创建人包含当前用户 | 私有 |
| `ASSIGNED_BY_ME` | 分配人包含我 **且** 负责人不包含我 | 私有 |
| `COMPLETED` | 完成态=已完成（常配 sort=完成时间） | 私有 |
| `LIST` | `listId` 成员任务 | 清单共享默认；OWNER/EDITOR 可保存 |

- 打开快捷入口 = 套用种子 filter；用户可在工具栏追加条件；**持久化的是该用户对该 context 的 ViewConfig**，不是改任务数据。
- 快捷视图 **不是** 清单：无成员表、无清单自定义字段定义。

### D2 — 「我负责的」与多负责人

- 进入 MINE：当前用户 ∈ 负责人集合。
- 仅创建者、非负责人 → **不**进 MINE（走 CREATED 等）。
- 迁移期单 `assignee_id` = 集合唯一元素。

### D3 — ViewConfig 合同草案

```text
ViewConfig {
  displayMode: LIST | BOARD
  groupBy:
    | null
    | { mode: FIELD, fieldKey: string }
    | { mode: PERSONAL_CUSTOM }          // 仅 MINE 合法
    | { mode: LIST_GROUP }               // 仅 LIST；可与 null 等价
  sort: { key: string, order: ASC|DESC } | MANUAL
  filters: FilterClause[]                // AND；在种子上叠加
  visibleFieldKeys: string[]
}
```

- 同一 context 的列表/看板共用 `groupBy` / sort / filters。
- 个人 context → upsert 私有行；LIST → 共享默认（VIEWER 不持久化共享配置，会话临时可允许）。

### D4 — 字段分组（A）与「无分组」

- `groupBy.mode = FIELD`：按字段值分区；空值 →「无分组」。
- 拖拽跨列写回字段；完成态不可非法清空。
- 系统字段优先：completion/status、assignees、due（桶可后细化）。
- 固定 TODO/IN_PROGRESS/DONE 三列 **不再** 为长期唯一看板模型。

### D5 — 「我负责的」个人自定义分组（B）— **V1 要做**

- 表（示意）：`task_mine_group(id, tenant_id, user_id, name, rank, is_default)`；`task_mine_group_item(task_id, user_id, group_id, rank)`（或等价）。
- 每用户恰好一个默认个人组；新建进 MINE 的任务落入该用户默认组（若尚无归属）。
- 仅当查看者打开自己的 MINE 且 `groupBy = PERSONAL_CUSTOM` 时，用 B 驱动分区。
- 用户 A 的个人组 **绝不**出现在用户 B 的 MINE。
- 删非默认组：任务回到该用户默认组；**不**删任务。

### D6 — 多清单（**BREAKING**）

- `task_list_item(list_id, task_id, group_id?, …)`；迁移后停写 `task_item.list_id`。
- 详情可编辑各清单下的 **C** 组；移出清单不删任务。
- 子任务 V1 跟随父任务清单成员集。

### D7 — 清单内组（C）

- `task_list_group`；每清单一个默认组；删组任务回默认组。
- `groupBy` 为空或 `LIST_GROUP` 时用 C 分区；字段分组优先于 C 的列呈现，但详情仍可改 C 归属。

### D8 — 分配人（**立项**）

- 系统字段 `assigner_id`（或指派事件上的分配人；列表查询需可过滤）。
- **语义：** 将负责人集合改为「不再包含自己、且包含他人」的操作者记为分配人；自建且自己仍是负责人时分配人可为 null 或本人（contract 写清「我分配的」边界与飞书一致：负责人不包含我且分配人包含我）。
- 「我分配的」查询依赖此字段；无分配人则该入口为空或仅历史回填后有数据。

### D9 — 多负责人表

- `task_item_assignee`；写路径真源；Bot/日历按负责人 fan-out。

### D10 — 与 list-board 关系

- 清单/成员 API 保留；`board-move` 在字段分组 integrate 后废弃为「更新分组字段 / 更新组归属」。

### D11 — 前端优先

- 各 P 段 `-web` → `-api` → `-integrate`；母 `tasks.md` 不替代子 change tasks。

### D12 — 清单自定义字段存储（P8 · 关闭 OQ#3）

**拍板（2026-07-20）：定义表 + 选项表 + EAV 取值表**；不做 JSONB 整包存值、不做按字段加宽列。

| 表（示意） | 职责 |
|------------|------|
| `task_list_field` | 清单侧字段定义：`list_id`、名称、`field_key`、类型、排序；V1 仅 `SINGLE_SELECT` |
| `task_list_field_option` | 单选选项：稳定 `value_key`、展示名、排序（可选色） |
| `task_item_field_value` | 任务取值：`(item_id, field_id)` → `option_id`（可空 =「无分组」） |

- **为何 EAV：** `groupBy` / `group-move` 需按 option 分桶与写回；EAV 可索引、可扩展多类型；JSONB 整包不利于按值过滤与跨任务聚合；宽表会随字段增删改 schema。
- **作用域：** 字段定义仅挂清单；快捷视图不定义字段。多清单任务：各清单字段独立；详情按当前清单上下文编辑取值。
- **`groupBy`：** `mode=FIELD` 且 `fieldKey` 指向自定义字段（contract 写死前缀，如 `custom:{fieldId}`）；桶 key = option.`value_key` 或 `__empty__`。
- **非本 P8：** 公式/关联/多选/数字、字段级权限、跨清单超级字段库。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| A/B/C 三套组命名混淆 | D0 写进 UI 文案与 API 枚举；code review 检查点 |
| 快捷视图与清单权限不一致 | ALL 可见范围必须写死 contract；禁止漏权 |
| 分配人历史为空 | 迁移：无法推断的旧数据保持 null；「我分配的」仅新指派后准确 |
| 切片过多 | 严格串行 P0–P7；每段可独立验收 |
| Bot 多负责人打扰 | 每用户 `dedupeKey` 仍唯一 |

## Migration Plan

1. P0：快捷视图查询（可先单负责人种子）+ 左栏。
2. P1：ViewConfig 表。
3. P2：字段分组替换死板三列。
4. P3：多负责人表 + MINE 语义。
5. P4：分配人 + ASSIGNED_BY_ME。
6. P5：我负责的个人组（B）。
7. P6：多清单成员表。
8. P7：清单内组（C）。
9. P8（可选）：清单自定义字段。

回滚：关新 UI；表保留；紧急时可双读退回。

## Open Questions

1. **完成态展示：** 库内三态 vs 工具栏「未完成/已完成」二分映射（建议：库内保留三态，快捷「已完成」筛 DONE；未完成含 TODO+IN_PROGRESS）。
2. **VIEWER 会话级改清单视图：** 建议允许临时、不落库。
3. ~~**自定义字段存储（P8）：**~~ → **已关闭，见 D12**（定义 + 选项 + EAV）。
4. **「全部任务」可见范围：** 仅我负责∪我创建∪我关注∪我是清单成员的任务，或更宽 — **实现 P0 contract 前必须写死**（建议：并集上述，避免全租户泄漏）。
