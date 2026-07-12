# 设计：接受组织邀请

## 前置

- 管理端 `POST /admin-api/system/user/invite` 已创建 `sys_tenant_user.status = NOT_JOINED`
- 新账号 `sys_user.password` 为随机值，被邀请人须自行设置密码

## 接受流程（V1 · 单租户）

```text
被邀请人打开 /app/invite/accept
        │
        ▼
输入手机号 → GET preview（展示组织名）
        │
        ▼
设置密码 → POST accept
        │
        ├─ 校验 mobile 对应用户存在
        ├─ 校验当前租户存在 NOT_JOINED 关系
        ├─ 写入新密码（BCrypt）
        ├─ sys_tenant_user.status → ACTIVE
        └─ 签发 JWT（同 login 响应）
        │
        ▼
进入工作台 /app/messages
```

## API

### GET /app-api/system/member-invite/preview

| Query | 必填 | 说明 |
|-------|------|------|
| `mobile` | 是 | 邀请手机号 |

响应（有邀请时）：

| 字段 | 说明 |
|------|------|
| `tenantId` | 租户 ID |
| `tenantName` | 组织名称 |
| `nickname` | 组织内展示名（`sys_user.nickname`） |

无待接受邀请 → 业务错误 `MEMBER_INVITE_NOT_FOUND`。

**鉴权**：`permitAll`

### POST /app-api/system/member-invite/accept

| 字段 | 必填 | 说明 |
|------|------|------|
| `mobile` | 是 | 手机号 |
| `password` | 是 | 设置登录密码（≥6 位） |

成功响应同 `AuthLoginRespVO`：`accessToken`、`tenantId`。

**鉴权**：`permitAll`

## UI

| 页面 | 变更 |
|------|------|
| `/app/login` | 底部链接「收到邀请？设置密码加入」→ `/app/invite/accept` |
| `/app/invite/accept` | `workspace-auth` layout；手机号失焦拉 preview；双密码确认；主按钮「加入组织」 |

## 错误码

| Code | 说明 |
|------|------|
| `MEMBER_INVITE_NOT_FOUND` | 无待接受邀请 |
| `MEMBER_INVITE_PASSWORD_WEAK` | 密码长度不足 |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 管理端邀请 → /app/invite/accept → 加入 → 可登录工作台
```
