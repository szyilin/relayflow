单 change 前端优先；覆盖 B1 / B2，不引入新编辑器、离线持久化或多人自动合并。

## 1. 前端（web/）— 先做

- [x] 1.1 阅读工作台 UI / 前端规范，在既有 `openspec/lanes/workspace-docs-library/contract.md` 冻结草稿键、修订号、CAS、保存状态、离开与冲突恢复矩阵；更新 API 对接看板，明确现有路由 / API 不变。
- [x] 1.2 实现按账号 / 租户 / 文档隔离的修订与单文档串行队列；防抖捕获原上下文，合并最新待保存快照，保存 / 加载 / 元数据迟到响应不覆盖新输入。用可控慢响应验证 R1 返回后仍保留 R2 且按新版本续存。
- [x] 1.3 实现保存中 / 待保存 / 已保存 / 失败 / 冲突状态及恢复操作；切文档、返回和 SPA 离开 flush 并等待，失败可留在原处重试或明确放弃，beforeunload 提醒；身份切换取消旧派发并忽略旧响应。
- [x] 1.4 在 `/app/docs` 通过慢响应 / 失败夹具演示状态及交互，覆盖复制或导出本地草稿、明确加载服务端版本、账号 / 租户切换；运行 `pnpm --dir web build` 与 `pnpm --dir web typecheck`，记录前端验收证据。

## 2. 后端 — 后做

- [x] 2.1 审计实际 doc_object 元数据写入，打开记录、库 / 云盘重命名及其他元数据仅写目标字段与 audit；保留既有 CAS / ACL / TableLogic，不将完整旧实体回写，不新增 schema 或强制覆盖参数。
- [x] 2.2 在真实 PostgreSQL 使用可控并发交错验证打开 / 重命名与正文保存的两种提交次序；断言新标题与新正文同时保留、版本只随正文成功 CAS 增加，陈旧版本冲突、VIEW / 已失权 / 已删除仍拒绝；运行相关测试和 Maven compile。

## 3. 联调

- [x] 3.1 store 全部走真实 API，移除本切片临时夹具接线；浏览器 `/app/docs` 验证连续输入慢保存、快速切文档 / 返回 / 路由离开、刷新提醒、失败重试、双窗口冲突和失权，断言 UI 与落库正文 / 版本一致。
- [x] 3.2 验证 logout / 账号 / 租户切换发生在在途保存期间时，新身份不显示或提交旧草稿；补充相关自动化回归与发布说明（用户先保存再刷新、内存草稿不保证设备崩溃恢复），更新 contract / 看板。
- [x] 3.3 运行相关自动化回归、`./mvnw -pl relayflow-server -am compile`、`pnpm --dir web build`、`pnpm --dir web typecheck` 与 `openspec validate docs-save-consistency-hardening --strict`，记录浏览器验证路径及结果。
