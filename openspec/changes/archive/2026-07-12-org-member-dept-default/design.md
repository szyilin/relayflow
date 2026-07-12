# 设计：组织成员默认主部门（org-member-dept-default）

## Context

- 产品参考：飞书「成员与部门」— 成员必在组织树上有节点；根节点 = 企业名
- 真源规格：`openspec/specs/system/spec.md` §组织架构、§用户写操作
- 现状 gap：`assignDept` 允许 null；种子用户未关联部门

## Goals / Non-Goals

**Goals:**

- 每个 `sys_tenant_user`（ACTIVE 成员）至少一条 `sys_user_dept` 且 `primary_flag = 1`
- 新建用户未传 `deptId` → 根部门
- 租户必有唯一根部门；名称默认 = `sys_tenant.name`
- 历史数据 Flyway 一次性回填
- 根部门不可删除

**Non-Goals:**

- 管理端 UI、app-api 通讯录
- 多部门分配 UI
- 租户创建时自动建根部门（V1 仍靠 Flyway seed + `getOrCreateRootDept` 兜底）

## 根部门模型

```text
sys_tenant (id=1, name=默认企业)
    └── sys_dept 根 (parent_id=0, name 同步 tenant.name)  ← 默认主部门
            └── 子部门 (parent_id=根 id)
                    └── sys_user_dept (user_id, dept_id, primary_flag=1)
```

| 项 | 约定 |
|----|------|
| 识别根部门 | 同租户下 `parent_id = 0` 的 `sys_dept`（V1 每租户唯一） |
| 默认落点 | 创建/回填时 `deptId` 缺省 → 根部门 id |
| 与飞书对齐 | 根部门展示名 = 企业/租户名，而非固定「总部」 |

## 服务层

### DeptService.getOrCreateRootDept(Long tenantId)

1. `SELECT` 租户内 `parent_id = 0` 且 `deleted = 0` 的部门
2. 若存在 → 返回 id
3. 若不存在 → 读 `sys_tenant.name`，`INSERT` 根部门（sort=0, status=0），返回 id

### UserServiceImpl.assignDept

```text
deptId 有效 → 写入主部门（现有逻辑）
deptId null（create）→ rootDeptId = deptService.getOrCreateRootDept(tenantId)
deptId null（updateUserDept）→ 抛 USER_DEPT_REQUIRED
```

创建路径 **始终** 产生主部门关联，不再 early return。

### DeptServiceImpl.deleteDept

- 若 `dept.parent_id == 0`（根部门）→ 抛 `DEPT_ROOT_DELETE_FORBIDDEN`

## Flyway：`V0.1.0.7__org_member_dept_default.sql`

1. **同步根部门名称**（tenant_id=1）  
   `UPDATE sys_dept SET name = (SELECT name FROM sys_tenant WHERE id = 1) WHERE tenant_id = 1 AND parent_id = 0 AND deleted = 0`

2. **回填无部门成员**  
   对 `sys_tenant_user` 中不存在 `sys_user_dept` 的用户，插入：
   - `tenant_id`, `user_id`, `dept_id = 根部门 id`, `primary_flag = 1`

使用 `INSERT ... SELECT ... WHERE NOT EXISTS` 保证幂等。

## 错误码（system-api）

| 码 | 含义 |
|----|------|
| `USER_DEPT_REQUIRED` | 更新主部门时 deptId 不能为空 |
| `DEPT_ROOT_DELETE_FORBIDDEN` | 不可删除根部门 |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
./mvnw -pl relayflow-server -am test -Dtest='*User*'  # 若有相关测试则跑
openspec validate org-member-dept-default --strict

# 手工
# 1. 启动后查 sys_user_dept：所有 tenant_user 均有 primary 行
# 2. POST 创建用户不传 deptId → 用户部门 = 根部门名（默认企业）
# 3. PUT updateUserDept deptId=null → 4xx
# 4. DELETE 根部门 → 4xx
```

## 后续 change 衔接

| Change | 依赖本 change |
|--------|----------------|
| `admin-user-by-dept` | 用户分页 `deptId` 过滤；UI 部门树 |
| `workspace-contacts` | `/app-api` 按部门列成员 |
