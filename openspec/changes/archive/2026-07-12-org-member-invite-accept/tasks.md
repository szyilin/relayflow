# Tasks：org-member-invite-accept

## 1. 规格

- [x] 1.1 proposal / design / spec delta

## 2. 后端（-api）

- [x] 2.1 `MemberInviteService`：preview + accept（NOT_JOINED → ACTIVE + JWT）
- [x] 2.2 `AppMemberInviteController` + VO；Security permitAll
- [x] 2.3 错误码 `MEMBER_INVITE_NOT_FOUND`、`MEMBER_INVITE_PASSWORD_WEAK`

## 3. 前端（-web）

- [x] 3.1 `api/app/member-invite.ts`
- [x] 3.2 `/app/invite/accept` 页面；`login.vue` 入口链接
- [x] 3.3 接受成功后写入 auth session 并跳转工作台

## 4. 验证

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 `cd web && pnpm build`
- [x] 4.3 端到端：邀请 → accept 页 → 加入 → 登录工作台
