# Delta：docs · share-star integrate

## MODIFIED Requirements

### Requirement: 分享 / 星标 / 最近 store 使用 App API 真源

`useDocsStore` MUST 通过 `web/src/api/app/docs.ts` 读写分享、与我共享、星标与最近足迹；MUST NOT 使用 `USE_COLLAB_TEMP` 或 store 内 seed 作为持久化真源。

#### Scenario: 首次进入页面

- **WHEN** 用户打开 `/app/docs`
- **THEN** store 调用 library tree、recent、shared、starred 填充侧栏面板

#### Scenario: 分享授予

- **WHEN** 所有者在分享对话框选择成员并授予 VIEW 或 EDIT
- **THEN** store 调用 `createDocumentShare`（或已有则更新），并刷新该文档协作者列表

#### Scenario: 离开分享

- **WHEN** 接收方离开分享
- **THEN** store 调用 `leaveSharedDocument`；正文不可再打开；最近列表可出现 `NO_ACCESS` 墓碑

#### Scenario: 星标

- **WHEN** 用户对可读文档加星或取消星标
- **THEN** store 调用 `starDocument` / `unstarDocument` 并刷新星标列表

#### Scenario: 从最近移除

- **WHEN** 用户从最近移除一项
- **THEN** store 调用 `removeRecentDocument`，不删除文档本体

#### Scenario: 租户切换

- **WHEN** 账号或企业切换
- **THEN** `resetLocal` 清空 API 缓存，后续 `ensureHydrated` 重新加载
