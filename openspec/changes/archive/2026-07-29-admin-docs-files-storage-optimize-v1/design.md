## Context

`admin-docs-product-settings-v1`（A1/A2）已上线：

| 现状 | 问题 |
|------|------|
| 文件列表标题下挂 `objectId`、所有者下挂 `userId` | 管理端可读性差；与飞书/既有 UI 约定冲突 |
| 筛选项「所有者用户 ID」 | 管理员不会记 ID；飞书为姓名/邮箱/手机 |
| 所有者列纯文本 | 已有规范：`WorkspaceOwnerChip` → `WorkspaceBusinessCard`（`docs/dev/workspace-ui-patterns.md`；管理端角色页已复用） |
| 存储页：用量列表 +「配额设置」Modal | 飞书拆成「用量详情」与「上限管理」（租户默认 / 白名单 / 部门 / 提示） |
| 配额模型：仅 `default` + `user_rule` | 缺部门级规则与明确优先级文案 |
| `DocAdminFilesServiceImpl.resolveFileSizes` 逐条 `fileApi.getFile` | 慢且易空 → 列表「大小」常为 `—` |
| `DocObjectExtMapper` `JOIN infra_file` | **跨域 Mapper 直查他域表**，违反 architecture |

飞书参考（产品原图）：用量详情（头像+姓名、部门、已用、上限、占比、查看云文档）；上限管理 Tabs：成员（租户默认 + 白名单，优先级成员>用户组>部门）/ 部门 / 上限提示。

约束：跨域仅 `*-api`；前端优先切片；管理端 Nuxt UI；不改 `/admin/infra/storage`。

## Goals / Non-Goals

**Goals:**

1. 文件管理与存储用量的人身份展示/搜索对齐飞书与 `WorkspaceOwnerChip` 规范。
2. 存储上限管理对齐飞书信息架构与规则优先级（本版：成员 + 部门；用户组后置）。
3. 修正计量：`FILE` 大小可稳定展示与汇总；去掉 docs→`infra_file` Mapper JOIN；写入路径维护权威字节。
4. Contract / API / UI 可分期交付（W1 UX+计量 → W2 上限页 → W3 部门）。

**Non-Goals:**

- 用户组白名单、导出用量 CSV、跨产品（IM 附件等）总存储。
- `RICH_DOC` 正文计入配额（仍不计）。
- 重做工作台 `/app/docs` IA。
- 对象存储提供方配置页。

## Decisions

### D1 — 所有者展示：统一 `WorkspaceOwnerChip`

- **决策**：管理端文件列表「所有者」、存储用量「成员」、白名单已选成员，一律 `WorkspaceOwnerChip`（`userId` + `displayName` + 可选 `avatarUrl`）。禁止姓名下挂裸 ID；objectId 不作为标题副文案（需要时放详情/复制或 tooltip）。
- **备选**：自研 AdminUserCell → 拒绝（与规范重复，且角色页已证明可复用）。

### D2 — 人选与搜索：成员搜索，非 ID 输入

- **决策**：
  - 文件页所有者筛选：成员搜索（姓名/手机/邮箱）→ 选定后以 `ownerUserId` 调列表 API；深链 `?ownerUserId=` 仍支持。
  - 转移/恢复指定新主：同模式（可复用工作台 `WorkspaceMemberPicker` 的管理端适配，或轻量 `UInputMenu` + admin/app 成员搜索 API）。
  - 存储用量 `keyword`：后端 MUST 按昵称/用户名/手机/邮箱匹配（经 system `MemberUserApi` / 管理端等价搜索），禁止仅精确 userId。
- **备选**：继续 ID 框 + 旁注「可搜姓名」→ 拒绝。

### D3 — 飞书式上限信息架构

```text
/admin/products/docs/storage          → 存储用量详情（列表）
/admin/products/docs/storage/quota    → 存储上限管理（编辑页）
     Tabs: 成员 | 部门 | 上限提示（占位可）
```

**成员 Tab：**

- 按租户：不限制 / 每人最多 N **GB**（UI 单位 GB；API 仍存 bytes）。
- 按白名单：最多约 20 条；每条选成员 + 不限制 / 最多 N GB。
- 文案优先级：**成员 > 部门**（用户组后置时写明「本版无用户组」）。

**部门 Tab：**

- 一部门一条上限；不限制 / 每部门最多 N GB（语义：该部门成员若无成员级覆盖时生效——见 D4）。

**上限提示：** 本 change 可占位「后续版本」；不阻塞 W1/W2。

- **备选**：继续 Modal → 拒绝（与飞书心智不符，规则变复杂后不可用）。

### D4 — 生效配额优先级

```text
effective(user) =
  user_rule(user)           if exists
  else dept_rule(primary_dept) if exists   -- 多部门时取主部门；无主部门则不套部门规则
  else tenant_default
  else unlimited
```

用户组规则：**后置**（表结构可预留 `subject_type`，本版只写 USER / DEPT）。

