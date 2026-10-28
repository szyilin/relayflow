# 本地后端容器契约

完整本地栈显式注入 `SPRING_PROFILES_ACTIVE=local-docker`，使用 `application-local-docker.yml`；旧宿主开发入口仍使用 `dev`。镜像本身不强制修改 profile，调用方必须选择正确模式。

## 配置注入

后续 `compose.local.yml` 按本表显式选择变量，不把整个本地 env 无差别传入所有容器。

| 来源 / 变量 | local-docker 的使用方式 |
|---|---|
| `POSTGRES_DB`、`POSTGRES_USER`、`POSTGRES_PASSWORD` | 内部数据库 `postgres:5432`，使用持久用户名 / 密码；可显式覆盖 `SPRING_DATASOURCE_URL` |
| `SPRING_REDIS_HOST`、`SPRING_REDIS_PORT` | 默认 `redis:6379`；宿主机发布端口 `REDIS_PORT` 不影响此内部端口 |
| `RELAYFLOW_JWT_SECRET`、`RELAYFLOW_ENCRYPT_AES_KEY` | 必须传入持久密钥，不使用开发默认值 |
| `MINIO_ROOT_USER`、`MINIO_ROOT_PASSWORD` | 必须传入，与初始化服务一致 |
| `RELAYFLOW_MINIO_ENDPOINT`、`RELAYFLOW_MINIO_PUBLIC_ENDPOINT`、`RELAYFLOW_MINIO_REGION` | 默认 `http://minio:9000`、`http://localhost:${MINIO_API_PORT:-19000}`、`us-east-1`；内外地址分别参与服务端 I/O 和浏览器签名 |
| `RELAYFLOW_LIVEKIT_API_KEY`、`RELAYFLOW_LIVEKIT_API_SECRET` | 必须传入，与 LiveKit 服务一致 |
| `RELAYFLOW_LIVEKIT_URL`、`RELAYFLOW_LIVEKIT_API_URL` | 默认浏览器 `ws://localhost:${LIVEKIT_PORT:-7880}`，后端 Room Service 为 `http://livekit:7880` |
| `XXL_JOB_ACCESS_TOKEN` | 持久调度 token；也支持直接注入 `RELAYFLOW_JOB_ACCESS_TOKEN` |
| `RELAYFLOW_JOB_ADMIN_ADDRESSES`、`RELAYFLOW_JOB_EXECUTOR_ADDRESS` | 默认 `http://xxl-job-admin:8080/xxl-job-admin`、`http://server:9999/`；不使用宿主机回调地址 |
| `RELAYFLOW_TENANT_*` | 默认单租户、默认租户 1、关闭开放注册，支持已有环境变量覆盖 |

容器 API 为 8080，执行器默认 9999，日志默认 `/app/logs/xxl-job/jobhandler`。前端和 Admin 通过项目网络连接；对外发布由完整 Compose 决定。基础设施初始化完成后再启动 server，不把配置文件绑定测试等同于完整服务联调。

## 镜像构建与验证

在仓库根目录执行：

```bash
docker build -f relayflow-server/Dockerfile -t relayflow-server:local .
./scripts/tests/server-image.test.sh relayflow-server:local
```

构建镜像固定 Java 21 JDK 摘要，运行镜像固定 Java 21 JRE 摘要；实际版本随本 change 验证记录保存。镜像由 Maven Wrapper 从源码构建，不复制宿主机 JAR。Maven 仓库 / Wrapper 目录使用 BuildKit 缓存，默认使用仓库声明及官方 Maven Central，不修改宿主机 daemon、Alpine 软件源或 Maven 配置。

构建只复制 Maven 父工程、Wrapper 与后端模块，保留 Maven 阶段日志；前端、运行配置目录和宿主机 JAR 不作为构建输入。首次下载依赖可能较慢，后续复用仓库与 Wrapper 缓存；`-U` 允许重新检查此前下载失败的依赖，避免失败缓存阻断修复后的重试。

确需自定义仓库时可用 `--secret id=maven_settings,src=/absolute/path/settings.xml`，自定义 settings 放在仓库之外，仅在构建阶段临时挂载，不作为 COPY 输入。`deploy/maven/settings.xml` 仍作为用户主动选择的已有配置保留。

运行用户为 UID 10001，日志目录可写。健康检查要求接口请求成功且业务 `code=0`，HTTP 失败、HTML 或业务错误均不能视为健康。镜像测试只验证运行环境、打包配置和健康检查协议；数据库 / Redis / MinIO / 调度及 RTC 的完整容器联调在后续 Compose 验收完成。
