# 设计：在线状态联调（im-presence-integrate）

## 联调清单

1. 用户 A、B 同租户；A 登录 → B 看到 A 在线（contacts/messages）
2. B 关闭浏览器 → 30s 内 A 看到 B 离线
3. 群聊会话 Aside 仍显示成员列表（非 presence 区）— 不冲突

## 验证

```bash
./mvnw compile && cd web && pnpm build
```
