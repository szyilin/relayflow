## 1. 契约与看板

- [x] 1.1 定稿 `openspec/lanes/org-invite/contract.md` channels 章（capability / shared / mobile / consume + curl）
- [x] 1.2 看板 `org-invite-channels`：api → in_progress → 完成后 archived

## 2. 表与生成

- [x] 2.1 Flyway `V0.1.0.42__sys_org_invite.sql`
- [x] 2.2（可选）`V0.1.0.43__migrate_not_joined_to_org_invite.sql`
- [x] 2.3 codegen `sys_org_invite` → diff 合入 system-biz

## 3. 领域与 API

- [x] 3.1 `OrgInviteService`：capability、shared ensure/reset、mobile create、consume + 设置/权限校验
- [x] 3.2 `AppOrgInviteController`：`/app-api/system/org-invite/*`
- [x] 3.3 consume 写 ACTIVE membership（含已成员幂等）；必要时 `ImBotApi.ensureUserEnablementsOnActive`

## 4. 验证

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 curl：capability / shared / reset / mobile / consume
- [x] 4.3 `openspec validate org-invite-channels-api --strict`；看板 archived
