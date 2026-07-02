# 部署规格增量

## 新增需求

### 需求：自托管单节点部署

系统应支持在单台服务器上通过 Docker Compose 部署 PostgreSQL、Redis、MinIO 与一个应用容器。

#### 场景：全新安装

- 给定 一台已安装 Docker 的空 Linux 服务器
- 当 运维执行 `docker compose -f deploy/compose.yml up -d`
- 那么 PostgreSQL、Redis、MinIO 与 RelayFlow 应用均成功启动
- 并且 不依赖外部 SaaS 服务

### 需求：离线（内网）配置

镜像与依赖预加载后，系统必须能在无互联网访问环境下运行。

#### 场景：离线启动

- 给定 所有容器镜像已在本地可用
- 当 应用在有效环境变量下启动
- 那么 各核心服务仅通过配置的主机名连接
- 并且 正常运行无需外网请求

### 需求：仓库根目录 Maven 模块化布局

仓库根目录应为 Maven 父工程；前端代码仅位于 `web/`。

#### 场景：仓库结构校验

- 给定 开发者克隆本仓库
- 当 查看根目录
- 那么 可见 `pom.xml` 与后端 Maven 模块
- 并且 前端资源仅存在于 `web/`

### 需求：单体应用入口

V1 应以名为 `relayflow-server` 的单一 Spring Boot 模块交付。

#### 场景：单 JAR 启动

- 给定 后端已通过 Maven 构建
- 当 运维运行 `relayflow-server` JAR
- 那么 system、infra、im 模块在同一进程中加载
- 并且 单一 HTTP 端口提供全部 REST 与 WebSocket 端点
