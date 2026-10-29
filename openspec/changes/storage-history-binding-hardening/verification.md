# 历史存储绑定验收（2026-10-29）

用户已明确批准非破坏性 V0.1.0.3 及隔离验证。本 change 已实现；未执行真实业务环境迁移或历史回填，未复制、删除对象。生产上线需按 [升级说明](../../../deploy/storage-history-upgrade.md)执行维护窗口与确认映射。

## 迁移与生成

- 对隔离 `member_auth_test` 在 V0.1.0.2 备份为 `/tmp/relayflow-storage-before.dump`，执行 V0.1.0.3 成功。新表包含加密配置、公共字段；provider 草稿/active、file、session 引用有 tenant/id 复合外键及索引。
- CLI `scripts/codegen.sh --module infra --tables infra_storage_config_revision,infra_storage_provider,infra_file,infra_file_upload_session --migrate --output /tmp/relayflow-storage-codegen`，生成临时参照后 diff 合入 DO/基础 Mapper/XML；自定义 SQL 使用 ExtMapper。未改历史脚本或产品版本。
- 将备份还原到 `storage_upgrade_copy_20261029`，从 0.1.0.2 增量迁移至 .3 成功，日志 `/tmp/relayflow-storage-copy-migrate.log`。
- 副本内明确采用先前私有文件验收夹具的原 Bootstrap 配置（隔离 MinIO、relayflow 桶、无前缀）、原租户及 10 月 29 日固定时间范围。`StorageUpgradeCopyPostgresTest` 先 dry-run，limit=1 执行第一批后重建 worker，沿固定 hash/cursor 续跑，再从 0 重跑。结果 **bound=4、unresolved=0、rejected=0、replay=0**。逐个核对原 key、URI、size 及 GET 字节完全相同；对象侧只 HEAD/GET。

## 真实 HTTP / MinIO / 浏览器

`scripts/tests/fixtures/storage-history-binding.mjs` 使用端口 18081/19000、专用测试库和 Redis，已通过：

- Bootstrap A 会话签发 PUT 后，保存未启用 tenant B，再 confirm/download 仍用 A。
- B 会话 PUT 后切回 Bootstrap，confirm 仍 HEAD B；文件继承同一引用。
- 已引用 B 改桶/前缀普通保存拒绝；明确另存 C 后 active 仍 B，新上传仍 B；显式启用 C 才改变新上传。A/B 文件仍读取原位置，B/C 原完整前缀保留。
- B 轮换无效凭据失败且旧下载成功；轮换为隔离 MinIO 的另一个有效用户后位置不变，旧 GET 用新 access key；普通保存暗改同位置凭据拒绝；被引用配置删除拒绝。
- 夹具自有文件与 pending 会话置空绑定后返回 `2002001006`，不签名、不回退当前配置；摘要只返回未解析数量与非秘密位置元数据。
- `--verify-access`：跨租户文件不可读；同租户普通成员访问 query/save/rotation/test/delete/activation 均 HTTP 403。
- JAR 复制到独立测试路径后重启服务，`--verify-restart` 重新登录读取 A/B/C 全部文件，桶、选定凭据与字节不变。先前构建覆盖运行 JAR 引发的测试进程类加载异常已通过重启消除，最终验收用独立副本运行。

浏览器路径 `/admin/infra/storage`（前端 15173 → 后端 18081），使用新建隔离企业管理员：

- 临时 store 原型已由真实 fetch 清除，源码无 Mock 接线；显示 A/B/C 的真实引用数、当前 active/draft 及未解析提示。普通配置 Secret 留空，轮换输入使用 password。
- 显式轮换提交无效凭据，提示“轮换失败，原凭据已保留”，弹窗保留，未出现成功提示。
- 使用既有 `uploadPrivateFile` 与 `fetchAuthenticatedBlobUrl` 完成真实浏览器 PUT/confirm/鉴权下载，字节一致；原 A 与 B 文件经同一下载入口读取也一致。
- 配置响应、测试服务与迁移报告检查未出现测试 secret 明文或密文；位置 JSON 和运行配置的 toString 均排除秘密字段。

## 自动化与门禁

相关后端测试 **35 项通过**（分别执行，非全量测试声明）：StorageLocationPostgresTest 7、StorageProviderServiceImplTest 4、MinioAddressingTest 6、LocalDockerProfileTest 7、FileAuthorizationTest 5、ImFileAccessPolicyTest 2、ImFileHistoryPostgresTest 1、DocSavePostgresTest 2、StorageUpgradeCopyPostgresTest 1。覆盖逻辑删除文件引用、pending 过期、跨租户/空位置、受管 public 端口变化、独立 endpoint、凭据失败恢复，以及 HEAD 大小异常保留空引用并报告 rejected。

常用回归命令（数据库只可指向专用隔离环境）：

```sh
./mvnw -pl relayflow-server -am test -Dtest=StorageLocationPostgresTest,StorageProviderServiceImplTest,MinioAddressingTest,LocalDockerProfileTest,FileAuthorizationTest,ImFileAccessPolicyTest,ImFileHistoryPostgresTest,DocSavePostgresTest -Dsurefire.failIfNoSpecifiedTests=false
```

PostgreSQL 测试需 `RELAYFLOW_AUTH_TEST_JDBC_URL/USER/PASSWORD`。副本演练另设 `RELAYFLOW_STORAGE_COPY_JDBC_URL`（必须包含 storage_upgrade_copy）与 `RELAYFLOW_STORAGE_HISTORY_TENANT`，从全新还原副本运行，不能对已完成映射的副本重复初始化断言。

- `./mvnw -pl relayflow-server -am compile` 通过。
- `pnpm --dir web build`、`pnpm --dir web typecheck` 通过；构建仍有现有 chunk 体积提示。
- 文档保存队列回归 5 项通过（`node --test web/scripts/tests/document-save-queue.test.mjs`）。
- `docker compose --env-file deploy/.env.example -f deploy/compose.yml config --quiet` 通过；隔离后端启动及重启成功。未重建或操作业务容器。
- `openspec validate storage-history-binding-hardening --strict`、`git diff --check` 通过。

实际入口盘点：上传 PUT、confirm HEAD、下载 GET 均按 revision；历史作业 HEAD 使用显式目标。既有 deleteFile 是逻辑删除，没有新增物理清理；原 SDK deleteObject 无业务调用，不趁本切片扩大为对象删除。未解析生产历史数据仍须由操作者提供原配置与范围，不宣称能从 URI 自动恢复。
