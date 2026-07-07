# 设计：用户创建/编辑（admin-user-mutate-slice）

## Context

- **上游**：`admin-dept-slice`、`admin-role-slice`、`system-rbac-kernel`
- **契约**：[`openspec/lanes/admin-user-mutate/contract.md`](../../lanes/admin-user-mutate/contract.md)

## Decisions

### D1：写操作拆分端点

update / update-status / update-dept / update-role 独立 PUT，与史诗 design §④ 一致；前端 store 顺序调用。

### D2：data_scope 过滤

`getUserPage` 使用 `DataScopeHelper` + 当前 `LoginUser`；多角色并集，SELF 与 deptIds 取并集。

### D3：雪花 ID 字符串序列化

沿用 `WebAutoConfiguration` Long→String，前端 ID 类型为 `string`。

## 非目标

- 用户名修改、密码重置、用户删除
