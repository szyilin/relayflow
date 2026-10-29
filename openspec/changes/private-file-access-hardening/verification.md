# 私有文件授权验证记录

2026-10-29；仅在专用 PostgreSQL/Redis/MinIO 容器执行验收，没有修改真实业务数据、对象字节或历史 Flyway，没有新增依赖或公开绑定端点。

## 实现与盘点

成员下载经过当前成员、租户和文件有效性检查，再查可信绑定授权，最后签名。无绑定 private 仅 creator 可读；有绑定后 creator 不再绕过业务 ACL；未知/失效/异常引用拒绝，其他独立合法引用仍可授权。管理员下载保留独立端点与 `infra:file:download`；public 文件保持公开路径，签名 TTL 仍为 private 15 分钟。

- IM file/image 与混合正文中的 fileId 在发送前统一验证已有读取资格，插入消息后同步绑定。转发先验证原附件读取资格，新消息拥有独立绑定。编辑只接受纯文本且旧绑定按当前正文重新判定；撤回/逻辑删除/失去当前会话成员关系均撤销该消息引用。
- 自定义 sticker 添加/收藏先验资格，并以 `im_user_sticker` 绑定授予所有者读取权。发送解析后的 sticker 同样经过引用检查。平台目录中的私有 fileId 不被默认放行。
- Bot 当前只生成文本/卡片，没有独立私有附件上传/发送参数；不把卡片中的任意客户端信息当成可信文件绑定。
- docs FILE 必须 ACTIVE、未逻辑删除、当前所有者匹配；RICH_DOC 分享不能代替 FILE 授权。入盘先验证已有读取资格。
- 消息/绑定同事务，绑定失败抛出并回滚；WS ACK 和消息 fanout 在提交后发送。绑定使用既有唯一索引的 ON CONFLICT 幂等插入。
- SPI 位于 infra-api；域策略只访问本域 Mapper。不存在 biz→biz 或跨域 Mapper。

## 历史修复

内部 XXL-JOB handler：`privateFileBindingRepair`。参数示例：

```json
{"tenantId":123,"bizType":"im_message","afterId":0,"limit":100,"execute":false}
```

`bizType` 可为 `im_message` / `im_user_sticker` / `doc_object`。默认 dry-run；`execute:true` 才幂等补绑定，一次仅处理一页。使用输出的 nextId 继续，done=true 结束；中断重跑相同页安全。只输出计数和异常 bizId/fileId，不输出正文、签名链接或凭据。

消息页包含无附件消息时仍推进游标。仅接受同租户真实当前引用与原上传者证据；历史转发额外核对真实源消息、文件匹配和转发人的源会话读取资格，循环/无法证明的引用拒绝。docs 核对原创建者和文件创建者；自定义表情无法证明来源时留在异常清单，由操作者核对，不猜测补授权。

发布顺序：备份 → 在副本分页预演并核对异常 → 维护窗口禁止普通流量，运行显式修复并重跑核对 → 开放严格授权。未恢复引用不通过租户全员放行兜底。修复只写绑定，不复制/删除对象，也不改 fileId/key；回退优先修复策略或关闭问题入口，不能恢复原宽松下载。

## 验证

核心自动化 9 项通过：FileAuthorizationTest 5、DocFileAccessPolicyTest 1、ImFileAccessPolicyTest 2、ImFileHistoryPostgresTest 1（真实 PostgreSQL 分页、中断重读、跨租户和可疑旧引用）。命令：

```sh
RELAYFLOW_AUTH_TEST_JDBC_URL=jdbc:postgresql://localhost:55433/member_auth_test \
RELAYFLOW_AUTH_TEST_JDBC_USER=relayflow RELAYFLOW_AUTH_TEST_JDBC_PASSWORD="$TEST_DB_PASSWORD" \
./mvnw -pl relayflow-module-infra/relayflow-module-infra-biz,relayflow-module-im/relayflow-module-im-biz,relayflow-module-docs/relayflow-module-docs-biz -am test \
 '-Dtest=*FileAuthorizationTest,*FileAccessPolicyTest,*FileHistoryPostgresTest' -Dsurefire.failIfNoSpecifiedTests=false
```

`./mvnw -pl relayflow-server -am compile` 与 `openspec validate private-file-access-hardening --strict` 通过。

真实客户端脚本 `scripts/tests/fixtures/private-file-access.mjs` 已通过：独立端口 18081/19000、测试库 member_auth_test。覆盖上传者预览、同租户/跨租户拒绝、伪造附件不落库、合法接收与转发、失去会话关系、撤回后的多引用并集、当前 FILE 所有权转移/删除、成员与管理员边界、public 和 900 秒签名。另在隔离 PostgreSQL 临时注入绑定失败触发器，验证业务消息回滚且真实 WS 不返回成功 ACK；触发器已移除。

浏览器已从 `/app/login` 登录测试账号，访问 `/app/messages` 查看撤回消息，进入 `/app/docs`；未改前端源码。既有签名 URL 在业务撤销后可继续使用到原 TTL，不承诺即时撤回。
