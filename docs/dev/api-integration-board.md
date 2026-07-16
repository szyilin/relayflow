# API 对接看板

> **前端 AI / 开发者第一入口**：查各切片 **UI 与 API 进度**。  
> **默认顺序（前端优先）**：`-web`（UI+Mock+contract 草案）→ `-api` → `-integrate`。见 [`frontend-first-workflow.md`](frontend-first-workflow.md)。

行为真源（归档后）：[`openspec/specs/`](../../openspec/specs/) · 切片契约（永久）：[`openspec/lanes/`](../../openspec/lanes/)

## 状态说明

| API 状态 | 含义 |
|----------|------|
| `planned` | 契约草案或后端未实现 |
| `ready` | 后端已实现、curl 通过 |
| `archived` | `-api` change 已归档 |

| Web 状态 | 含义 |
|----------|------|
| `pending` | UI 未开始 |
| `in_progress` | `-web` 进行中 |
| `ui_ready` | UI + contract 完成，待 `-api` |
| `done` | integrate 通过，store 无 Mock |

## 当前切片

| 切片 | API 状态 | Web 状态 | 端点 / 页面 | 契约 | 说明 |
|------|----------|----------|-------------|------|------|
| 统一登录 | archived | **done** | `POST …/auth/login` · `/app/login` | — | store 无 Mock |
| admin-shell | archived | **done** | `GET …/tenant/default` | [contract](../../openspec/lanes/admin-shell/contract.md) | store 无 Mock |
| admin-user-list | **ready** | **done** | `GET …/user/page` · `/admin/system/user` | [contract](../../openspec/lanes/admin-user-list/contract.md) | store 无 Mock |
| system-rbac-kernel | **ready** | **done** | `GET …/auth/get-permission-info` · nav 权限过滤 | [contract](../../openspec/lanes/system-rbac-kernel/contract.md) | store 无 Mock；sidebar 按 permission 过滤 |
| admin-dept | **ready** | **done** | `GET/POST/PUT/DELETE …/dept/*` · `/admin/system/dept` | [contract](../../openspec/lanes/admin-dept/contract.md) | store 无 Mock；部门树 CRUD |
| admin-role | **ready** | **done** | `GET/POST/PUT/DELETE …/role/*` · `/admin/system/role` | [contract](../../openspec/lanes/admin-role/contract.md) | store 无 Mock；角色 CRUD + 绑权限/数据范围 |
| admin-user-mutate | **ready** | **done** | user create/update · `/admin/system/user` | [contract](../../openspec/lanes/admin-user-mutate/contract.md) | store 无 Mock；新建 + 编辑 + 启停 |
| infra-storage-config | **archived** | **done** | `GET/PUT/DELETE …/storage/config` · `/admin/infra/storage` | [contract](../../openspec/lanes/infra-storage-config/contract.md) | store 无 Mock；MinIO 配置 + 测试连接 |
| infra-file | **archived** | **done** | 列表/上传/删除/下载 · `/admin/infra/file` | [contract](../../openspec/lanes/infra-file/contract.md) | Presigned 直传 + 302 下载；⑧ integrate 完成 |
| im-direct-chat | **ready** | **done** | `GET/POST /app-api/im/*` · `/app/messages` | [contract](../../openspec/lanes/im-direct-chat/contract.md) | store 无 Mock；REST + WS `message.new` |
| admin-user-by-dept | **ready** | **done** | `GET …/user/page?deptId=` · `/admin/system/user` | [contract](../../openspec/lanes/admin-user-by-dept/contract.md) | 左树右表；`deptId` 主部门过滤 + data_scope 交集 |
| workspace-contacts | **ready** | **done** | `GET /app-api/system/dept/*` · `/app/contacts` | [contract](../../openspec/lanes/workspace-contacts/contract.md) | 左树右表 + 名片发消息 → IM `openDirectChat` |
| im-group-chat | **ready** | **done** | `POST/GET …/im/group/*` · `/app/messages` | [contract](../../openspec/lanes/im-group-chat/contract.md) | store 无 Mock；群聊 REST + WS |
| im-message-file | **ready** | **done** | `POST …/message/send` (file/image) · `/app/messages` | [contract](../../openspec/lanes/im-message-file/contract.md) | private 上传 + JWT 预览/下载 |
| im-read-receipt | **ready** | **done** | `GET …/conversation/read-status` · `/app/messages` | [contract](../../openspec/lanes/im-read-receipt/contract.md) | 单聊「已读」+ WS `read.updated` |
| im-presence | **ready** | **done** | `GET …/im/presence/batch` · messages/contacts | [contract](../../openspec/lanes/im-presence/contract.md) | 30s REST 轮询；WS push 留后续 |
| org-member-invite-notify | **ready** | **done** | `member-invite` · `/app/register`（触达改走 Bot DM，见 im-bot） | [contract](../../openspec/lanes/org-member-invite-notify/contract.md) | store 无 Mock |
| workspace-tasks | **ready** | **done** | `/app-api/task/item/*` · `/app/tasks` | [contract](../../openspec/lanes/workspace-tasks/contract.md) | store 无 Mock |
| im-bot-invite-migrate | **archived** | n/a | 邀请 → `org-assistant` · `ALL_ACTIVE_MEMBERSHIPS` | 见 archive | [archive](../../openspec/changes/archive/2026-07-16-im-bot-invite-migrate/proposal.md) |
| im-bot-reach-policy-v1 | **archived** | n/a | `im_bot.type` · system 免订阅 · 并集 · 产方 catch | 见 archive | [archive](../../openspec/changes/archive/2026-07-16-im-bot-reach-policy-v1/proposal.md) |
| im-bot-task-due-migrate | **archived** | n/a | `TASK_DUE` → `task-bot` · `ImBotApi` SINGLE | — | [archive](../../openspec/changes/archive/2026-07-16-im-bot-task-due-migrate/proposal.md) |
| im-bot-notify-foundation | **archived** | **done** | `ImBotApi` · `bot_dm` · 删 Rail/`infra_notify`；§7 子 change 已开单 | [im-bot-dm](../../openspec/lanes/im-bot-dm/contract.md) | [archive](../../openspec/changes/archive/2026-07-16-im-bot-notify-foundation/proposal.md) |
| workspace-search | **archived** | **done** | `GET /app-api/infra/workspace-search` · Rail ⌘K Modal | [contract](../../openspec/lanes/workspace-search/contract.md) | store 无 Mock；E2E 通过；[archive](../../openspec/changes/archive/2026-07-16-workspace-search-v1/proposal.md) |
| workspace-profile-card | n/a | **done** | Rail 资料名片 · 更多账号 · 设置入口 | — | [archive](../../openspec/changes/archive/2026-07-16-workspace-profile-card-web/proposal.md) |
| workspace-settings | n/a | **ui_ready** | 设置窗通用三项 · `/app/messages` 气泡布局 | [contract](../../openspec/lanes/user-preference/contract.md) | [archive](../../openspec/changes/archive/2026-07-16-workspace-settings-web/proposal.md)；待 `user-preference-integrate` |
| user-preference | **archived** | **ui_ready** | `GET/PUT …/user/preference` · 设置窗 | [contract](../../openspec/lanes/user-preference/contract.md) | API [archive](../../openspec/changes/archive/2026-07-16-user-preference-api/proposal.md)；store 仍含 localStorage 兜底，正式联调尚未立项 |
| workspace-business-card | **ready** | **done** | 飞书式个人名片 · profile/remark API | [contract](../../openspec/lanes/workspace-business-card/contract.md) | [change](../../openspec/changes/workspace-business-card-api/proposal.md)；落库，无 localStorage 真源 |

