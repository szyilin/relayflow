## 1. 契约与看板

- [x] 1.1 定稿 `openspec/lanes/org-invite/contract.md` settings 章（去掉「仅 store」；标注本 `-api` 已实现 GET/PUT）
- [x] 1.2 看板 `org-invite-settings`：api → in_progress（完成实现后 → archived）

## 2. 表与生成

- [x] 2.1 Flyway `V0.1.0.41__sys_tenant_invite_settings.sql`（或顺延序号）
- [x] 2.2 `./scripts/codegen.sh --module system --tables sys_tenant_invite_settings` → 临时目录 → diff 合入 system-biz DO/Mapper

## 3. 领域与 API

- [x] 3.1 Service：默认常量、GET 合并、PUT 校验 + upsert（当前租户）
- [x] 3.2 Admin Controller：`GET`/`PUT /admin-api/system/invite-settings` + `@PreAuthorize`（list / create）
- [x] 3.3（可选）system-api 暴露只读 `InviteSettingsApi` 供后续 channels 同域 Service 使用；无跨域则可不做 — **跳过**（同域 Service 足够）

## 4. 验证

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 curl GET/PUT（admin JWT）对照 contract
- [x] 4.3 看板 api → archived；`openspec validate org-invite-settings-api --strict`
