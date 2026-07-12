# Tasks：workspace-profile-card

## 1. workspace-profile-card-web

- [x] 1.1 `WorkspaceProfileCard` + `WorkspaceAccountDock`
- [x] 1.2 `stores/accountDock.ts` 多账号持久化
- [x] 1.3 `api/app/profile.ts` + `stores/profile.ts`
- [x] 1.4 重构 `WorkspaceRail`（仅头像 + Dock；管理后台移入卡片）
- [x] 1.5 登录页 `addAccount=1` 追加账号
- [x] 1.6 `openspec/lanes/workspace-profile-card/contract.md`
- [x] 1.7 `cd web && pnpm build`

## 2. workspace-profile-card-api

- [x] 2.1 `GET/PUT /app-api/system/user/profile`
- [x] 2.2 `POST /app-api/infra/file/upload-session|upload-confirm`
- [x] 2.3 `AuthPermissionInfoRespVO` 增加 avatar
- [x] 2.4 `./mvnw -pl relayflow-server -am compile`

## 3. workspace-profile-card-integrate

- [x] 3.1 store 接真实 API；头像上传联调
- [x] 3.2 浏览器验证路径
- [x] 3.3 `pnpm build` + compile
