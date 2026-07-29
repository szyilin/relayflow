## Why

管理端「概览」仍是空占位，缺少飞书式企业身份卡与可跳转组织指标；企业编号种子值为可读单词 `default`，不符合「全局唯一、身份证式、不可编辑」的产品预期；企业头像尚未落地，概览与企业信息页无法展示/更换 Logo。

## What Changes

- 管理端概览「概览统计」改为企业身份卡：头像、名称、企业编号；四个可点击指标（组织总人数 → 用户管理、部门数 → 部门管理、超级管理员 / 子管理员 → 管理员角色）。
- 新增概览统计 API，返回企业摘要 + 四项计数。
- 企业编号：新建租户生成飞书风格大写字母数字串（全局唯一）；**BREAKING（数据）** 将现有种子 `code=default` 迁移为同风格唯一编号；创建后仍不可编辑。
- 企业头像：`sys_tenant.logo_file_id`；企业信息页可上传/更换；概览与档案共用。
- 「数据驻留地」：V1 保留只读展示「中国大陆」（无真实多地能力，非可配置字段）；本切片不删不改语义。

## Capabilities

### New Capabilities

- `admin-overview`: 管理端概览企业身份卡与组织统计指标（含跳转约定）

### Modified Capabilities

- `system`: 企业编号生成/种子迁移；企业头像字段与 profile API；概览统计查询
- `web-admin`: 概览页统计卡；企业信息页头像上传

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-system` | 编号生成、Flyway（logo + 迁移 default）、overview/profile API |
| `relayflow-server` Flyway | `V0.1.0.47__…` |
| `web/` | `/admin` 概览卡、企业信息头像、`api/admin` + store |
| 自部署 | 已有库执行迁移后 `default` 编号变更；回滚需手工还原 code（不推荐） |
| 依赖 | 无新 Maven/npm 依赖 |
