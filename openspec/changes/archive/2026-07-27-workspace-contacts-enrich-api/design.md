## Context

前置：`workspace-contacts-enrich-web` 已归档；契约草案见 [`openspec/lanes/workspace-contacts-enrich/contract.md`](../../lanes/workspace-contacts-enrich/contract.md)。组织内与群组不在本切片。本 change 为 **`-api`**：表 + REST，curl 可验；前端接真数据留给 `-integrate`。

已有相近表：`sys_contact_remark`（名片备注，`(tenant_id, owner_user_id, target_user_id)`）。本切片新建独立关系表，不与备注合表。

## Goals / Non-Goals

**Goals:**

- 持久化外部联系人、加联系人申请、星标
- 实现 contract 所列 5 类端点，响应字段与前端 store 类型可对齐
- 同意申请：`pending → accepted`，并为当前用户 upsert 外部联系人行
- 鉴权：JWT + 有效组织成员（`/app-api`）；仅操作「当前租户 + 当前用户」名下数据
- 看板 API=`ready`；contract 标注已对齐实现

**Non-Goals:**

- 不改 `web/`（无 integrate）
- 不提供完整「搜索并添加外部联系人 / 发起申请」产品流（contract V1 明确仅 list + accept）
- 不实现 `rejected` 拒绝流（status 枚举可预留，API 可不暴露 reject）
- 不做拼音库；`sortLetter` 用拉丁首字母 A–Z，否则 `#`
- 不跨域直连 `im_` 表；群组仍前端滤 IM 会话

## Decisions

### D1. 三张表，均带 `tenant_id`

| 表 | 含义 | 唯一键 |
|----|------|--------|
| `sys_contact_external` | 我在本租户通讯录中的外部联系人 | `(tenant_id, owner_user_id, target_user_id)` |
| `sys_contact_request` | 指向我的加联系人申请 | 主键 `id`；查询 `(tenant_id, to_user_id)` |
| `sys_contact_star` | 我星标的人（可组织内可外部） | `(tenant_id, owner_user_id, target_user_id)` |

公共字段：`id` 雪花、`tenant_id`、`creator`/`updater`、`create_time`/`update_time`、`deleted`。

`sys_contact_request` 建议列：`from_user_id`、`to_user_id`、`status`（`pending`/`accepted`/`rejected`）、`subtitle`（可选文案）。

### D2. 「外部」语义（V1）

外部联系人 = **显式关系行**，不是「非本租户成员」的自动投影。本租户部门目录仍只走组织内 API。同意申请时写入 `sys_contact_external`；V1 不自动把其他租户成员灌进外部列表。

`orgLabel`：组装时尽量取目标用户某一活跃成员身份的租户显示名；取不到则空串或「外部」，与 contract 可选字段一致。

### D3. 发起申请不在本切片

产品 UI 无「发起」入口。验收用：

- Flyway/手工 SQL 向默认租户插入若干 `pending`/`accepted` 样例（可选，tasks 标明），或
- 文档化 curl 前 SQL 插入步骤

不新增 `POST …/request` 除非后续 change 明确要做添加流。

### D4. 模块与分层

- Controller：`AppContactController`（或等价）于 `system-biz`，路径 `/app-api/system/contact/...`
- Service 读写本域三表；展示名/头像经本域 User/TenantUser 查询（不跨域 Mapper）
- DO/Mapper：Flyway 后 `./scripts/codegen.sh --tables …` → diff 合入；自定义 SQL 用 ExtMapper

### D5. Flyway 版本

下一序：`V0.1.0.40__sys_contact_enrich.sql`（若落地时已有更高 seq，顺延第四段；前三段保持 `0.1.0`）。

### D6. ID 与排序

- 响应 `id`：关系行雪花 ID（字符串化，与前端一致）
- `userId` / `fromUserId`：目标/申请人用户 ID
- 外部列表：服务端按 `sortLetter` 再 `displayName`（`zh-CN` 可比则用 Collator，否则自然序）
- 星标 `PUT`：`starred:true` upsert；`false` 逻辑删除或物理删行（选一，推荐逻辑删与表字段一致）

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 无发起申请导致联调列表恒空 | design/tasks 提供 SQL 样例；integrate 用同一套种子或管理脚本 |
| 跨租户显示名/组织标签不准 | V1 尽力组装；空 `orgLabel` 允许 |
| 与 `sys_contact_remark` 混淆 | 文档写明：备注 ≠ 外部关系 ≠ 星标 |
| 0.x 表结构日后合并 | 遵循 database.md：破坏性改表须先征得同意 |

## Migration Plan

1. 应用 `V0.1.0.40__sys_contact_enrich.sql`（三表 + 索引）
2. 部署含新 Controller 的 `relayflow-server`
3. 回滚：停用新端点 + 回滚迁移（0.x 可 drop 表，须征得同意）；前端未接 API 前无用户数据依赖

## Open Questions

- （无阻塞）是否在默认租户 seed 申请样例：实现时可按 tasks 二选一（SQL 文档 vs 迁移 seed）；推荐 **不写死迁移 seed**，仅在 tasks/contract 附 SQL，避免污染生产。
