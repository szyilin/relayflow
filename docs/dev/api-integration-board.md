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
| workspace-settings | n/a | **done** | 设置窗通用三项 · `/app/messages` 气泡布局 | [contract](../../openspec/lanes/user-preference/contract.md) | [archive](../../openspec/changes/archive/2026-07-16-workspace-settings-web/proposal.md)；偏好真源 → frontend-eng-hardening |
| user-preference | **archived** | **done** | `GET/PUT …/user/preference` · 设置窗 | [contract](../../openspec/lanes/user-preference/contract.md) | API [archive](../../openspec/changes/archive/2026-07-16-user-preference-api/proposal.md)；客户端真源 [frontend-eng](../../openspec/changes/archive/2026-07-17-frontend-eng-hardening-v1/proposal.md) |
| workspace-business-card | **archived** | **done** | 飞书式个人名片 · profile/remark API | [contract](../../openspec/lanes/workspace-business-card/contract.md) | [archive](../../openspec/changes/archive/2026-07-17-workspace-business-card-api/proposal.md)；落库，无 localStorage 真源 |
| im-self-direct-chat | **archived** | **done** | self-DIRECT · 本人名片发消息 | — | [archive](../../openspec/changes/archive/2026-07-17-im-self-direct-chat/proposal.md) |
| **workspace-calendar** | **archived** | **done** | `/app-api/calendar/*` · `/app/calendar` | [contract](../../openspec/lanes/workspace-calendar/contract.md) | [archive](../../openspec/changes/archive/2026-07-17-workspace-calendar-v1/proposal.md)；store 无 Mock |
| **workspace-calendar-share** | **archived** | **done** | `/app-api/calendar/share/*` · 侧栏共享 | [contract](../../openspec/lanes/workspace-calendar/contract.md) | [archive](../../openspec/changes/archive/2026-07-17-workspace-calendar-v1-1/proposal.md) |
| **workspace-calendar-rrule** | **archived** | **done** | RRULE / editScope · 编辑器重复 | 同上 | 同上 |
| **workspace-calendar-dnd** | **archived** | **done** | `PUT /event/reschedule` · 日/周拖拽 | 同上 | 同上 |
| **task-calendar-projection** | **archived** | **done** | `GET …/task/item/due-range` · `/app/calendar` 任务图层 | [contract](../../openspec/lanes/task-calendar-projection/contract.md) | [archive](../../openspec/changes/archive/2026-07-17-task-calendar-projection/proposal.md)；store 无 Mock/回退 |
| **workspace-task-core** | **archived** | **done** | `/app/tasks` 详情 + 协作（关注/评论/动态/指派） | [detail](../../openspec/lanes/workspace-task-detail/contract.md) · [collab](../../openspec/lanes/workspace-task-collab/contract.md) | [archive](../../openspec/changes/archive/2026-07-17-workspace-task-core-v1/proposal.md)；P0/P1 integrate 完成；store 无 Mock |
| frontend-eng-hardening | n/a | **done** | 租户 reset、分页、偏好 API 真源、God 拆分、Dock 威胁模型 | — | [archive](../../openspec/changes/archive/2026-07-17-frontend-eng-hardening-v1/proposal.md) |

## 规划中（OpenSpec 已立项 · 待实现）

