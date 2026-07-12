# 设计：管理端按部门管人（admin-user-by-dept）

## Context

- 飞书参考：管理后台「成员与部门」— 左树右表，选中部门显示**直属**成员
- 契约真源：[`admin-user-list`](../../lanes/admin-user-list/contract.md) + 本 change [`contract.md`](../../lanes/admin-user-by-dept/contract.md)
- 部门数据：已有 `GET /admin-api/system/dept/list` + `stores/dept.ts` 建树

## Goals / Non-Goals

**Goals:**

- 用户列表页左侧嵌入部门树（与部门管理页同源数据）
- 选中部门 → 用户表仅显示**主部门 = 该部门**的成员
- 未选 / 选根部门 → 显示根部门直属成员（非全租户全量，与飞书一致）
- 新建用户页：部门默认 = 列表页当前选中部门
- `deptId` 过滤与现有 `data_scope` **叠加**（取交集）

**Non-Goals:**

- 按子部门聚合成员（`includeChildren=true`）
- 在用户页内嵌部门 CRUD（仍跳转部门管理或复用 modal，V1 不强制）
- 修改部门删除/创建规则（见 `org-member-dept-default`）

## UI 布局

```text
/admin/system/user
┌─────────────────────────────────────────────────────┐
│ AdminNavbar + PageHeader「用户列表」                  │
├──────────────┬──────────────────────────────────────┤
│ 部门树        │ 搜索 + 新建用户 + 用户表格 + 分页        │
│ (UTree)      │ 列：用户名/昵称/部门/状态/操作          │
│ 根=租户名     │                                      │
└──────────────┴──────────────────────────────────────┘
```

| 交互 | 行为 |
|------|------|
| 点击树节点 | `selectedDeptId` 更新 → `fetchPage({ deptId })` |
| 首次进入 | 默认选中根部门（`parent_id=0` 的节点） |
| 新建用户 | `router.push('/admin/system/user/create?deptId=...')` |
| 编辑 | 保持现有弹窗/路由，部门下拉不变 |

## API 扩展

### GET /admin-api/system/user/page

新增 Query：

| 参数 | 类型 | 说明 |
|------|------|------|
| `deptId` | long | 可选；有值时仅返回**主部门**为该 id 的用户 |

过滤顺序（`UserServiceImpl.getUserPage`）：

```text
1. 租户成员集合
2. data_scope 过滤（现有）
3. keyword 模糊（现有）
4. deptId：JOIN sys_user_dept WHERE primary_flag=1 AND dept_id=deptId
5. 分页
```

`deptId` 缺省：V1 **不**改变现有行为（全量可见用户）— integrate 阶段前端**始终传**选中部门 id，产品行为等价飞书。

## 前端数据流

```text
deptStore.fetchList() → buildTree → selectedDeptId
userStore.fetchPage({ deptId: selectedDeptId, keyword, pageNo })
```

## 验证

```bash
# -web
cd web && pnpm build
# 浏览器：/admin/system/user — 树 + 表 Mock 或空 API

# -api
./mvnw -pl relayflow-server -am compile
curl ".../user/page?deptId=1&pageNo=1&pageSize=20" -H "Authorization: Bearer $TOKEN"

# -integrate
# 切换部门节点，表格人数变化；新建用户默认部门正确
openspec validate admin-user-by-dept --strict
```

## 下游

- `workspace-contacts` 复用同一套部门树语义（只读 `/app-api`）
