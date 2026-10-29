## Context

覆盖 B3。所有文件只持久 provider 名 `minio` 与 `minio://bucket/key`；保存租户配置会覆盖同一 provider 的 JSON。`resolveEffectiveProviderConfig` 看生效来源，`resolveProviderConfig` 看是否存在租户记录，二者可分别返回平台桶 A 与未启用租户桶 B。upload confirm 还会重新读取当前配置，无法保证与签发 PUT 一致。

## Goals / Non-Goals

**Goals:** 新上传固定来源和存储位置；保存 / 切换默认配置不改旧文件定位；历史记录有明确可预演的恢复路径；保持 Docker 双地址、原 fileId 和对象 key。

**Non-Goals:** 不引入其他存储产品、自动搬迁对象、删除旧文件、重写历史 Flyway、依靠扫描猜测所有旧 endpoint；不处理 P2 上传确认幂等与并发。规划不是数据库迁移执行授权。

## Decisions

### D1. 持久存储位置版本

用户已明确批准非破坏性增量迁移及隔离验证，实施模型如下：

| 位置 | 规划增量 | 语义 |
|---|---|---|
| `infra_storage_config_revision` 新表 | 雪花 id、tenant_id、provider、source、定位配置、加密凭据、公共字段 | 一个可持续读取的位置版本；source 为 bootstrap / tenant |
| `infra_storage_provider` | `storage_config_revision_id` 草稿及 `active_storage_config_revision_id` 生效引用 | 保存配置不等于切换上传目标，两者分别持久化 |
| `infra_file_upload_session` | nullable `storage_config_revision_id` | 创建上传会话时固定；不可被 confirm 改绑 |
| `infra_file` | nullable `storage_config_revision_id` | 从上传会话继承，历史 backfill 后非空 |

source、规范化 endpoint、bucket 与决定对象命名空间的配置属于不可被普通保存覆盖的定位信息；改变位置建立新 revision。pathPrefix 已包含在完整 objectKey 中，下载不能再次追加当前前缀。provider / storage_uri 保留用于兼容展示与迁移证据，不再承担唯一解析依据。

历史旧 key 未包含配置前缀时，以显式 `legacyRelativeKeys=true` 注册历史位置，由 SDK 应用原前缀，数据库 key 保持原样；该位置禁止用于新上传。新上传一律存完整 key，运行时配置不再追加前缀。位置 hash 包含来源、规范内部 endpoint、桶、region、前缀与旧 key 语义，租户内唯一；引用用 tenant/id 复合外键隔离。

同一位置的凭据可由显式轮换操作更新加密字段：必须保持位置身份不变，提示影响该位置所有历史引用并验证连接；不能借“轮换”修改 endpoint / bucket，也不能改变其他位置凭据。普通保存遇到凭据变更时必须走同一明确规则。这样避免把凭据永久冻结后无法进行正常密钥轮换。

### D2. 固定上传与读取来源

- Bootstrap 配置仍来源于部署；在首次实际上传 / 明确迁移绑定时创建或取得本租户对应位置，不在每个租户首次访问时预插业务偏好。保存但未启用 tenant 配置只建立草稿位置，不影响当前目标。
- 创建上传会话时解析当前生效来源并绑定 revision，再签 PUT；confirm 必须按 session revision 做 HEAD，并把引用带入 file。期间切换源不改变原会话。
- 下载按 file revision 解析；先执行 A1 的业务授权，授权后才生成签名。不存在、跨租户或已停用但仍被引用的位置不得静默落到当前默认配置。
- 所有对象操作入口盘点后统一解析：PUT、HEAD、GET，以及实际存在的清理 / 删除流程。既有逻辑删除保持，不趁机新增物理删除。
- 被引用位置不能被删除或改定位；可停止接收新上传，同时保留旧对象读取。删除草稿配置和删除历史位置是不同操作，引用保护包含未过期 session 与文件记录。

### D3. 双地址是路由适配，不是对象身份

