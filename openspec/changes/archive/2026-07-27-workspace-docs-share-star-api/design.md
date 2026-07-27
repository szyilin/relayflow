## Context

Contract：`openspec/lanes/workspace-docs-share-star/contract.md`。母 design D1–D10。

## Goals / Non-Goals

**Goals:** 完整 REST + 表 + ACL + docs-bot；`mvn compile`。

**Non-Goals:** 改 `web/`；去掉前端 Mock（`-integrate`）。

## Decisions

1. Schema 并入本 `-api`（`V0.1.0.39`）
2. DO/Mapper 按 calendar_share 模式（BaseMapper + lambda）；复杂列表 Java 组装
3. `docs-biz` 增加 `im-api` 依赖
4. 保存 body：EDIT grantee 允许；乐观锁不再强制 `owner_user_id = me`

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 软删后 visit 墓碑 | object select null → DELETED；依赖 title_snapshot |
| 显示名 N+1 | 列表限 limit≤50；UserApi.getUserBasic best-effort |