## 规划中（OpenSpec 已立项 · 待实现）

| 切片 | API 状态 | Web 状态 | 端点 / 页面 | 契约 | 说明 |
|------|----------|----------|-------------|------|------|
| **workspace-business-card-api** | **ready** | **done** | profile 扩展 · `contact-remark` | [contract](../../openspec/lanes/workspace-business-card/contract.md) | 与 web 同批联调；可 archive |
| **im-bot-group-member** | **ready** | **done** | 群挂/移除 Bot · 成员列表含 Bot | [contract](../../openspec/lanes/im-group-bot/contract.md) | [change](../../openspec/changes/im-bot-group-member/proposal.md)；G1；群主侧栏添加/移除 |
| **im-bot-runtime-platform** | **ready** | n/a | Bot Runtime SPI · noop/platform · webhook stub | — | [change](../../openspec/changes/im-bot-runtime-platform/proposal.md)；G3；Ingress 可测入口已就绪 |
| **im-bot-group-mention** | **ready** | **done** | 群 @Bot → Ingress | [contract](../../openspec/lanes/im-group-bot-mention/contract.md) | [change](../../openspec/changes/im-bot-group-mention/proposal.md)；G2；事务提交后 best-effort |
| **im-bot-interactive-card** | **ready** | **done** | card 发送 · `/app-api/im/card/action` · `/app/messages` | [contract](../../openspec/lanes/im-interactive-card/contract.md) | [change](../../openspec/changes/im-bot-interactive-card/proposal.md)；见 [约定](im-bot-interactive-card.md)；邀请卡 `system.member.invite.accept` |

