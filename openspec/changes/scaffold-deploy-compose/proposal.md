# 提案：Docker Compose 基础设施（scaffold-deploy-compose）

## Why

后端与 Flyway 需要 PostgreSQL、Redis、MinIO。本 change 提供本地与自部署用的 Compose 编排，**不涉及 Java 代码**。

## What Changes

- 新增 `deploy/compose.yml`（PostgreSQL 16、Redis 7、MinIO）
- 新增 `deploy/.env.example`
- 可选：`db/` 占位说明（Flyway 在 server 资源目录，非 db/）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无 — deployment 规格已描述 Compose 能力，本 change 为实现）

## Impact

| 区域 | 影响 |
|------|------|
| `deploy/` | 新建 |
| Java 模块 | 无 |

**可与 `scaffold-server` 并行**，但联调需 server 已存在。
