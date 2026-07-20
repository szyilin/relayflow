## Context

接 `workspace-task-custom-field-web`；contract 已冻结；存储 D12 EAV。

## Goals / Non-Goals

**Goals:** Flyway + DO/Mapper + list-field REST + group-move 扩展。

**Non-Goals:** 改 web；多类型字段；公式/关联。

## Decisions

1. 镜像 list-group：BaseMapper + Wrappers，无 ExtMapper。
2. `field_key` 库内存 `custom:{id}`（插入后回写）或稳定 id 字符串；对外 groupBy 一律 `custom:{id}`。
3. `value_key` 未传时服务端生成 `opt_{rank}_{short}`。
4. 设值 / 自定义 group-move：须清单成员可 mutate + 任务在该清单 + `requireEditable`。
5. 删选项清空引用取值；删字段级联删选项与取值。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| group-move 与系统字段分支膨胀 | 委托 `TaskListFieldService.applyGroupMove` |
