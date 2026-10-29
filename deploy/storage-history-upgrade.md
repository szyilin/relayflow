# 存储位置绑定升级

适用于 `V0.1.0.3__bind_storage_location_revisions.sql`。新二进制要求文件与上传会话绑定持久位置；旧文件的空引用不会自动使用当前默认配置。

## 维护窗口与预演

1. 停止旧实例接收上传、配置修改及文件写入，等待可确认的在途上传完成。备份 PostgreSQL（例如 `pg_dump -Fc`）、应用加密主密钥、Bootstrap 配置及各租户原加密配置。密钥与备份按现有秘密管理方式保管，不能附在报告或任务参数中。
2. 还原到隔离副本，使用新版本 Flyway 执行增量迁移。迁移只创建位置表、可空引用、索引和租户复合外键，不改旧 key、URI 或对象字节。按 codegen 文档生成临时参照并核对应用 DO/Mapper。
3. 按租户梳理原始配置与文件创建时间范围。旧 `minio://bucket/key` 不包含 endpoint；同名桶、相同 key 或一次 HEAD 成功都不能证明来源。缺少原配置、多源混杂的范围必须缩小或保留未解析，不得试探当前默认配置并自动回填。
4. 使用具备 `infra:storage:update` 权限的管理身份，调用既有 `PUT /admin-api/infra/storage/config` 注册确认过的历史位置：`operation=register_history`、`provider=minio`、`historySource=bootstrap|tenant`，以及原 endpoint、bucket、region、pathPrefix 和凭据。该操作不改新上传目标。请求不得写入命令历史或共享日志。
5. **原 key 语义也需确认**：旧实现把 `pathPrefix` 留在配置中、数据库 key 不含该前缀时，注册历史位置必须指定 `legacyRelativeKeys=true`；已含完整前缀的 key 使用 false。该标记属于位置身份，不能用于新上传。不能通过改数据库 key 补偿前缀。

## 显式映射与续跑

内部 XXL-JOB Handler 为 `storageHistoryBinding`；不暴露新的公网迁移 API。参数不包含凭据：

```json
{
  "tenantId": 123,
  "revisionId": 456,
  "source": "bootstrap",
  "bucket": "confirmed-original-bucket",
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-01-31T23:59:59Z",
  "afterId": 0,
  "limit": 100,
  "execute": false
}
```

首次不提供 `revisionId` 可只报告未解析数量；有明确目标时，dry-run 对候选对象做 HEAD 和大小核对。返回固定计划的 `planHash`、`nextId`、`done` 及 candidates/verified/bound/unresolved/rejected 数量。异常只记录 fileId，不打印密钥或签名地址。时间上界包含端点，多个计划应避免范围重叠。

审核原配置、范围、目标及预演异常后，使用完全相同的 tenant/revision/source/bucket/from/to，设 `execute=true`、`confirmPlan=<dry-run planHash>`。每次最多 500 行，只写仍为空的文件引用；下一批沿用 hash 并将 `afterId` 设为上次 `nextId`，直到 `done=true`。中断后按游标恢复；从 0 重跑也不会覆盖已有引用。更改范围或目标会产生新 hash，必须重新预演确认。HEAD 只用于校验操作者已经确认的目标，不能发现或选择历史 endpoint。

核对每个计划的绑定、未解析和异常数量，并对保留文件做原 key、URI、大小及下载字节抽检。无法确认的旧 pending 会话不补写引用，客户端重新创建上传；未过期数量会显示在存储管理页。隔离副本演练通过后，保持生产维护窗口，在生产执行同一套备份、迁移、确认和分批流程；所需历史映射核对完成后再开放新版本严格读写。不要让旧实例继续产生无位置绑定的新数据。

## 运行与恢复边界

- 保存配置只是草稿；显式启用后才影响新上传。当前 active 引用与 draft 引用分别保存。停止使用某位置不会删除历史位置或阻断它的旧文件。
- 被文件（包括保留的逻辑删除记录）或未过期 pending 会话引用的位置受到保护。变更桶、endpoint、region、前缀需明确另存位置；不搬迁旧对象。
- 同位置凭据在管理页历史位置卡片中显式轮换，先验证连接再保存。失败保留原凭据；成功影响该位置全部引用。上游撤销旧凭据后的恢复需使用仍有效的凭据重新轮换。
- 受管 MinIO 只在内部命名空间匹配时应用当前 public 路由，浏览器端口变化不改位置身份。独立存储使用自己的地址；签名后不替换 hostname。
- 未绑定文件返回 `2002001006`（`STORAGE_LOCATION_UNRESOLVED`），不会回退当前桶。普通改凭据返回 `2002001007`，应使用显式轮换。
- 保留新增表、引用和备份。**旧二进制不理解新位置引用，不能直接回退后继续写入**。故障时保持维护窗口，使用兼容版本或向前修复；不得删列、批量重置 URI、默认来源或历史引用来掩盖失败。

隔离验收夹具：`scripts/tests/fixtures/storage-history-binding.mjs`；备份副本续跑与字节验证：`StorageUpgradeCopyPostgresTest`。测试仅适用于专门的隔离数据库和 MinIO，不可指向业务环境。
