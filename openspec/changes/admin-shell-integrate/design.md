# 设计：admin-shell-integrate

## 联调步骤

```text
1. docker compose -f deploy/compose.yml up -d
2. ./mvnw -pl relayflow-server -am spring-boot:run
3. cd web && pnpm dev
4. http://localhost:5173/admin/login
5. 登录 admin / admin123
6. 确认 navbar 租户名 ≠ Mock 硬编码（与 curl /default 一致）
7. 用户菜单 → 退出 → /admin/login
8. 直访 /admin → 重定向登录
```

## 验收标准

| 检查项 | 期望 |
|--------|------|
| 租户名 | 与 `curl .../tenant/default` 的 `data.name` 一致 |
| 退出 | token 清除，无法直访 `/admin` |
| 构建 | `pnpm build` + `mvn compile` 均通过 |

## 问题分流

| 现象 | 处理 |
|------|------|
| CORS / 401 on /default | 回 `-api` 修 Security |
| 前端未发请求 / 字段错 | 回 `-web` 修 store/api |
| 契约与实现不符 | 更新 `openspec/lanes/admin-shell/contract.md` 后两边同步 |

## 归档顺序建议

1. `admin-shell-integrate`
2. `admin-shell-web`
3. `admin-shell-api`
