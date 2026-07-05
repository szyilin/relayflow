# 设计：统一登录

## 路由

| 路径 | 行为 |
|------|------|
| `/` | 已登录 → `/app/messages`；未登录 → `/app/login` |
| `/app/login` | 唯一登录 UI；已登录 → `/app/messages` |
| `/admin/login` | 301 式前端重定向 → `/app/login`（兼容旧书签） |
| `/admin/**` | 需 token；无 token → `/app/login?redirect=...` |

## 联调

1. `pnpm dev`
2. 打开 `/app/login`，登录 → `/app/messages`
3. 侧栏「管理后台」→ `/admin`
4. 退出 → `/app/login`
5. 直访 `/admin/login` → 自动到 `/app/login`

**账号**：`admin` / `admin123`（需后端与库中用户）
