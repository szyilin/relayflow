# 任务：组织成员默认主部门（org-member-dept-default）

> **Lane**：平台 / system 规则 · 无 UI · 阻塞 `admin-user-by-dept`、`workspace-contacts`

## 1. 规格与设计

- [ ] 1.1 阅读本 change `proposal.md`、`design.md` 与飞书 [创建用户 API](https://open.feishu.cn/document/server-docs/contact-v3/user/create) 部门必填说明

## 2. system-api

- [ ] 2.1 `ErrorCodeConstants` 增加 `USER_DEPT_REQUIRED`、`DEPT_ROOT_DELETE_FORBIDDEN`

## 3. system-biz — 根部门

- [ ] 3.1 `DeptService.getOrCreateRootDept(tenantId)` + 实现（读 tenant 名、缺则建根部门）
- [ ] 3.2 `deleteDept`：根部门（`parent_id=0`）禁止删除

## 4. system-biz — 用户主部门

- [ ] 4.1 `assignDept`：create 路径 `deptId==null` → 根部门；update 路径 `deptId==null` → 抛错
- [ ] 4.2 `createUser` / `updateUserDept` 走上述规则（无需改 Controller 字段必填，保持 API 兼容）

## 5. Flyway 回填

- [ ] 5.1 `V0.1.0.7__org_member_dept_default.sql`：根部门名同步为租户名；无部门成员补 `sys_user_dept`（幂等）

## 6. 验证

- [ ] 6.1 `./mvnw -pl relayflow-server -am compile`
- [ ] 6.2 启动后 SQL/接口验证：全量成员有主部门；创建用户无 deptId 有部门；不可删根部门
- [ ] 6.3 `openspec validate org-member-dept-default --strict`

## 下一 change（不在本范围）

- `admin-user-by-dept` — 管理端用户页部门树 + 按部门分页
- `workspace-contacts` — 工作台通讯录
