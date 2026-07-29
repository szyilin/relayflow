## 1. Schema

- [x] 1.1 Flyway `V0.1.0.57__im_bot_owner_user.sql`：`im_bot.owner_user_id` + 从 `agent_def` / user enablement 回填
- [x] 1.2 更新 `ImBotDO` 字段 `ownerUserId`

## 2. Provisioning & catalog

- [x] 2.1 `ImBotProvisioningServiceImpl` 写入/更新 `ownerUserId`
- [x] 2.2 `listBotCatalog` 改为：当前用户 `owner_user_id` 的 carrier；排除 system
- [x] 2.3 `addBot`：carrier 校验 `owner_user_id == 当前用户`；更新错误码如需
- [x] 2.4 补充/调整相关单测

## 3. Frontend

- [x] 3.1 `ImAddGroupBotModal` 空态文案改为无可用智能体机器人；必要时展示描述字段
- [x] 3.2 `pnpm build && pnpm typecheck`；后端 `./mvnw -pl relayflow-server -am compile`
