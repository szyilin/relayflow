## Context

- API 已就绪：`GET …/search-account`、`POST …/request`、`POST …/request/{id}/reject`
- `contactAdd` 曾用 `buildDraftHits`；`api/app/contacts.ts` search/create 与 `contacts.rejectRequest` 已接 API（本切片去草稿）

## Goals / Non-Goals

**Goals:** store + api 客户端接真接口；删除草稿；看板 done。

**Non-Goals:** 改搜索规则、短信、管理端；重做 Modal 布局。

## Decisions

1. **ID 规范化**：`userId` / request `id` 一律 `String(...)`（与现有 contacts API 一致）。
2. **outgoing**：仍用 create 成功后的本地摘要列表（API list 仅「指向我」）；不新增 outgoing list API。
3. **错误**：`ApiError.message` 写入 `lastError`；空搜结果不视为错误。

## Risks

| 风险 | 缓解 |
|------|------|
| 无测试账号搜不到 | 验收用已知 mobile/email；空态文案保留 |
