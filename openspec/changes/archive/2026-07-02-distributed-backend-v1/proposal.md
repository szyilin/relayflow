# 提案：架构演进 — 微服务就绪单体（V1）与分布式部署（Phase 2）

## Why

团队认同 **最终形态为分布式微服务**（独立扩缩容、技术展示、对齐 Spring Cloud 生态），但 **起步阶段** 不宜同时攻坚业务功能与 Nacos/Gateway/多进程运维。

业界共识（Fowler、Newman、Azure）：**先模块化单体、边界按限界上下文写好，再拆部署单元**。否则易高耦合单体或过早微服务成灾。

本变更将架构决策 **文档化、规格化**，明确 V1 与 Phase 2 的边界，避免实现时摇摆。

## What Changes

### V1（立即生效 — 开发按此执行）

- **运行时**：单一 `relayflow-server` 依赖各 `*-biz`
- **代码**：`relayflow-module-{system,infra,im}` 各 `*-api` + `*-biz`
- **跨域**：仅 `*-api`；禁止 `*-biz → *-biz`、禁止跨域直连表
- **数据库**：单库 + 表前缀分域；Flyway 在 `relayflow-server`
- **不引入**：Gateway、Nacos、独立 `*-server`（Phase 2）

### Phase 2（目标态 — 满足触发条件后再实现）

- Gateway + Nacos + `system-server` / `infra-server` / `im-server`
- OpenFeign 远程化已有 `*-api`
- 分库 + 各 `*-server` 独立 Flyway
- 更新 Compose 多服务编排

## Capabilities

### Modified Capabilities

- `deployment`：补充「微服务就绪模块化单体」要求；保留「单体应用入口」为 V1 真源；Phase 2 分布式要求作为增量规格（待 Phase 2 archive 时合并或独立 change）

## Impact

| 区域 | V1 | Phase 2 |
|------|-----|---------|
| `docs/dev/architecture.md` | 新增 | 补充 Phase 2 |
| `AGENTS.md`、`openspec/config.yaml`、工作流 | 更新 V1 单体约束 | Phase 2 再追加 gateway / *-server 脚手架 |
| Maven | 三域 api/biz + server | + gateway、*-server |
| `openspec/specs/deployment` | 合并 V1 增量 | 合并 Phase 2 增量 |

## Non-Goals

- V1 实现 Gateway / Nacos
- 为飞书侧边栏每项拆微服务
- 第三方工作台应用各建一个后端服务
