# 文档保存一致性验证记录

2026-10-29。按前端 → 后端 → 联调实施；未新增依赖、schema、编辑器或强制覆盖接口。

## 前端

契约已补充至 workspace-docs-library。草稿队列以账号/租户作用域和 objectId 隔离；单文档串行、递增 revision、独立确认版本和快照。防抖捕获原文档，R1 返回仅确认 R1，不回写编辑器正文；R2 用返回的新 CAS 继续保存。加载/元数据响应有独立作用域和 generation，不能复位脏草稿。

失败/冲突保留正文及原 CAS，提供重试、复制 JSON、导出 JSON、明确确认后加载服务器版本。切文档、返回、SPA 离开等待 flush；失败时可留在文档或明确放弃；beforeunload 只提示，不宣称卸载期间可靠保存。

保存请求显式捕获发起时 token；账号/租户/token 变化或 store 销毁时清理旧定时器、取消客户端请求并忽略旧响应。旧请求不能获得新 token；已经开始的服务器事务不承诺回滚。迟到旧身份的 401/403 不重定向新会话。

纯队列 `node --test web/scripts/tests/document-save-queue.test.mjs`：5 项通过（R1/R2 串行、原文档防抖、冲突保持原 CAS、失败重试、身份切换清除后续派发）。

`pnpm --dir web build` 和 `pnpm --dir web typecheck` 通过。首次沙箱构建遇到本机 watcher EMFILE，允许正常文件监听后构建通过；只有既有大 chunk 警告。

## 后端

新增明确的 touchOpened / rename / saveBody SQL，分别只写目标字段及 audit；正文保持 tenant/id/deleted/lifecycle/type/content_version 条件 CAS。DO 的 body/contentVersion 禁止普通 updateById 回写，预防后续元数据路径再次引入旧行覆盖。既有生命周期清除正文的运维语义保留；没有放宽 ACL、TableLogic 或版本冲突。

真实 PostgreSQL `DocSavePostgresTest` 2 项通过：可控并发的两种提交次序，新标题/正文并存且正文版本只加一次；陈旧普通实体不回写 body/version，跨租户/已删除 CAS 不能写入。

运行：

```sh
RELAYFLOW_AUTH_TEST_JDBC_URL=jdbc:postgresql://localhost:55433/member_auth_test \
RELAYFLOW_AUTH_TEST_JDBC_USER=relayflow RELAYFLOW_AUTH_TEST_JDBC_PASSWORD="$TEST_DB_PASSWORD" \
./mvnw -pl relayflow-module-docs/relayflow-module-docs-biz -am test \
 '-Dtest=DocSavePostgresTest,DocFileAccessPolicyTest' -Dsurefire.failIfNoSpecifiedTests=false
```

Maven compile、package 与 OpenSpec strict 已通过。既有无关 TenantBootstrapServiceTest 的失败见成员会话 change 验证记录，不将全量测试宣称通过。

## 联调与浏览器

真实 API 夹具 `scripts/tests/fixtures/docs-save-consistency.mjs` 通过：并发打开/重命名/保存，陈旧版本冲突、VIEW 保存拒绝、撤销分享后的失权、删除后的拒绝。

浏览器路径 `/app/docs`，开发服务 15173 和无 HMR 的构建预览 15174，均连接专用后端 18081：

- 控制第一个正文请求慢返回，R1/R2 实际发出版本 0/1，最终编辑器仍为 R2，acknowledged=revision=2、服务器版本=2。
- 另一窗口先提交正文，当前本地保存进入 conflict，正文保留；刷新事件被 beforeunload 阻止；返回弹出选择，取消后仍留在原文档。
- 复制/导出捕获的 JSON 包含本地冲突草稿；明确确认后加载服务器正文，状态恢复已保存。
- 模拟一次网络失败，正文保留于失败状态，显式重试沿用原版本成功。
- 构建预览中在保存中切换同企业的另一个账号，旧请求 controller 已 abort，新身份 activeDocument=null、无旧草稿；再恢复原身份重新从服务器加载。已开始的请求仍可能在服务端提交，不以取消宣称回滚。
- 待保存时立即返回，flush 完成后才清空 activeDocument，服务器读取到最后编辑的正文。

临时网络夹具只在浏览器测试期间覆盖 XHR/复制/下载，已恢复；源码和 store 没有 Mock 接线。

发布：让正在编辑者先保存再刷新。内存草稿不保证浏览器强杀、设备崩溃后的恢复；回退不重置正文或 content_version。
