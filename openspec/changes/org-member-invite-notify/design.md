# 设计：组织邀请通知（org-member-invite-notify）

## Context

- 邀请写库：[`UserServiceImpl.inviteMember`](../../../relayflow-module-system/relayflow-module-system-biz/src/main/java/com/relayflow/module/system/service/user/UserServiceImpl.java) → `NOT_JOINED`
- 注册激活：[`AuthRegisterServiceImpl`](../../../relayflow-module-system/relayflow-module-system-biz/src/main/java/com/relayflow/module/system/service/auth/AuthRegisterServiceImpl.java) 激活全部 `NOT_JOINED`
- 占位接口：[`im-platform-foundation` design D6](../archive/2026-07-12-im-platform-foundation/design.md) `NotifyInboxApi`
- 权限：注册页 pending API 为 **permitAll**；inbox API 须 JWT + 有效成员

## Goals / Non-Goals

**Goals:**

- 管理员邀请后，被邀请人能在 **注册前**（pending 预览）与 **登录后**（inbox）感知邀请
- `NotifyInboxApi.push` 可被 `system-biz` 调用，不依赖 `im-biz`
- V1 仅 `MEMBER_INVITE` 类型；表结构与 API 预留 `type` 扩展

**Non-Goals:**

- 短信/邮件、邀请 deep link token
- 全量通知中心（审批、任务、@我）
- 管理端查看他人通知

## D1：数据模型 `infra_notify`

```sql
CREATE TABLE infra_notify (
  id            BIGINT PRIMARY KEY,
  tenant_id     BIGINT NOT NULL,          -- 邀请方租户
  user_id       BIGINT,                   -- 接收人；邀请时可能尚无 user，nullable
  mobile        VARCHAR(20),              -- 邀请手机号；user_id 为空时用于注册页关联
  type          VARCHAR(32) NOT NULL,     -- V1: MEMBER_INVITE
  title         VARCHAR(200) NOT NULL,
  body          VARCHAR(500),
  payload_json  JSONB,                    -- { "inviterNickname", "tenantName", "tenantId" }
  read_flag     SMALLINT NOT NULL DEFAULT 0,
  create_time   TIMESTAMP NOT NULL,
  update_time   TIMESTAMP NOT NULL,
  deleted       SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_infra_notify_user ON infra_notify (tenant_id, user_id, read_flag) WHERE deleted = 0;
CREATE INDEX idx_infra_notify_mobile ON infra_notify (mobile, read_flag) WHERE deleted = 0 AND user_id IS NULL;
```

| 字段 | 说明 |
|------|------|
| `user_id` | 邀请时若 `sys_user` 已存在则写入；否则仅 `mobile`，注册成功后回填 |
| `read_flag` | 0=未读，1=已读；接受/注册激活后可批量已读 |

Flyway：`V0.1.0.{n}__infra_notify.sql`（序号按当前最新迁移递增）。

## D2：NotifyInboxApi 实现

```text
relayflow-module-infra-api
  └── api/notify/NotifyInboxApi.java          # 已有或新建
relayflow-module-infra-biz
  └── service/notify/NotifyInboxServiceImpl
```

```java
public record NotifyItemCommand(
    Long tenantId,
    Long userId,           // nullable
    String mobile,         // nullable，与 userId 至少其一
    String type,
    String title,
    String body,
    Map<String, Object> payload
) {}
```

**规则**：

- `userId` 与 `mobile` 不可同时为空
- 同租户同手机号同 type 的未读 `MEMBER_INVITE` 可 **幂等更新** payload（重复邀请刷新 `update_time`）或拒绝重复 — **V1 选择幂等更新**，避免收件箱刷屏

## D3：邀请钩子

在 `UserServiceImpl.inviteMember` 事务提交后（或同事务内）：

```text
inviteMember 成功
  → 解析 tenantName、inviterNickname
  → NotifyInboxApi.push(MEMBER_INVITE)
       userId = 已存在 sys_user.id 或 null
       mobile = request.mobile
```

`system-biz` 依赖 `infra-api`（符合架构：跨域仅 `*-api`）。

## D4：公开 pending 预览

| 端点 | 鉴权 | 说明 |
|------|------|------|
| `GET /app-api/system/member-invite/pending?mobile=` | permitAll | 返回 `{ items: [{ tenantId, tenantName, invitedAt }] }` |

实现：查 `sys_tenant_user` + `sys_tenant` where `mobile` 匹配用户且 `status=NOT_JOINED`；**不**暴露管理员手机号、部门细节。

与 inbox 关系：pending 来自成员关系真源；inbox 为「通知记录」副本，注册页优先用 pending（更准确），inbox 用于登录后历史提醒。

## D5：收件箱 App API

| 端点 | 说明 |
|------|------|
| `GET /app-api/infra/notify/page?pageNo=&pageSize=` | 当前用户 JWT `user_id` 未读+已读分页 |
| `GET /app-api/infra/notify/unread-count` | 未读数（Rail 角标） |
| `POST /app-api/infra/notify/read` | body `{ ids: [] }` 标记已读 |

**鉴权**：JWT；按 `user_id` 过滤，**不**跨租户泄漏（`MEMBER_INVITE` 的 `tenant_id` 在 payload 中供展示）。

## D6：注册页 UI

```text
/app/register?mobile=138...
  ┌─────────────────────────────────────┐
  │ UAlert：你收到 2 个企业邀请，注册后将自动加入 │
  │   · 张三的工作室                        │
  │   · Acme 科技                          │
  └─────────────────────────────────────┘
  （原有注册表单）
```

- mobile 输入 blur/change 时 debounce 调 `pending` API
- 无 mobile 时不请求

## D7：工作台通知 UI（V1 最小）

```text
WorkspaceRail
  └── 铃铛图标 + unread-count 角标
        点击 → UModal 或侧滑列表（最近 20 条）
        MEMBER_INVITE 项 → 文案「XXX 邀请你加入 YYY」
        已注册多租户用户：引导使用企业切换器（不重复 accept 流程）
```

未登录用户不展示铃铛。

## D8：WebSocket `domain=notify`（V1 可选）

若时间紧，V1 可仅 REST 轮询 `unread-count`（进入工作台时拉一次 + 邀请后下次登录可见）。

若实现：

```json
{ "domain": "notify", "type": "notify.new", "payload": { "unreadCount": 3 } }
```

升级 `infra-biz` 占位 Handler；`NotifyInboxServiceImpl.push` 后 fanout 至在线 `user_id`。

## D9：注册后回填 user_id

`AuthRegisterServiceImpl` 创建/绑定 `sys_user` 后：

```text
UPDATE infra_notify SET user_id = ? WHERE mobile = ? AND user_id IS NULL
```

同事务或注册成功后异步均可；须保证 inbox 登录后可见历史邀请通知。

## 验证

```bash
openspec validate org-member-invite-notify --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 浏览器：管理端邀请 → 注册页见横幅 → 注册 → 工作台铃铛
```

## 看板登记

| 切片 | 页面 | 端点 |
|------|------|------|
| org-member-invite-notify | `/app/register`、Rail 铃铛 | `member-invite/pending`、`infra/notify/*` |
