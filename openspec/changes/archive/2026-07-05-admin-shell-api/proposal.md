# 提案：管理端壳层 · 后端 Lane（admin-shell-api）

## 背景

`tenant-ready-foundation` 已提供 `GET /admin-api/system/tenant/default`。本 change 为 **并行 Lane 之后端部分**，确保接口在 dev/联调环境下可用，并与共享契约一致。

## 并行结构

| Change | Lane |
|--------|------|
| **admin-shell-api**（本 change） | 后端 |
| `admin-shell-web` | 前端 |
| `admin-shell-integrate` | 联调与门禁 |

契约真源：[`openspec/lanes/admin-shell/contract.md`](../../../lanes/admin-shell/contract.md)

## 前置条件

- [x] `admin-login-slice` 已完成（JWT 登录可用）
- [x] `openspec/lanes/admin-shell/contract.md` 已冻结

## 范围

| 模块 | 内容 |
|------|------|
| `relayflow-module-system` | 确认 `TenantController` `/default` 行为与 VO 字段 |
| `relayflow-framework` | 确认 Security 对 `/default` 的 permitAll / CORS |
| `web/` | **不修改**（归属 `-web`） |

## 非目标

- 新增 logout API
- Flyway / 新表
- 多租户切换

## 影响域

- OpenSpec：`system`（租户查询确认）
- Maven：按需微调 Security，默认无新业务代码
