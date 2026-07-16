# Tasks：im-bot-invite-migrate

> 验证：`openspec validate im-bot-invite-migrate --strict`；`./mvnw -pl relayflow-module-system/relayflow-module-system-biz -am test -Dtest=UserServiceImplInviteTest -Dsurefire.failIfNoSpecifiedTests=false`；`./mvnw -pl relayflow-server -am compile`

## 1. 规格与目录收口

- [x] 1.1 新 Flyway：停用 `invite-helper`；更新 `org-assistant` 描述含成员邀请
- [x] 1.2 同步修订母 change / 架构草案中「邀请 → invite-helper」表述为 `org-assistant`

## 2. 邀请产方

- [x] 2.1 `UserServiceImpl.inviteMember`：有 ACTIVE 企业时 `ImBotApi.send(org-assistant, ALL_ACTIVE_MEMBERSHIPS)`；无则跳过
- [x] 2.2 文案、`dedupeKey=MEMBER_INVITE:{tenantId}`、deep link 元数据
- [x] 2.3 更新 `UserServiceImplInviteTest`：校验 `ImBotApi.send` 参数；无 ACTIVE 时不调用 send

## 3. 验证

- [x] 3.1 `openspec validate im-bot-invite-migrate --strict`
- [x] 3.2 相关单元测试 + `./mvnw -pl relayflow-server -am compile` 通过
