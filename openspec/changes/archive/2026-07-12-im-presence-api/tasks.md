# 任务：在线状态 API（im-presence-api）

> **Lane**：`*-api` · 契约 [`contract.md`](../../lanes/im-presence/contract.md)

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`

## 2. im-biz

- [x] 2.1 `ImPresenceService` + `ImPresenceController`
- [x] 2.2 租户成员校验（`TenantMemberApi.filterActiveMemberUserIds`）
- [x] 2.3 V1 MVP：REST batch + `RealtimeTransportApi.isUserOnline`（WS push 留后续）

## 3. 验证

- [x] 3.1 curl batch；双浏览器在线/离线对比
- [x] 3.2 `./mvnw -pl relayflow-server -am compile`
- [x] 3.3 `openspec validate im-presence-api --strict`
- [x] 3.4 看板 API → ready

## 下一 change

- `im-presence-integrate`
