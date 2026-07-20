# Delta：docs · integrate

## MODIFIED Requirements

### Requirement: 文档库 store 使用 App API 真源

`useDocsStore` MUST 通过 `web/src/api/app/docs.ts` 读写文档库数据，MUST NOT 使用 store 内临时 Map 或客户端-only Markdown 导出作为持久化真源。

#### Scenario: 首次进入页面

- **WHEN** 用户打开 `/app/docs`
- **THEN** store 调用 `listLibraryTree` 与 `listRecentDocuments` 填充侧栏

#### Scenario: 保存正文版本冲突

- **WHEN** `saveDocumentBody` 返回 `DOC_VERSION_CONFLICT`（1006001003）
- **THEN** UI 展示可理解的冲突提示，不静默覆盖

#### Scenario: 导出 Markdown

- **WHEN** 用户点击导出
- **THEN** store 调用 `exportDocumentMarkdown`，下载服务端生成的 Markdown

#### Scenario: 租户切换

- **WHEN** 账号或企业切换
- **THEN** `resetLocal` 清空 API 缓存状态，后续 `ensureHydrated` 重新加载
