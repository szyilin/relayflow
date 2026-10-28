# 本地 MinIO 初始化

`init-local.sh` 是完整本地栈使用的一次性初始化入口，由 `compose.local.yml` 编排；现有开发 `compose.yml` 保持不变。

在包含 `mc`、`timeout` 的客户端容器中运行 `/bin/sh /init/init-local.sh`，只读挂载本目录到 `/init`。必填 `MINIO_ROOT_USER`、`MINIO_ROOT_PASSWORD`，可配置 `RELAYFLOW_MINIO_ENDPOINT`（默认 `http://minio:9000`）、`RELAYFLOW_MINIO_REGION`（默认 `us-east-1`）、`LOCAL_WEB_PORT`（默认 `18080`）及 `LOCAL_INIT_TIMEOUT_SECONDS`（默认 `60`）。

初始化顺序为认证 / 就绪检查、幂等确保 `relayflow` 桶、配置并检查本地 CORS。脚本不删除对象、不改桶标签 / 版本状态 / 匿名访问策略，不把桶设为公开；认证、建桶或 CORS 失败会非零退出。失败后直接重试即可，已有对象保留。

当前验证的社区版 MinIO 使用服务级 `api.cors_allow_origin`，不使用不受支持的桶级 CORS XML API。允许源固定为 `http://localhost:${LOCAL_WEB_PORT}` 与 `http://127.0.0.1:${LOCAL_WEB_PORT}`，重复运行只收敛这项受管设置，保留其他 API 设置。服务端原有 CORS 处理允许 PUT / GET / HEAD 预检并暴露 ETag；CORS 不替代签名或文件权限。

local 服务不得额外用 `MINIO_API_CORS_ALLOW_ORIGIN` 覆盖此动态配置；端口变化后重新运行初始化。其他网络 / 源的部署不应复用这个本地专用脚本。

已验证版本：MinIO `RELEASE.2025-04-22T22-12-26Z`、mc `RELEASE.2025-08-13T08-35-41Z`。执行 `./scripts/tests/local-init.test.sh` 验证首次 / 重复 / 失败重试、CORS、签名下载和停止恢复。

最终 local 使用 `Dockerfile` 的 `minio` / `mc` 两个 target 构建 `relayflow-minio:local` / `relayflow-mc:local`：旧上游容器和二进制分发已不能用于可靠冷启动。源码提交分别固定为 `0d7408fc9969caf07de6a8c3a84f9fbb10a6739e`、`7394ce0dd2a80935aded936b09fa12cbb3cb8096`，由 Go 官方模块代理下载，并由 Go 校验和数据库校验；Go / Alpine 构建输入按摘要固定。没有改 MinIO / mc 源码，运行镜像包含许可证和源码标签。初始化回归改为使用这两份本地构建镜像。
