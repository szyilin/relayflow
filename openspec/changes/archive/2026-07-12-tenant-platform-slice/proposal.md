# 提案：租户平台层收尾（tenant-platform-slice）

## Why

[`tenant-ready-foundation`](../tenant-ready-foundation/proposal.md) 框架与 Flyway 已落地，但 §5（infra/im 租户维度、Redis/WS）与 §7（隔离测试）尚未在独立 change 中验收。本切片作为 `[平台]` 收尾，不新增用户可见 UI。

## What Changes

1. 验收 infra/im DO 继承 `TenantBaseDO`（codegen 已含 `tenant_id`）
2. 验收 Redis key 使用 `TenantRedisKeyBuilder`；WS fanout 按租户频道隔离
3. 默认租户 `id=1` 删除保护（`TenantService.assertDeletable`）
4. 框架层单元测试：TenantLineHandler、Redis key、WS Session 租户隔离
5. 完成后归档 `tenant-ready-foundation` 并同步 deployment/system 规格

## Impact

| 区域 | 变更 |
|------|------|
| `relayflow-framework` | 测试、WS fanout 校验 |
| `relayflow-module-system` | 默认租户保护 |
| `openspec/specs/` | 合并 tenant-ready delta |

## 非目标

- `relayflow.tenant.enabled=true` 产品能力
- 前端租户切换 UI
