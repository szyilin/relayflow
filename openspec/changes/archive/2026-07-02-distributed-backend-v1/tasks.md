# 任务：distributed-backend-v1（架构演进文档与规格）

> **V1 实现仍从 `scaffold-maven-parent` 起**，本 change 以 **文档与 deployment 规格增量** 为主；Phase 2 脚手架在触发条件满足后另开或续本 change 第二章节。

## 1. 文档与规格（V1 架构决策）

- [x] 1.1 编写 `docs/dev/architecture.md`（两阶段架构）
- [x] 1.2 更新 `distributed-backend-v1` proposal / design / deployment spec delta
- [x] 1.3 运行 `openspec validate distributed-backend-v1 --strict`
- [x] 1.4 合并 deployment 规格至 `openspec/specs/`（微服务就绪单体等增量）
- [x] 1.5 同步 `AGENTS.md`、`openspec/config.yaml`、`implementation-workflow.mdc`、`README.md`、`docs/user/architecture-overview.md`、`docs/dev/code-style.md`、`docs/dev/database.md`、`docs/dev/README.md`

## 2. Phase 2 实现（勿在 V1 脚手架阶段提前做）

- [ ] 2.1 BOM 增加 Spring Cloud、Nacos（Phase 2 change 时）
- [ ] 2.2 `relayflow-spring-boot-starter-rpc`
- [ ] 2.3 `relayflow-gateway` + 三 `*-server` 空壳
- [ ] 2.4 Compose：Nacos + Gateway + 多服务
- [ ] 2.5 分库迁移与 Feign 切换
