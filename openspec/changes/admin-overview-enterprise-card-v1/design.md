## Context

管理端 `/admin` 概览「概览统计」仍为 Empty 占位。企业信息已有 profile/address API；`sys_tenant.code` 唯一索引已存在，但种子租户写死 `default`，新建租户用 `t_`+UUID。企业头像在 `admin-enterprise-info-v1` 中标为后置可选，尚未落地。

约束：V1 单体；前端优先；跨域不直连他域表；头像复用 infra 公开文件 fileId；管理端路由 `/admin` 前缀。

## Goals / Non-Goals

**Goals:**

1. 概览企业身份卡：头像、名称、编号 + 四指标（人数/部门/超管/子管）可跳转。
2. 企业编号：飞书风格 11 位大写字母数字，全局唯一，创建后不可改；迁移种子 `default`。
3. 企业头像：`logo_file_id` + 企业信息页上传 + 概览/档案展示。
4. 概览统计 API 一次返回摘要与计数。

**Non-Goals:**

- 企业认证流程、扩容营销链、数据驻留多地选择/编辑
- 改用户个人头像链路
- Gateway / 独立 overview 微服务

## Decisions

### D1. 企业编号格式

- **选择**：11 位 `[A-Z0-9]`（排除易混 `0/O/1/I`），`SecureRandom` 生成；冲突重试；DB `uk_sys_tenant_code` 兜底。
- **相对**：保留 `t_`+UUID（唯一但观感不像身份证）；雪花 ID 当编号（暴露内部发号）。
- **迁移**：Flyway 将 `code='default'` 更新为生成值（脚本内固定一枚稳定演示码，如 `RFDEMO7K9X2`，避免每次 migrate 变化）；新建租户走运行时生成。

### D2. 企业头像

- 列：`sys_tenant.logo_file_id BIGINT NULL`（infra `infra_file.id`）。
- Profile GET/PUT 增加 `logoFileId`（string | null）；PUT 仅接受本租户可读的 fileId（与用户头像同样：公开文件 id）。
- 前端：管理端 `upload-session` + `confirm`（`api/admin/file`）→ `PUT profile`；展示用 `resolveAvatarUrl`。

### D3. 概览统计 API

- `GET /admin-api/system/enterprise/overview`（权限 `system:enterprise:query`；概览页亦可用，与企业信息同码，避免新权限扩散）。
- 返回：`code, name, logoFileId, memberCount, deptCount, superAdminCount, subAdminCount`。
- 计数：
  - `memberCount`：`sys_tenant_user.status=ACTIVE`
  - `deptCount`：当前租户未删部门
  - `superAdminCount`：绑定 `code=super_admin` 角色的去重用户
  - `subAdminCount`：绑定 `role_type=CUSTOM` 角色的去重用户（不含仅超管）

### D4. 前端跳转

| 指标 | 路由 |
|------|------|
| 组织总人数 | `/admin/system/user` |
| 部门数 | `/admin/system/dept` |
| 超级管理员 / 子管理员 | `/admin/system/role` |

### D5. 数据驻留地

保留只读「中国大陆」；不入库、不编辑。产品说明：V1 无多地部署选择，字段仅为合规展示对标。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 外部文档/脚本依赖 `code=default` | 仓库内仅种子 SQL 历史保留；运行时无硬编码 |
| 编号碰撞 | 重试 + unique index |
| 概览权限过严导致无企业权限管理员看不到卡 | 与企业信息共用 query；后续若需可再拆 overview 权限 |

## Migration Plan

1. Flyway：`logo_file_id` + `UPDATE code WHERE code='default'`。
2. 发布后端 API；前端接 overview + avatar。
3. 回滚：不建议回写 `default`；可保留列置空 logo。

## Open Questions

无（数据驻留地本切片保留只读）。
