## 1. 表与生成

- [ ] 1.1 Flyway 新增 `sys_user_preference`（`tenant_id`+`user_id` 唯一，`settings` JSONB，`schema_version`，公共字段）
- [ ] 1.2 `./scripts/codegen.sh` 按需生成 DO/Mapper，diff 合入 system-biz

## 2. 领域与 API

- [ ] 2.1 代码默认真源（`general.themeMode` / `themeColor` / `im.chatBubbleLayout` 等）与 deepMerge
- [ ] 2.2 Service：GET 合并；PUT upsert；未知键忽略或校验（按 design）
- [ ] 2.3 Controller：`GET`/`PUT /app-api/system/user/preference`；仅当前用户+租户
- [ ] 2.4 完善 `openspec/lanes/user-preference/contract.md`（若 web 已起草则对齐）

## 3. 验证

- [ ] 3.1 `./mvnw -pl relayflow-server -am compile`
- [ ] 3.2 `openspec validate user-preference-api --strict`
