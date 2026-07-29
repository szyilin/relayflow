## 1. 契约与文档对齐

- [x] 1.1 更新 `openspec/lanes/admin-role/contract.md`：去掉 dataScope/deptIds/必填 code；parentId 可选；补充角色成员 list/add/remove；page 可附带 admins 摘要
- [x] 1.2 更新 `docs/dev/product-permission-model.md` 与看板备注：管理面改为单轴（角色+功能权限），删除双轴 data_scope 表述

## 2. 数据库与后端（0.x 破坏性）

- [x] 2.1 新增 Flyway：DROP `sys_role_dept`、DROP `sys_role.data_scope`；CUSTOM 角色 `code=NULL`；保留 `super_admin` code 与 partial unique
- [x] 2.2 删除 DataScope 枚举/Helper/`SysRoleDept*`；Role CRUD VO/Service 去掉 dataScope；CUSTOM 强制 code=null；parentId 可选；用户分页去掉 data_scope 过滤
- [x] 2.3 实现角色成员 API（list/add/remove → `sys_user_role`）；page/get 响应去掉 dataScope，可选附带 admins；`./mvnw -pl relayflow-server -am compile` 通过

## 3. 管理端 UI

- [x] 3.1 导航/页标题改为「管理员角色」；列表列改为名称/管理员/权限摘要/操作；表单去掉编码与数据范围；上级可选并提示一级角色留空
- [x] 3.2 权限树默认折叠、勾选父级联选中子孙并展开；详情抽屉（成员/权限 Tab）接成员 API；`pnpm build` + `pnpm typecheck` 通过

## 4. 验证

- [x] 4.1 `openspec validate admin-roles-simplify --strict`；本地重置/迁移库后冒烟：超管仍在、新建角色无 code/dataScope、用户列表全量、浏览器路径 `/admin/system/role`
