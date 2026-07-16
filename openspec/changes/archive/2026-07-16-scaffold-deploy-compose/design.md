# 设计：scaffold-deploy-compose

## 服务

| 服务 | 镜像 | 端口 |
|------|------|------|
| postgres | postgres:16 | 5432 |
| redis | redis:7 | 6379 |
| minio | minio/minio | 9000, 9001 |

## 验证

```bash
docker compose -f deploy/compose.yml config
docker compose -f deploy/compose.yml up -d
docker compose -f deploy/compose.yml down
```

## 环境变量

见 `deploy/.env.example`：`POSTGRES_*`、`REDIS_*`、`MINIO_*`。
