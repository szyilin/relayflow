## Context

前置：`workspace-contacts-enrich-web`（UI + Mock）与 `workspace-contacts-enrich-api`（表 + REST）均已就绪。本 change 为 **`-integrate`**：前端接真 API、删临时数据、看板 done、浏览器走查。

契约真源：[`openspec/lanes/workspace-contacts-enrich/contract.md`](../../lanes/workspace-contacts-enrich/contract.md)。

## Goals / Non-Goals

**Goals:**

- API 客户端与 store 对齐 contract 字段
- 打开「外部 / 新的联系人 / 星标」时拉取列表；accept / star 写操作后刷新
- 删除 seed 与「仅改内存」的同意/星标真源
- 租户切换时清空或重拉 enrich 数据（沿用既有 `resetForTenantSwitch` 模式）
- 看板 Web=`done`；`pnpm build` + `pnpm typecheck`；浏览器验收

**Non-Goals:**

- 不新开「发起加好友」UI
- 不改组织内 / 部门钻取逻辑
- 不引入拼音库
- 不改后端表结构（缺口另开 `-api` 修补 change）

## Decisions

### D1. API 模块位置

在 `web/src/api/app/` 扩展现有 `contacts.ts` 或新建 `contactEnrich.ts`：优先与现有 `getDeptTree` 同文件/同目录，避免重复 auth 封装；类型与 store 接口对齐（可共享类型）。

### D2. 加载时机

- 进入对应 `category` 时拉取（或首次进入时拉取并缓存到 store）
- `accept` / `PUT starred` 成功后：刷新申请列表 + 外部列表（accept）；刷新星标列表（star）
- 失败：`lastError` + toast，不静默吞掉

### D3. 空列表与验收数据

无 seed 后空列表合法。联调前按 `-api` tasks 的 SQL 样例插入 pending 申请等，或使用已有测试账号数据。

### D4. 我的群组

仅 `useImStore` 中 `type === 'group'`；删除任何「不足时 store 临时群」真源（若 `-web` 仍残留）。

### D5. 看板与 contract

- 看板：`workspace-contacts-enrich` → API archived/ready + Web **done**
- contract 头注释改为 integrate 完成；「临时数据」段落改为历史说明或删除

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| API 未起或迁移未跑 | tasks 写明先起 server；失败可见错误 |
| 字段名漂移 | 以 contract 为准；发现不一致记入联调笔记并修一端 |
| 星标与名片入口双写 | V1 仅通讯录星标 API；名片若另有星标另开 change |

## Migration Plan

1. 部署含 enrich API 的后端并跑 Flyway
2. 发布前端 integrate
3. 回滚：回退前端提交；用户关系数据仍留在 DB 无害

## Open Questions

- （无）星标是否从名片入口同步：本切片不处理。
