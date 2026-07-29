## Why

工作台缺少飞书式「头像旁 +」统一创建入口；通讯录目录虽已具备分类壳层，但「加人」闭环不完整（不能发起外部联系人申请；组织邀请仅管理端手机号一条路径）。同时现有组织邀请把「邀请」等同于 `sys_tenant_user.status=NOT_JOINED`，无法承载飞书式邀请设置（关/仅管理员/全员）、非管理员范围/有效期/审批，以及二维码、企业链接、邀请码、手机号等多种渠道。需要在一次母 change 中对齐产品入口，并对组织邀请域做可演进的重新设计（0.x 允许推倒表结构）。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。

### A. 工作台「+」创建枢纽（Create Hub）

在工作台 Rail / 资料区头像旁增加「+」菜单（对齐飞书图一子集），本期实现四项：

1. **创建群组** — 复用/增强既有 `POST /app-api/im/group/create`；打开成员选择器（组织内 / 外部 / 我管理的群组，见飞书图三–六）
2. **添加企业成员** — 语义为 **邀请加入当前企业**（非加好友）；按租户邀请设置决定入口可见性与能力
3. **添加外部联系人** — 按确切手机号或邮箱搜索账号；多企业同号全展示；发起申请（补齐现有仅 list/accept 的缺口）
4. **创建文档** — 进入云文档并创建 RICH_DOC（复用既有 library create）

非本期：录音转写、多维表格、问卷、会议室投屏、视频会议等。

### B. 组织邀请（重点 · 可推倒 redesign）

对齐飞书「组织架构 > 成员与部门 > 全局设置」：

| 设置项 | 行为 |
|--------|------|
| 邀请功能 | 关闭 / 开启仅管理员 / 开启全员（含非管理员） |
| 非管理员可邀请范围 | 邀请人所在部门 / 指定部门 |
| 非管理员邀请有效期 | 如 3 天后过期等 |
| 非管理员邀请审批 | 可选「需要管理员审批」 |

邀请方式（客户端「添加企业成员」弹窗，对齐飞书图二）：

- 企业二维码
- 企业链接
- 企业邀请码
- 手机号邀请

管理端入口放在 **成员与部门** 板块下（新建「邀请设置 / 全局设置」页或等价导航），与用户列表邀请并存但统一走新邀请域。

**BREAKING（数据模型）**：邀请真源从「仅 `sys_tenant_user.NOT_JOINED`」演进为独立邀请实体 + 租户邀请设置；`NOT_JOINED` 仍可作为加入结果态，但链接/码/QR/审批状态不以成员行为唯一真源。允许 0.x 破坏性迁移（须在 design 写清迁移与回滚）。

### C. 横切：通用成员选择器

创建群组（及后续加人场景）共用可选人面板：搜索 + 面包屑钻取组织内（含子部门「下级」）/ 外部（「外部」标签）/ 我管理的群组。

## Capabilities

### New Capabilities

- `workspace-create-hub`：工作台「+」菜单、四项创建入口、成员选择器壳层与导航规则
- `org-invite`：租户邀请设置、四种邀请渠道、邀请实体生命周期、审批与过期、管理端设置页与工作台邀请 UI

### Modified Capabilities

- `workspace-contacts-directory`：补齐「发起添加外部联系人」搜索与申请；空态与入口与 Create Hub 对齐
- `system`：成员邀请相关行为从「仅 admin 手机号 invite」扩展为设置驱动的多渠道邀请；既有 `POST /admin-api/system/user/invite` 纳入新域或兼容层
- `im`：创建群组入口可从 Create Hub 打开；成员选择器数据源规则（组织内/外部/我管理的群组）
- `docs`：Create Hub「创建文档」触发既有文档库创建路径（行为增量，非重做编辑器）
- `web-admin`：成员与部门下增加邀请设置导航与页面

## Impact

- **领域 / Maven**：`relayflow-module-system`（邀请设置与实体，主战场）、`relayflow-module-im`（建群 + 可选「我管理的群组」查询）、`relayflow-module-docs`（创建文档复用）、`relayflow-server`（Flyway）
- **前端**：`web/` 工作台 Rail/Create Hub、通讯录、消息建群、云文档；管理端 `system` 下邀请设置页
- **API**：新增 `/admin-api/system/invite-settings*`、`/app-api/system/org-invite/*`（或等价命名）、外部联系人 `POST request` + 搜索；既有 admin `user/invite` **BREAKING 或兼容适配**
- **DB**：新增 `sys_tenant_invite_settings`、`sys_org_invite`（及 token/码相关列或子表）；可能调整 `sys_tenant_user` 与邀请的衔接；0.x 允许推倒
- **自部署回滚**：Flyway 回滚需运维同意；关闭邀请功能即可停用客户端入口；旧 `NOT_JOINED` 行需迁移脚本保留可激活路径
- **不做**：短信通道（仍 deferred）、邮箱联系人目录、服务台、BPM 审批引擎（邀请审批用轻量状态机，不引 Flowable）
