# API 契约：org-member-invite

> **状态**：草案（2026-07-12）  
> **变更**：`org-member-invite`

## POST /admin-api/system/user/invite

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 权限 | `system:user:create` |

**Request**

```json
{
  "mobile": "13800138000",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "deptId": 1,
  "roleIds": [100]
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `mobile` | 是 | 邀请手机号 |
| `nickname` | 否 | 组织内姓名 |
| `email` | 否 | 工作邮箱 |
| `deptId` | 否 | 主部门，缺省根部门 |
| `roleIds` | 否 | 预分配角色 |

**Response `data`**：用户 ID（number）

**错误**

| code | 说明 |
|------|------|
| `USER_ALREADY_MEMBER` | 已是本组织成员 |

## GET /admin-api/system/user/page 扩展字段

列表项新增：

| 字段 | 类型 | 说明 |
|------|------|------|
| `memberStatus` | string | `ACTIVE` / `NOT_JOINED` / `SUSPENDED` / … |
| `mobile` | string | 手机号 |

## 前端

| 路由 | 说明 |
|------|------|
| `/admin/system/user/create` | 邀请成员表单（文案改造，暂保留路由） |
