## 1. Data & tenant code

- [x] 1.1 Flyway：`sys_tenant.logo_file_id`；将 `code='default'` 迁移为固定演示编号（如 `RFDEMO7K9X2`）
- [x] 1.2 抽出共享 `TenantCodeGenerator`（11 位无歧义大写字母数字）；注册创建企业改用该生成器并冲突重试
- [x] 1.3 DO / profile VO 增加 `logoFileId`；updateProfile 可写 logo（忽略 code）

## 2. Overview API

- [x] 2.1 实现 `GET /admin-api/system/enterprise/overview`（成员/部门/超管/子管计数 + 企业摘要）
- [x] 2.2 `./mvnw -pl relayflow-server -am compile` 通过

## 3. Frontend enterprise avatar + overview card

- [x] 3.1 `api/admin/enterprise` 扩展 profile `logoFileId` + `getEnterpriseOverview`；store 支持上传头像与加载 overview
- [x] 3.2 企业信息页基础信息区可上传/展示企业头像
- [x] 3.3 `/admin` 概览统计卡：头像/名称/编号 + 四指标可跳转
- [x] 3.4 `cd web && pnpm build && pnpm typecheck` 通过

## 4. Docs / contract

- [x] 4.1 更新 `openspec/lanes/admin-enterprise-info/contract.md`（logo + overview）
- [x] 4.2 `openspec validate admin-overview-enterprise-card-v1 --strict`
