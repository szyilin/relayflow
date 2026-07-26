## Context

ViewConfig 已持久化 `filters`，但前端只实现 `status IN`，快捷访问默认条件靠 API `scope`/`status` 隐式生效，筛选面板不可见。产品要求对齐飞书：可见可编辑的多条件筛选；入口带默认种子，用户改后仍留在该入口并按保存条件查询。

## Goals / Non-Goals

**Goals:**

- Feishu 式筛选 UI：当 / 且 + 字段 + 运算符 + 值；可添加 / 删除 / 清空 / 重置
- 各快捷入口默认种子写入并展示于 `filters`；重置恢复种子
- 客户端对已加载任务完整应用 AND filters；工具栏显示「筛选 N」
- 更新 lane contract 中 FilterClause 形状

**Non-Goals:**

- 本切片不改 Java / 服务端按 clause 过滤（另开 `-api`）
- 不支持 OR 组、自定义字段筛选（可后置）
- 不解决「客户端筛选 + 分页」的全局真分页（-api 后再做）

## Decisions

### D1 — FilterClause 形状

```json
{ "field": "status", "op": "IN", "values": ["TODO"] }
{ "field": "creatorId", "op": "EQ", "values": ["__ME__"] }
{ "field": "dueTime", "op": "BEFORE", "values": ["2026-07-26"] }
```

| field | 支持 op（V1） |
|-------|----------------|
| `status` | `IN` / `EQ` |
| `assigneeId` / `creatorId` / `assignerId` | `CONTAINS` / `NOT_CONTAINS` / `EQ` / `NE` / `IS_EMPTY` / `IS_NOT_EMPTY` |
| `dueTime` / `createTime` | `BEFORE` / `AFTER` / `ON_OR_BEFORE` / `ON_OR_AFTER` / `IS_EMPTY` / `IS_NOT_EMPTY` |
| `title` | `CONTAINS` / `NOT_CONTAINS` / `EQ` |

- 人员「当前用户」用哨兵 `__ME__`，应用时替换为 `auth.userId`
- 多条件 **AND**；无 OR

### D2 — 种子与持久化

| contextType | 默认 filters 种子 |
|-------------|-------------------|
| `MINE` | `assigneeId CONTAINS __ME__` + `status IN [TODO]` |
| `FOLLOWING` | `[]`（关注关系由 following API 保证） |
| `ALL` | `[]` |
| `CREATED` | `creatorId EQ __ME__` |
| `ASSIGNED_BY_ME` | `assignerId EQ __ME__` + `assigneeId NOT_CONTAINS __ME__` |
| `COMPLETED` | `status IN [DONE]` |
| `LIST` | `[]` |

- 无服务端行 / 「重置」→ 种子
- 用户改 filters 后 `PUT` 保存；再进该入口用已保存条件，**不**每次覆盖为种子
- 清空 = 用户显式 `filters: []`，尊重空列表

### D3 — 数据加载（-web）

个人快捷入口（mine / all / created / assigned_by_me / done）统一以 `scope=ALL` 拉取可见并集，**不再**用 API `status`/`CREATOR`/`ASSIGNED_BY_ME` 表达种子；收窄交给客户端 filters。`FOLLOWING` / `LIST` 仍走专用 API。

### D4 — UI

- Popover 规则列表：每行 字段 | 运算符 | 值 | 删除
- 「添加」追加空行；「清空」→ `[]`；工具栏「重置」→ 入口种子
- 人员值：至少「我」+ 可选成员搜索；日期用 date input；状态用多选/单选

## Risks / Trade-offs

- **分页失真**：客户端筛选后当前页可能偏少；接受至 `-api` 服务端过滤
- **已保存空 filters**：旧用户 COMPLETED 配置可能是 `[]`，首次看到未筛 DONE；可用重置恢复，或在 normalize 时对「从未写过种子」的旧默认做一次性迁移（本切片：仅 defaultViewConfig / 重置带种子，不强制改已存空列表）
