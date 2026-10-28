> **执行范围**：本 change 为 [平台]；用户已授权将剩余任务按依赖顺序全部实现。各阶段依据实际验证结果勾选，不把规划产物齐备或单阶段通过视作全部实现完成。

## 1. 本地配置与存储地址

- [x] 1.1 [平台] 实现本地配置生成 / 校验工具和非敏感模板：明确 env / 项目来源，工具容器生成随机密钥，原子写入及并发保护，已有配置不覆盖，旧卷缺配置时拒绝新建；补全 Git / Docker 忽略规则。验证首次生成、复用、损坏配置、旧卷保护、失败不留半文件及并发场景，不要求本机 Java / Node / OpenSSL。
- [x] 1.2 [平台] 实现 OSS 运行时 internal / public endpoint 与 region 解析，覆盖 Bootstrap、匹配受管地址的租户 / inline 配置及历史下载，独立存储不改；验证签名前选择地址和旧单地址兼容，运行受影响测试及 Maven compile。

## 2. 初始化、镜像与完整编排

- [x] 2.1 [平台] 增加 MinIO 建桶 / CORS 初始化和 local 专用 XXL 首次初始化标记；验证失败重试、重复执行不覆盖对象及调度 Cron / 启停，保留旧开发入口策略。
- [x] 2.2 [平台] 完成后端 `local-docker` profile / 镜像构建适配与配置注入契约，正确设置内部依赖、LiveKit API URL 和执行器回调；验证 Maven compile、镜像构建与 env 映射，不加载 dev 默认值。
- [x] 2.3 [平台] 增加 `web/` 多阶段构建和 Nginx 静态服务 / REST / WS / SSE 代理；验证 frozen lockfile 构建、`pnpm build` + `pnpm typecheck`、深链刷新及 API 不回退 HTML。
- [x] 2.4 [平台] 新增 `deploy/compose.local.yml`，整合初始化依赖、固定版本镜像、独立项目卷和端口；验证 local 与原开发 Compose 配置、首次启动服务健康、受管 MinIO 浏览器直传及本机 LiveKit 连接，记录 amd64 / arm64 镜像可用性。

## 3. 启动入口与验收

- [x] 3.1 [平台] 完成 `start-local.sh` / `stop-local.sh`：前置检查、镜像准备、有界就绪、`--no-build`、失败脱敏诊断及成功输出；验证端口冲突、再次运行、本项目占用不误报、停止保留 env / 卷和缺镜像反馈。
- [x] 3.2 [平台] 增加隔离 `smoke-local.sh` 与协议夹具，验证空环境登录、文件 PUT / confirm / GET、WebSocket、再次启动及停止恢复；浏览器验证 `/app/login`、受保护深链、SSE 分段和同机两标签页真实媒体，记录证据与平台限制。

## 4. 文档与持续验证

- [x] 4.1 [平台] 更新 README / 部署说明：默认完整体验与宿主开发两入口、固定镜像版本、配置备份、日志 / 停止、首次网络依赖及离线复用边界；核对文档命令与实际入口一致。
- [x] 4.2 [平台] 加入独立项目的冷启动 / 恢复 CI 和脱敏诊断归档，运行本 change strict validate、受影响测试、Maven compile、前端 build / typecheck 与冒烟；既有无关测试或 lint 问题单独记录，不忽略失败后宣称通过。
