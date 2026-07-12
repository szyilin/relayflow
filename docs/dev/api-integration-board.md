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
| org-member-invite-notify | **ready** | **done** | `member-invite/pending` · `infra/notify/*` · `/app/register`、Rail 铃铛 | [contract](../../openspec/lanes/org-member-invite-notify/contract.md) | store 无 Mock |

## 规划中（OpenSpec 已立项 · 待实现）

| 切片 | API 状态 | Web 状态 | 端点 / 页面 | Change | 说明 |
|------|----------|----------|-------------|--------|------|
| workspace-tasks | planned | **ui_ready** | `/app-api/task/item/*` · `/app/tasks` | [contract](../../openspec/lanes/workspace-tasks/contract.md) | UI + Mock；待 `-api` |
| account-sms-verify | planned | pending | `auth/sms/send` · `/app/register` | [account-sms-verify](../../openspec/changes/account-sms-verify/proposal.md) | 注册验证码；可选 |

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
