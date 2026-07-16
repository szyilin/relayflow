## 1. 资料名片重排与占位菜单

- [ ] 1.1 重构 `WorkspaceProfileCard`：头像与昵称同水平轴；菜单顺序为个性签名（占位）、我的个人名片（占位）、登录更多账号、设置、退出登录；管理后台置底（`isAdmin`）；占位项统一「（占位）」标识 + toast
- [ ] 1.2 调整 `WorkspaceRailHeader`：头像与名称垂直居中同轴；移除名称旁 `WorkspaceAccountSwitcher` chevron 入口

## 2. 登录更多账号与设置

- [ ] 2.1 新增 `WorkspaceMoreAccountsPanel`：分组列出其他企业（`my-list`/`switchTenant`）与本机其他 dock 会话；底部「加入企业」占位 +「创建新账号」链注册；迁移并删除旧 switcher 双入口
- [ ] 2.2 新增 `WorkspaceSettingsPanel`：从名片打开；内含主题切换（复用 color mode）；其余设置项占位
- [ ] 2.3 从 `WorkspaceShell` 移除已登录壳层右上角主题浮层（登录/auth layout 可保留）

## 3. 会话头与右栏精简

- [ ] 3.1 消息页：去掉会话头「单聊」副标题；关闭「活跃状态」aside；群成员列表迁到会话头等价入口
- [ ] 3.2 通讯录页：去掉「活跃状态」aside

## 4. 文档与验证

- [ ] 4.1 更新 `docs/dev/workspace-ui-patterns.md`（名片菜单、主题入口、无活跃状态右栏）
- [ ] 4.2 `cd web && pnpm build && pnpm typecheck`；浏览器路径：`/app/login` → 工作台点头像验名片/设置/更多账号；`/app/messages` 验无单聊标签与无活跃状态栏
