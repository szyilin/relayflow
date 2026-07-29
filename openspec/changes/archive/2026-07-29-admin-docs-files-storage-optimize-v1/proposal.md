## Why

`admin-docs-product-settings-v1` 已交付企业文件管理与按人存储配额，但管理端体验与计量与飞书云文档后台差距过大：列表在姓名下展示裸 ID、按用户 ID 筛所有者、未复用既定「头像+姓名→名片」组件；存储上限是「默认 + 用户 ID 规则弹窗」，而非飞书式「用量详情 / 上限管理（租户默认 + 白名单优先级）」；文件大小依赖逐条 `FileApi` 解析，列表大量为 `—`，用量统计架构也不稳妥。需在飞书产品原图对齐下做一轮 UX + 计量架构优化，避免继续在错误模型上叠功能。

## What Changes

- **文件管理列表**：标题行不再展示 objectId；所有者列改用 `WorkspaceOwnerChip`（头像+显示名，点击 `WorkspaceBusinessCard`）；**禁止**姓名下挂裸 userId。
- **所有者筛选 / 转移目标**：改为姓名 / 手机号 / 邮箱（及现有成员搜索能力）选人，**禁止**以「所有者用户 ID」输入框为主路径（深链 `?ownerUserId=` 仍可内部保留）。
- **存储用量页**：对齐飞书「存储用量详情」——成员列头像+姓名、已用/上限/占比、操作「查看云文档」；搜索占位「请输入姓名、邮箱或手机号」。
- **存储上限管理**：**BREAKING（产品交互）** 从单页弹窗改为独立「上限管理」页（或等价全屏编辑态），对齐飞书：
  - **按租户设置**：不限制 / 每人最多 N GB；
  - **按白名单设置**：覆盖规则；优先级 **成员 > 部门**（用户组本版后置）；
  - **部门** Tab：按部门设上限（一部门一条）；
  - **上限提示设置** Tab：本版可占位或最小实现，详设见 design。
- **计量架构**：消除文件列表 N+1 `getFile`；列表/用量以可靠字节来源展示（批量 JOIN 或 `doc_object` 冗余 `size_bytes` + 写入路径维护）；明确 `RICH_DOC` 不计入配额、列表大小展示规则；为后续汇总缓存留扩展点。
- 更新 `openspec/lanes/admin-docs-product-settings/contract.md` 与管理端 UI 文案。

## Capabilities

### New Capabilities

- `docs-storage-metering`: 云文档对象大小解析、列表展示与按所有者用量汇总的计量架构（含写入维护与跨域仅经 `infra-api`）。
- `admin-docs-storage-quota-feishu`: 飞书对齐的存储用量详情 + 上限管理（租户默认、成员/部门白名单优先级、GB 单位编辑）。

### Modified Capabilities

- `workspace-owner-chip`: 适用范围扩展到管理端云文档「所有者/成员」列（仍用同一组件与名片交互；禁止自研纯文本 ID 展示）。
- `admin-product-settings`: 云文档产品壳层导航/文案与「文件管理 · 存储空间」入口行为随飞书信息架构微调（用量 vs 上限管理）。

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `/admin/products/docs/files`、`/storage`（及可能的 `/storage/quota`）；复用 `WorkspaceOwnerChip` / 成员搜索；store + `api/admin/docs*` |
| `relayflow-module-docs` | Admin files/storage VO、配额模型（部门规则表）、计量 Service/Mapper；上传触顶逻辑沿用并适配新生效规则 |
| `relayflow-module-system`（经 `*-api`） | 成员搜索（姓名/手机/邮箱）、部门信息展示；docs-biz **禁止**直查 `sys_` 表 |
| `relayflow-module-infra`（经 `*-api`） | 文件 size 批量查询或 docs 侧 JOIN 策略需符合跨域边界 |
| Flyway | 可能：`doc_object.size_bytes`、部门配额规则表、租户设置字段调整 |
| 迁移 / 回滚 | 新表/列可空或默认可回滚；旧「仅用户规则」数据迁入新白名单模型；交互 BREAKING 但 API 可兼容扩展字段 |

不改：`/admin/infra/storage`（对象存储配置）；工作台 `/app/docs` 主路径（除非 tasks 明确复用计量字段）。
