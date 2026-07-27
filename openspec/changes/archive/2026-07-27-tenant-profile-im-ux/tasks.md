## 1. 成员资料企业作用域

- [x] 1.1 Flyway `V0.1.0.36__tenant_user_profile.sql`：`sys_tenant_user` 增加 `nickname`/`avatar`/`signature`/`cover_file_id`，并从 `sys_user` 回填 ACTIVE 行
- [x] 1.2 更新 `SysTenantUserDO`；`UserServiceImpl` profile GET/PUT 读写成员列（昵称回退全局）
- [x] 1.3 `PermissionServiceImpl` permission-info 的 nickname/avatar 改为当前租户成员资料
- [x] 1.4 `./mvnw -pl relayflow-server -am compile` 通过

## 2. 前端头像与 Dock

- [x] 2.1 `accountDock`：`updateCurrentProfile` / `syncAllTenantsForCurrentAccount` 不再跨企业覆盖 avatar/nickname
- [x] 2.2 `WorkspaceMoreAccountsPanel` 等原生 img：加载失败回退文字 tile；梳理其他头像展示点
- [x] 2.3 切租户后 profile/auth 展示当前企业头像

## 3. IM 消息体验

- [x] 3.1 `stores/im`：按会话内存缓存；有缓存静默刷新；`resetForTenantSwitch` 清空缓存
- [x] 3.2 `messages/index.vue`：可靠滚底；未读/回到最新 FAB；进入不全量立刻已读，可视区/离开上报
- [x] 3.3 `cd web && pnpm build && pnpm typecheck` 通过

## 4. 验证

- [x] 4.1 `openspec validate tenant-profile-im-ux --strict`
- [x] 4.2 勾选已完成 tasks
