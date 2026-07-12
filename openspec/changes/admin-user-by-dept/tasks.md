# 任务：管理端按部门管人（admin-user-by-dept）

> **前置**：[`org-member-dept-default`](../org-member-dept-default/tasks.md) 已完成  
> **契约**：[`contract.md`](../../lanes/admin-user-by-dept/contract.md)

## Lane `-web`（admin-user-by-dept-web）

- [ ] 1.1 起草 `openspec/lanes/admin-user-by-dept/contract.md`（`deptId` 分页扩展）
- [ ] 1.2 `/admin/system/user`：左侧 `UTree` 部门树 + 选中态；右侧保留现有表格
- [ ] 1.3 `stores/user.ts` / `api/admin/user.ts`：`getUserPage` 支持 `deptId` 参数（可先 Mock 过滤）
- [ ] 1.4 新建用户路由携带 `deptId` query，创建表单默认部门
- [ ] 1.5 `cd web && pnpm build`；看板 web → `in_progress` / `ui_ready`

## Lane `-api`（admin-user-by-dept-api）

- [ ] 2.1 `UserPageReqVO.deptId`；`UserServiceImpl.getUserPage` 主部门过滤
- [ ] 2.2 与 `data_scope` 叠加验证；curl 见 contract
- [ ] 2.3 `./mvnw -pl relayflow-server -am compile`；看板 api → `ready`

## Lane `-integrate`（admin-user-by-dept-integrate）

- [ ] 3.1 用户列表 store 去 Mock（若有）；树节点切换联调分页
- [ ] 3.2 新建/编辑用户部门与树选中一致
- [ ] 3.3 更新 `docs/dev/api-integration-board.md` → **done**
- [ ] 3.4 `openspec validate admin-user-by-dept --strict`

## 下一 change

- [`workspace-contacts`](../workspace-contacts/proposal.md) — 工作台通讯录
