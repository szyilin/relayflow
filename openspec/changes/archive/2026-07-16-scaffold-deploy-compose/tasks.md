# 任务：scaffold-deploy-compose

## 1. Compose 文件

- [x] 1.1 创建 `deploy/compose.yml`（postgres、redis、minio + volumes + healthcheck）
- [x] 1.2 验证：`docker compose -f deploy/compose.yml config`

## 2. 环境变量模板

- [x] 2.1 创建 `deploy/.env.example`
- [x] 2.2 在 `README.md` 或 `docs/user/` 补充一行启动说明（若尚无）

## 3. 门禁

- [x] 3.1 本地试跑 `docker compose -f deploy/compose.yml up -d`（可选，有 Docker 时） — **关闭（路线重置，不再作为当前 backlog）**
- [x] 3.2 `openspec validate scaffold-deploy-compose --strict`
