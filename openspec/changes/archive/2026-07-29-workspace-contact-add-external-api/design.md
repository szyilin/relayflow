## Context

前置：`workspace-contact-add-external-web`（UI + 草稿）与 enrich-api（`sys_contact_*` + list/accept/star）。本 change 仅后端补齐 search / create / reject。

契约真源：[`openspec/lanes/workspace-contacts-enrich/contract.md`](../../lanes/workspace-contacts-enrich/contract.md)「发起添加 / 拒绝」章。

## Goals / Non-Goals

**Goals:**

- `GET …/contact/search-account?mobile=` 或 `?email=`（确切匹配）
- `POST …/contact/request` 创建 pending（写入当前租户 `sys_contact_request`）
- `POST …/contact/request/{id}/reject`（仅收件人 + pending → rejected）
- 定稿 contract；看板 api → archived；`mvn compile` + curl

**Non-Goals:**

- 不改 `web/` / integrate
- 不新开联系人表（除非发现缺口）
- 扫码、短信、邮箱目录、Bot 推送
- 不改 accept / list / star 既有行为（除错误码对称扩展）

## Decisions

### D1. 搜索范围

按 `sys_user.mobile` / `sys_user.email` **全局确切匹配**（`deleted=0`）；同一标识多用户全部返回。`orgLabel`：优先取该用户任一 ACTIVE 租户名（可排除当前租户优先显示「其他企业」；V1 取第一条 ACTIVE 成员租户名即可）。排除当前登录用户自身。

### D2. alreadyAdded

对搜索结果：若当前用户在本租户已有 `sys_contact_external` 指向该 `userId`，或存在本租户 `from=me & to=target & status=pending`，则 `alreadyAdded=true`。

### D3. 创建申请

- 须有效组织成员；`targetUserId` 存在且 ≠ 自己
- 已是 external 或已有 pending → 业务错误（不幂等静默）
- 插入 `pending`；`subtitle` = message（可空串，截断 200）
- 响应可返回申请项形态（或最小 `{ id }`）；与 contract 对齐优先返回 `ContactRequestItem` 字段子集亦可——定稿：**返回创建后的 request 项**（from 视角可不进 list；list 仍只返回 to=me）

### D4. 拒绝

对称 accept：校验 to=me + pending → `rejected`；不写 external。新增/复用错误码 `CONTACT_REQUEST_INVALID`（不可拒绝）与 `NOT_FOUND`。

### D5. Flyway

默认无新迁移。若需 pending 唯一索引 `(tenant_id, from_user_id, to_user_id) WHERE status=pending AND deleted=0`，可用 `V0.1.0.44__…`；实现时若应用层校验足够可先不加。

### D6. 鉴权

与既有 contact enrich：JWT + 有效组织成员。无新 permission 码。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 跨租户用户隐私 | 仅确切手机/邮箱；不返回模糊列表 |
| 重复 pending | 创建前查重 + 可选唯一索引 |
| 搜索性能 | 确切等值查询；mobile/email 已有业务唯一性预期 |

## Migration Plan

1. 部署含新端点的 server
2. 前端 integrate 接 API
3. 回滚：回退 Java；表数据无害

## Open Questions

- （无）