### 建议下一切片（尚未立项）

| 切片 | 说明 |
|------|------|
| `user-preference-integrate` | 设置窗正式以 API 为真源；收紧 localStorage 兜底；看板 Web → **done** |

### SUPERSEDED（不再按旧写真源扩写）

| 切片 | 状态 | 说明 |
|------|------|------|
| notify-inbox-v2 | **archived / SUPERSEDED** | 写真源改为 Bot/`im_message`；见 [archive](../../openspec/changes/archive/2026-07-16-notify-inbox-v2/proposal.md) |

### V1.1 协作扩展 · 建议实施顺序

```text
1. 产方迁移：invite ✅ / task-due ✅
2. 群 Bot / card：group-member → runtime-platform → group-mention → interactive-card ✅（可 archive）
3. workspace-profile-card-web / workspace-settings-web / user-preference-api ✅
4. workspace-business-card-web（当前）→ `-api` → integrate
5. user-preference-integrate（建议并行或随后）
6. bpm-v1 — deferred，见下方「暂缓实现」
```

## 已归档规划（暂缓实现）

| 切片 | 状态 | Change | 说明 |
|------|------|--------|------|
| account-sms-verify | archived / deferred | [archive](../../openspec/changes/archive/2026-07-12-account-sms-verify/proposal.md) | 注册验证码；规格已同步，**前期不实现** |
| bpm-approval / bpm-v1 | archived / deferred | [archive](../../openspec/changes/archive/2026-07-16-bpm-v1/proposal.md) | Flowable 通用审批；规划+contract 草案已就绪，**短期内不实现**；恢复时从 archive 取出 |

## 实施顺序（system-admin-v1）

| 顺序 | Change | Lane |
|------|--------|------|
| ①a | `system-rbac-kernel-api` | 后端 |
| ①b | `system-rbac-kernel-web` | 前端（前置 ①a ready） |
| ② | `admin-dept-slice` | web → api → integrate |
| ③ | `admin-role-slice` | web → api → integrate |
| ④ | `admin-user-mutate-slice` | web → api → integrate |

## 仍为 Mock 的页面（待后续切片）

| 区域 | 页面 | 说明 |
|------|------|------|
| 工作台 | `/app/docs` | 壳层占位；V2 云文档 |
| 管理端 | `/admin` 概览 | 页面内 Mock / 占位 |

## 参考

- [frontend-first-workflow.md](frontend-first-workflow.md)
- [parallel-lane-workflow.md](parallel-lane-workflow.md)