触顶仍在 Drive `registerFile`：`DOC_STORAGE_QUOTA_EXCEEDED`。

### D5 — 计量架构（核心）

**问题根因：**

1. 列表对每行 FILE 调 `FileApi.getFile` → 失败/空则 `sizeBytes=null` → UI `—`。
2. 用量 SQL `JOIN infra_file` → 跨域违例，且与列表路径不一致。

**目标态：**

```text
doc_object.size_bytes BIGINT NULL
  - FILE：注册/确认成功时写入 = infra 文件 size；变更绑定则更新
  - RICH_DOC / 无对象存储：NULL（列表展示 —；不计入配额）

用量汇总：SUM(size_bytes) WHERE type=FILE AND lifecycle=ACTIVE AND deleted=0
  （仅查 doc_ 表，无 JOIN infra_）

回填：一次性 Job/迁移脚本经 FileApi 批量读 size 写回 size_bytes（可管理端触发或 Flyway 后应用启动任务，详 tasks）
```

**FileApi：** 若尚无批量接口，本 change 在 `infra-api` 增加 `getFiles(Collection<Long> ids)`（或等价），供回填与校验；**稳态路径不依赖列表热路径调 FileApi**。

- **备选 A**：仅批量 FileApi、不落库 → 仍跨域热路径、分页成本高。
- **备选 B**：docs Mapper 继续 JOIN infra_file → 违规，拒绝。

### D6 — 列表 size 展示规则

| type | sizeBytes | UI |
|------|-----------|-----|
| FILE 且已回填 | >0 或 0 | 格式化 B/KB/MB |
| FILE 未回填 | null | `—`（回填后消失） |
| RICH_DOC | null | `—`（产品说明：文档正文不计容量） |

### D7 — 实现分期（前端优先）

| Wave | 内容 | 切片建议 |
|------|------|----------|
| W1 | 文件页 UX（chip、搜人、去 ID）+ 计量（`size_bytes` + 回填 + 去 JOIN）+ 用量搜人/chip | `…-metering-web/api` + files UX 同切片或紧随 |
| W2 | 用量页飞书化 + 上限管理页（租户 + 成员白名单，GB） | `…-quota-ux-web/api/integrate` |
| W3 | 部门规则表 + 优先级 + 部门 Tab | `…-dept-quota-*` |
| W4（可选） | 上限提示占位→真实现；用户组 | 另开 change |

母 change 本目录只定目标与 tasks 勾选顺序；实现时一次只做一个 wave 的 tasks 组。

### D8 — API 演进（摘要）

- `GET .../files`：响应增加 `ownerAvatarUrl`（可选）；`sizeBytes` 来自 `doc_object`；筛选仍 `ownerUserId`，另可加 `ownerKeyword` 由服务端解析为 userIds（或前端先 search 再传 id——**推荐前端选人传 id**，简单且与深链一致）。
- `GET .../storage-usage`：增加 `avatarUrl`、`deptName`（经 system-api）；keyword 含邮箱。
- `GET/PUT .../storage-quota`：扩展 `deptRules[]`；`defaultQuotaBytes`；UI 用 GB 换算；白名单上限条数校验（如 ≤20）。
- 新路由仅前端：`/storage/quota`；后端可仍一套 quota API。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 历史 FILE `size_bytes` 为空 | 回填任务 + 列表可短暂 `—`；用量在回填前偏低须在 release note 说明 |
| 部门主部门缺失 | 无主部门则跳过部门规则，回落到租户默认 |
| `WorkspaceOwnerChip` 依赖 app 名片 API | 管理端已用于角色页；缺权限时降级为只读 chip 仍展示名 |
| 配额 PUT 全量替换与部门规则并存 | contract 写清全量语义；W3 前后兼容字段 |
| 一次做完三 Tab 过大 | 严格按 Wave；W4 另 change |

## Migration Plan

1. Flyway：`doc_object.size_bytes`；`doc_storage_quota_dept_rule`（W3）；权限码不变或微调。
2. 部署后跑回填（或 integrate 验证脚本）。
3. 删除 ExtMapper 中 `infra_file` JOIN；改 SUM `size_bytes`。
4. 前端发版：先 W1 不破坏旧 quota API；再 W2 换路由；W3 加 deptRules。
5. 回滚：新列可空；旧 API 字段保留；新路由可临时链回 Modal（不推荐长期）。

离线/内网：无外部依赖；回填仅打本机 `infra`/`docs`。

## Open Questions

1. 多部门成员：是否强制「主部门」字段已存在？若否，W3 用「任一部门命中取更严上限」还是「仅主部门」？（**建议仅主部门**，与飞书简化对齐。）
2. 上限提示 Tab：本 change 占位是否足够，还是要最小「邮件/Bot 提醒」？（**建议占位**。）
3. 白名单硬顶 20：是否与飞书一致写死？（**建议 20，contract 注明。**）
