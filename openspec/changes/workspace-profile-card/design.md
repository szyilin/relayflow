# 设计：工作台个人资料卡片

## UI 结构

```text
WorkspaceRail 底部
├── WorkspaceAccountDock（飞书式）
│   ├── 当前账号头像（仅圆形，无文字）→ 打开 ProfileCard
│   └── 已登录账号×企业 小方块列表（当前高亮）
└── WorkspaceProfileCard（UPopover）
    ├── 大头像（点击上传）
    ├── 昵称 inline 编辑
    ├── 企业名 + 「未认证」UBadge
    ├── 退出登录
    ├── 管理后台（isAdmin）
    └── 登录更多账号 → /app/login?addAccount=1
```

Header 区移除 `WorkspaceTenantSwitcher`（企业名仅在 ProfileCard 展示；切换走 Dock）。

## 多账号会话

localStorage `relayflow:account-dock`：

```typescript
interface AccountDockEntry {
  key: string          // `${userId}:${tenantId}`
  userId: number
  username: string
  nickname: string
  avatar?: string      // fileId
  tenantId: number
  tenantName: string
  token: string
}
```

- 登录/切换企业/更新资料后 upsert 当前 entry
- 点击 Dock 其他 entry：恢复 token + tenant，`fetchPermissionInfo`
- 同账号跨企业：优先 `switchTenant`，更新 token
- 退出：移除当前 userId 全部 entry，切到下一个或 `/app/login`
- Token 失效：移除 entry 并 toast

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/app-api/system/user/profile` | 当前用户资料 + 企业名 |
| PUT | `/app-api/system/user/profile` | 更新 nickname、avatar（fileId） |
| POST | `/app-api/infra/file/upload-session` | 任意已登录成员 |
| POST | `/app-api/infra/file/upload-confirm` | 确认上传 |

`avatar` 存 `sys_user.avatar` = fileId 字符串；展示 URL `/app-api/infra/file/public/{fileId}`。
