## Context

`-web` 已交付 UI；契约草案在 `openspec/lanes/workspace-business-card/contract.md`。`avatar` 已用 `VARCHAR` 存 fileId，封面沿用同一模式。

## Goals / Non-Goals

**Goals:** 签名/封面持久化；联系人备注 upsert；前端去 localStorage；他人可读公开资料。

**Non-Goals:** 语音/视频真实通话；备注图片附件；合表（本切片新建 `sys_contact_remark`，若日后合并须先征得同意）。

## Decisions

### D1. 签名/封面在 `sys_user`

公开资料字段跟用户走，不另起 preference 行。`cover_file_id` 与 `avatar` 同为 fileId 字符串。

### D2. 备注独立表 `sys_contact_remark`

`(tenant_id, owner_user_id, target_user_id)` 唯一；C 类无行即空。

### D3. 0.x 无旧数据迁移

丢弃 web 阶段 localStorage；不写回填脚本。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 日后合表 | database.md 政策：先征得同意 |
| 封面大图 | 沿用 public 上传与体积限制 |

## Migration Plan

`V0.1.0.20__user_profile_card.sql`：ALTER + CREATE。