| 切片 | API 状态 | Web 状态 | 端点 / 页面 | 契约 | 说明 |
|------|----------|----------|-------------|------|------|
| **workspace-task-list** | **archived** | **done** | `/app-api/task/list/*` · `/app/tasks?listId=` | [contract](../../openspec/lanes/workspace-task-list/contract.md) | integrate 完成；store 无本地临时清单 |
| **workspace-task-board** | **archived** | **done** | status 三态 · `PUT …/board-move` · `/app/tasks` 看板 | [contract](../../openspec/lanes/workspace-task-board/contract.md) | 母 change；P1；store 无本地拖拽暂存；**将被 view-model 字段分组演进** |
| **workspace-task-view-model-v1** | **planned** | **planned** | 快速访问 · ViewConfig · 字段分组 · 分配人 · 我负责的个人组 · 多负责人 · 多清单 · 清单内组 | [proposal](../../openspec/changes/workspace-task-view-model-v1/proposal.md) | 母 change；对齐飞书「快捷视图 ≠ 清单」；计划 B 个人组 |
| **workspace-docs-library** | **planned** | **planned** | `/app-api/docs/*` · `/app/docs` 我的文档库 | [proposal](../../openspec/changes/workspace-docs-library-v1/proposal.md) | 母 change；TipTap JSON 真源；V1 仅 MD 导出；无 `doc_embed`；云盘/知识库另开 |
| **workspace-task-quick-views** | **archived** | **done** | `scope=ALL\|ASSIGNED_BY_ME` · `assignerId` · `/app/tasks` 快速访问 | [contract](../../openspec/lanes/workspace-task-quick-views/contract.md) | integrate 完成；store 无本地临时 |
| **workspace-task-view-config** | **archived** | **done** | `GET/PUT …/view-config/*` · `/app/tasks` 工具栏 | [contract](../../openspec/lanes/workspace-task-view-config/contract.md) | integrate 完成；store 无本地暂存 |
| **workspace-task-group-by-field** | **archived** | **done** | `PUT …/group-move` · `/app/tasks` 按字段分区/看板列 | [contract](../../openspec/lanes/workspace-task-group-by-field/contract.md) | integrate 完成；store 无本地拖拽暂存；`board-move` 过渡兼容 |
| **workspace-task-multi-assignee** | **archived** | **done** | `PUT …/assignees` · 详情多选负责人 · MINE=包含我 | [contract](../../openspec/lanes/workspace-task-multi-assignee/contract.md) | integrate 完成；store 无本地暂存 |
| **workspace-task-assigner** | **archived** | **done** | `assignerId` 展示 · `scope=ASSIGNED_BY_ME`（既有） | [contract](../../openspec/lanes/workspace-task-assigner/contract.md) | integrate 完成；写入随 assignees；无本地 Mock |
| **workspace-task-mine-groups** | **archived** | **done** | `GET/POST/PUT/DELETE …/mine-group/*` · `/app/tasks`「我负责的」自定义分组 | [contract](../../openspec/lanes/workspace-task-mine-groups/contract.md) | integrate 完成；store 无本地 Mock |
| **workspace-task-multi-list** | **archived** | **done** | `PUT …/list-memberships` · 详情多清单 | [contract](../../openspec/lanes/workspace-task-multi-list/contract.md) | integrate 完成；store 无本地 Mock |
| **workspace-task-list-groups** | **archived** | **done** | `GET/POST/PUT/DELETE …/list-group/*` · 清单内分组 | [contract](../../openspec/lanes/workspace-task-list-groups/contract.md) | integrate 完成；store 无本地 Mock |
| **workspace-task-custom-field** | **archived** | **done** | 清单单选自定义字段 · `groupBy=custom:{id}` · EAV | [contract](../../openspec/lanes/workspace-task-custom-field/contract.md) | P8；integrate 完成；store 无本地 Mock |

**已交付顺序**：`list-web` → `list-api` → `list-integrate` → `board-web` → `board-api` → `board-integrate`。

**下一母 change 建议顺序**：`quick-views` ✅ → `view-config` ✅ → `group-by-field` ✅ → `multi-assignee` ✅ → `assigner` ✅ → `mine-groups` ✅ → `multi-list` ✅ → `list-groups` ✅ → **`custom-field` ✅（P8）**。

### 建议下一切片（尚未立项或可并行）

| 切片 | 说明 |
|------|------|
| `workspace-docs-library` | **已立项**（母 change）；实现顺序：`docs-schema` → `-web` → `-api` → `-integrate` |
| `workspace-docs-drive-v1` | 云盘（文档库交付后再开详细母 change） |
| `workspace-docs-wiki-v1` | 知识库（更后） |

### SUPERSEDED（不再按旧写真源扩写）

| 切片 | 状态 | 说明 |
|------|------|------|
| notify-inbox-v2 | **archived / SUPERSEDED** | 写真源改为 Bot/`im_message`；见 [archive](../../openspec/changes/archive/2026-07-16-notify-inbox-v2/proposal.md) |
| domain-event-redis-streams | **archived / REVERTED** | Redis Streams 缺可靠 ack；V1 回退 `@Lazy`；见 [archive](../../openspec/changes/archive/2026-07-17-domain-event-redis-streams/proposal.md) |

### V1.1 协作扩展 · 建议实施顺序

```text
1. 产方迁移：invite ✅ / task-due ✅
2. 群 Bot / card：group-member → runtime-platform → group-mention → interactive-card ✅
3. workspace-profile-card-web / workspace-settings-web / user-preference-api ✅
4. workspace-business-card-web / -api / im-self-direct-chat ✅
5. workspace-calendar-v1 ✅ archive
6. workspace-calendar-v1-1（共享 / RRULE / DnD）✅ archive
7. task-calendar-projection（任务图层投影）✅ archive
8. workspace-task-core-v1（任务详情 + 协作）✅ archive
9. frontend-eng-hardening-v1（含偏好 API 真源）✅ archive
10. bpm-v1 — deferred，见下方「暂缓实现」
```

## 已归档规划（暂缓实现）

| 切片 | 状态 | Change | 说明 |
|------|------|--------|------|
| account-sms-verify | archived / deferred | [archive](../../openspec/changes/archive/2026-07-12-account-sms-verify/proposal.md) | 注册验证码；规格已同步，**前期不实现** |
| bpm-approval / bpm-v1 | archived / deferred | [archive](../../openspec/changes/archive/2026-07-16-bpm-v1/proposal.md) | Flowable 通用审批；规划+contract 草案已就绪，**短期内不实现**；恢复时从 archive 取出 |
| domain-event-redis-streams | archived / REVERTED | [archive](../../openspec/changes/archive/2026-07-17-domain-event-redis-streams/proposal.md) | 领域事件 Redis Streams；实现撤回，V1 仍用 `@Lazy` 同步 |

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
