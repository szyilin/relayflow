# 设计：组织成员邀请

## 概念分层

```text
sys_user（全局账号）          sys_tenant_user（租户成员关系）
├── 登录标识（username/mobile）  ├── status（NOT_JOINED / ACTIVE / …）
├── 密码（仅用户自己设置）        ├── 组织内角色、部门（sys_user_role/dept）
└── 注册时填写                  └── 邀请待同意时不可登录该租户
```

| 字段 | 归属 | 管理端邀请时可填 |
|------|------|------------------|
| 密码 | 账号 | 否 |
| 用户名 | 账号 | 否（后端按手机号派生） |
| 手机号 | 账号标识 | 是（必填） |
| 姓名/昵称 | 组织内展示（V1 仍写 sys_user.nickname） | 是 |
| 工作邮箱 | 组织内（V1 仍写 sys_user.email） | 否 |
| 部门、角色 | 组织 | 是 |

## 邀请流程（V1）

```text
管理员填写手机号 + 组织信息
        │
        ▼
按 mobile 查 sys_user
   ├─ 存在 → 复用 user_id
   └─ 不存在 → 创建 sys_user（username=mobile，随机不可登录密码）
        │
        ▼
校验未重复加入本租户
        │
        ▼
INSERT sys_tenant_user status=NOT_JOINED
分配主部门 + 角色
        │
        ▼
（后续）用户注册/登录后同意 → ACTIVE
```

## API

### POST /admin-api/system/user/invite

| 字段 | 必填 | 说明 |
|------|------|------|
| `mobile` | 是 | 邀请目标手机号 |
| `nickname` | 否 | 组织内姓名 |
| `email` | 否 | 工作邮箱 |
| `deptId` | 否 | 缺省落根部门 |
| `roleIds` | 否 | 预分配角色 |

权限：`system:user:create`（与创建成员同级）

### GET user/page 扩展

| 新字段 | 类型 | 说明 |
|--------|------|------|
| `memberStatus` | string | `TenantUserStatus` 枚举名 |
| `mobile` | string | 手机号 |

保留 `status` 数字字段供启用/禁用切换（0=ACTIVE，1=SUSPENDED）。

## UI

| 页面 | 变更 |
|------|------|
| 列表 | 按钮「邀请成员」；列：姓名、账号状态、手机号、部门 |
| 表单 | `max-w-4xl` 双栏；区块：基础信息、归属与权限；主按钮「发送邀请」 |

状态徽章：

| memberStatus | 展示 |
|--------------|------|
| ACTIVE | 正常 |
| NOT_JOINED | 待同意 |
| SUSPENDED | 已暂停 |
| 其他 | 枚举中文兜底 |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 浏览器：/admin/system/user → 邀请成员 → 列表出现「待同意」
curl -X POST .../user/invite -d '{"mobile":"13900001111","nickname":"李四","deptId":1}'
```
