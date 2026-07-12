# Tasks：org-member-invite

## 1. 规格与契约

- [x] 1.1 proposal / design / spec delta
- [x] 1.2 `openspec/lanes/org-member-invite/contract.md` 草案

## 2. 后端（-api）

- [x] 2.1 `UserInviteReqVO` + `POST /admin-api/system/user/invite`
- [x] 2.2 `UserServiceImpl.inviteMember`：按手机号查找/创建用户，`NOT_JOINED`
- [x] 2.3 `UserRespVO` 增加 `memberStatus`、`mobile`；`UserConvert` 映射完整枚举
- [x] 2.4 错误码 `USER_ALREADY_MEMBER`

## 3. 前端（-web）

- [x] 3.1 邀请表单：加宽布局、去密码/用户名、手机号必填
- [x] 3.2 列表：邀请成员按钮、账号状态徽章、手机号列
- [x] 3.3 `api/admin/user.ts` + `stores/user.ts` 对接 invite

## 4. 验证

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 `cd web && pnpm build`
- [x] 4.3 浏览器 `/admin/system/user` → 邀请成员
