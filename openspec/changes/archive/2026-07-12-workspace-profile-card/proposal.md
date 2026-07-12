# 提案：工作台个人资料卡片（workspace-profile-card）

## 背景

员工工作台左下角需对齐飞书交互：仅展示头像，点击弹出资料卡片；支持昵称 inline 编辑、头像上传；展示当前企业与「未认证」标签；退出登录与管理后台入口迁入卡片。左下角 Dock 展示本浏览器已登录账号下的全部企业，便于一键切换。

## 范围

- **前端**：`WorkspaceProfileCard`、`WorkspaceAccountDock`、多账号会话持久化、Rail 底部重构
- **后端**：`GET/PUT /app-api/system/user/profile`、工作台头像上传（`app-api/infra/file/upload-*`）
- **不做**：企业认证流程、个性签名、状态、名片/二维码

## 非目标

- 租户级昵称（V1 沿用 `sys_user.nickname` 全局昵称）
- 服务端多设备会话管理（仅浏览器 localStorage 记录多账号）
