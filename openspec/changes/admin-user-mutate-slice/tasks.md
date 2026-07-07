# 任务：admin-user-mutate-slice

> **Lane**：全栈 · **前置** `admin-dept-slice`、`admin-role-slice`、`system-rbac-kernel` 已完成。

## 前置

- [x] 0.1 阅读 [`openspec/lanes/admin-user-mutate/contract.md`](../../lanes/admin-user-mutate/contract.md)
- [x] 0.2 阅读 [`system-admin-v1/design.md`](../system-admin-v1/design.md) §④

## 契约

- [x] 1.1 冻结 `openspec/lanes/admin-user-mutate/contract.md`

## 后端

- [x] 2.1 扩展 `UserService`：get / update / updateStatus / updateDept / updateRole；create 支持 deptId/roleIds
- [x] 2.2 `GET /admin-api/system/user/get` + 写操作 PUT 端点 + `@PreAuthorize`
- [x] 2.3 `GET /user/page` 增加 **data_scope** 过滤
- [x] 2.4 `./mvnw -pl relayflow-server -am compile` + contract curl 验收

## 前端（web/）

- [x] 3.1 `api/admin/user.ts` 增加 mutate API 与类型
- [x] 3.2 `stores/user.ts` 增加 create/get/update 方法
- [x] 3.3 `/admin/system/user/create` 接 API；部门/角色从 dept/role API 加载
- [x] 3.4 用户列表：编辑弹窗 + 启用/禁用操作
- [x] 3.5 `cd web && pnpm build`

## 联调

- [x] 4.1 更新看板 `admin-user-mutate` → `done`
- [x] 4.2 `openspec validate admin-user-mutate-slice --strict`

## 不在本 change

- 重置密码、删除用户
- `tenant-ready-foundation` §5/§7