持久保存内部定位与位置所属命名空间；本机浏览器的 localhost / 发布端口不作为身份字段。仅在能确认受管命名空间匹配时，于签名前应用当前 internal / public 路由，保留原位置 bucket、region、完整 objectKey 与明确选择的凭据。独立存储不强制套用平台地址；不能在签名后换 hostname。

位置版本不能把“改本地浏览器端口”误认为“把历史文件迁移到另一桶”。改变实际存储命名空间必须建新位置；跨位置对象复制属于另一个迁移项目。

### D4. 历史记录必须经过显式映射

- 增量引用先允许 null，提供按 tenant / source / bucket / 时间范围的 dry-run 清单。列出可用配置、候选位置、未解析 / 多义 / 对象校验失败计数，不输出密钥或签名 URL。
- 旧 provider + URI 无法证明原 endpoint。同名桶 / 相同 key 不能自动认定同源。操作者必须补充历史配置并确认映射；HEAD 的大小等校验只能作为证据，不能凭一次成功探测猜测来源。
- 映射计划需固定输入范围与目标 revision；执行仅填仍为空且符合预演条件的引用，不改已有绑定，不复制 / 删除对象。幂等分批、支持中断续跑、保留可审计的计数与异常清单。
- 新读取遇到未解析记录返回 `STORAGE_LOCATION_UNRESOLVED` 等稳定可恢复错误，不去当前配置试探。不能解析的上传中会话明确过期 / 重建，不重签为另一个位置。
- 生产升级先在副本预演，确认需要保留访问的旧文件已完成映射，再在维护窗口部署严格读取。若原配置已丢失，必须标记未恢复；不能宣称自动迁移成功。

### D5. 前端先明确配置操作的影响

在既有 infra-storage-config contract 冻结：保存草稿、生效来源切换、位置改变、凭据轮换、被引用位置保护及历史未解析计数。沿用 `/admin/infra/storage` 与 Nuxt UI，响应不返回 secret。提示“切换仅影响新上传，历史位置保留”，轮换单独说明影响范围；异常不可被成功 toast 掩盖。不另建复杂存储运维产品。

## Risks / Trade-offs

- 原字段丢失历史 endpoint → 显式映射与原配置备份，不能仅凭 bucket 自动完成。
- 引用字段与程序版本不匹配 → 增量列、维护窗口、先绑定后严格校验；禁止旧程序继续写无 revision 的新会话。
- 凭据轮换可能切断旧读取 → 校验位置不变、明确影响范围、保留故障恢复所需备份；不承诺被上游撤销的旧凭据仍可用。
- 增加位置表与解析步骤 → 换取历史可定位性，不引入新中间件；状态缓存必须尊重轮换与引用保护。

## Migration Plan

1. 实施前确认新增 Flyway 许可，读 CONTRIBUTING / database / codegen；仅准备非破坏性增量，不 squash 或重写当前 init_foundation。
2. 备份 DB 和现有 Bootstrap / tenant 加密配置，副本执行 dry-run；补齐原配置并确认映射计划。
3. CLI codegen 临时目录 → diff 合并；维护窗口执行增量迁移与幂等绑定，验证数量、原对象字节和异常清单，再启用严格读写。
4. 先完成 private-file-access-hardening；授权失败不得签 URL。验证 Bootstrap / tenant、未启用保存、切换期间 confirm、凭据轮换、旧对象读取和 Docker 端口变化。
5. 回滚保留新增位置与引用。旧版程序不理解位置绑定，不能直接回退继续写；需要维护窗口使用兼容版本 / 向前修复，不删列、重置默认配置或批量改写旧 URI。

## Open Questions

迁移许可已取得。隔离副本使用本轮测试夹具明确已知的 Bootstrap 原配置和时间范围完成映射；真实环境的历史配置及范围仍由操作者逐项确认，不能把测试成功视作生产数据已经恢复。没有新端口或必需环境变量；离线环境依赖已有 DB / MinIO，不调用公共云完成迁移。
