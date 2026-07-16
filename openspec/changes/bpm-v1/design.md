# 设计：审批工作流 V1（bpm-v1）

## Context

- 规划：[`bootstrap-v1-foundation`](../archive/2026-06-30-bootstrap-v1-foundation/design.md) §2.5、§10 — V1.1 引入 `relayflow-module-bpm` + 嵌入式 BPMN
- 触达：[`im-bot-notify-foundation`](../im-bot-notify-foundation/design.md) — `approval-bot` + `ImBotApi`（**不**走已拆除的 `NotifyInboxApi`）
- 架构：`*-api` + `*-biz`；`bpm-biz` → `system-api`（解析审批人）、`im-api`（`ImBotApi`）
- 当前：**无** bpm 模块目录；`openspec/config.yaml` 标注 V1.1 前暂不启用

## Goals / Non-Goals

**Goals:**

- 飞书式最小闭环：**我发起的** / **待我审批** 列表 + 提交表单 + 通过/驳回
- 嵌入式 **Flowable 7.x**（Spring Boot 3 兼容版本，写入 BOM）
- 内置 **单条 BPMN**「通用审批」：`start → userTask(approve) → end`
- 待办产生时经 `approval-bot` 投递 bot_dm（best-effort；不挡提交）

**Non-Goals:**

- 流程设计器、多节点、会签、条件网关
- 动态表单设计器（V1 固定字段：标题、说明）
- 管理端发起他人申请、代理审批

## Decisions

### D1：Maven 与 Flowable

```text
relayflow-module-bpm/
├── relayflow-module-bpm-api/     # BpmProcessApi、DTO、枚举
└── relayflow-module-bpm-biz/     # Flowable 配置、Service、Controller
```

`relayflow-dependencies/pom.xml`：

```xml
<flowable.version>7.0.1</flowable.version>  <!-- 实现时核对 Spring Boot 3.4 兼容 -->
```

`bpm-biz` 依赖：

- `flowable-spring-boot-starter-process`
- `relayflow-module-system-api`
- `relayflow-module-im-api`（`ImBotApi`）

`relayflow-server/pom.xml` 增加 `relayflow-module-bpm-biz`。

Flowable 表：使用引擎自带 schema update（`flowable.database-schema-update=true`）**或** 导出 SQL 入 Flyway（实现时二选一，优先 **引擎自动建 ACT_*** + Flyway 只管 `bpm_` 扩展表）。

### D2：业务扩展表 `bpm_process_instance_ext`

与 Flowable `ACT_RU_*` / `ACT_HI_*` 通过 `process_instance_id` 关联：

```sql
CREATE TABLE bpm_process_instance_ext (
  id                    BIGINT PRIMARY KEY,
  tenant_id             BIGINT NOT NULL,
  process_definition_key VARCHAR(64) NOT NULL,  -- V1: general_approval
  process_instance_id   VARCHAR(64) NOT NULL,   -- Flowable proc inst id
  title                 VARCHAR(200) NOT NULL,
  summary               VARCHAR(500),
  applicant_id          BIGINT NOT NULL,
  approver_id           BIGINT NOT NULL,          -- V1 发起人指定或默认超管
  status                VARCHAR(16) NOT NULL,     -- RUNNING | APPROVED | REJECTED | CANCELLED
  create_time           TIMESTAMP NOT NULL,
  update_time           TIMESTAMP NOT NULL,
  deleted               SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_bpm_inst_applicant ON bpm_process_instance_ext (tenant_id, applicant_id, status);
CREATE INDEX idx_bpm_inst_approver ON bpm_process_instance_ext (tenant_id, approver_id, status);
```

Codegen：`./scripts/codegen.sh --module bpm --tables bpm_process_instance_ext`

### D3：内置 BPMN `general_approval.bpmn20.xml`

```text
[start] → [userTask id=approveTask name=审批] → [end]
```

- 部署：应用启动时 `RepositoryService.createDeployment()` 若不存在则部署
- 流程变量：`applicantId`、`approverId`、`title`、`summary`
- 审批人：`approveTask` assignee = `${approverId}`（字符串 userId）

**备选**：纯状态机不引 Flowable → 拒绝，与 bootstrap「BPMN 引擎」不一致。

### D4：App API

前缀：`/app-api/bpm`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/instance/submit` | body `{ title, summary?, approverId? }`；无 approverId 时取租户首个 `super_admin` |
| GET | `/instance/mine/page` | 我发起的，分页 |
| GET | `/task/todo/page` | 待我审批（Flowable task query + ext 表 join） |
| GET | `/instance/get?id=` | 详情含状态与时间线（简化：仅 ext + 当前 task） |
| POST | `/task/approve` | body `{ taskId, comment? }` |
| POST | `/task/reject` | body `{ taskId, comment? }` |

鉴权：JWT + 有效成员；**不**用 `sys_permission`（工作台产品面）。

错误码：`BPM_INSTANCE_NOT_FOUND`、`BPM_TASK_NOT_FOUND`、`BPM_TASK_FORBIDDEN`、`BPM_APPROVER_REQUIRED`

### D5：触达生产方（approval-bot）

`bpm-biz` 在创建待办（流程进入 `approveTask`）后：

```java
ImBotSendTarget target = new ImBotSendTarget();
target.setScope(ImBotSendTarget.SCOPE_SINGLE);
target.setTenantId(tenantId);
target.setUserId(approverId);

ImBotSendCommand command = new ImBotSendCommand();
command.setBotCode("approval-bot");
command.setText("「" + title + "」待你审批");
command.setDedupeKey("APPROVAL_PENDING:" + extId);
command.setRoute("/app/approvals?instanceId=" + extId);
command.setEntityType("approval");
command.setEntityId(String.valueOf(extId));
command.setTarget(target);
try {
  imBotApi.send(command);
} catch (Exception e) {
  log.warn(...); // best-effort；不回滚提交
}
```

依赖 foundation 已种子的 `approval-bot` 与 system Bot 免订阅策略。审批完成：**不**撤回已发 bot 消息。与 task-due 策略一致。

### D6：前端

```text
web/src/
├── pages/app/approvals/index.vue
├── stores/approvals.ts
├── api/app/bpm.ts
└── composables/useWorkspaceNav.ts   # 新增 rail 项「审批」
```

| 区域 | V1 |
|------|-----|
| Panel | Tab：待我审批 / 我发起的 |
| Main | 列表 + 详情 `UDrawer`：标题、说明、申请人、通过/驳回 |
| 新建 | `UModal`：标题、说明、审批人 `USelectMenu`（成员列表，复用 contacts API） |
| 导航 | Rail 增加 `id=approvals`，icon `i-lucide-file-check` |

路由：`/app/approvals`；query `instanceId` 打开详情。

### D7：管理端（可选 V1）

`/admin/bpm/process` — 只读列表已部署 `processDefinitionKey`、版本、部署时间。无建模 UI。可在 `bpm-approval-integrate` 之后单独小 change，**不阻塞** MVP。

### D8：与 task 模块边界

| 模块 | 职责 |
|------|------|
| `task` | 个人待办清单（TODO/DONE） |
| `bpm` | 多人审批流程实例、Flowable 引擎 |

禁止在 `task_item` 存审批状态。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Flowable 表体积 | V1 单流程、无历史归档任务 |
| 审批人离职 | V1 不处理；文档注明后续委托 change |
| Bot 投递失败 / 用户未开 messages | 待办列表仍可用；send best-effort |

## Migration Plan

1. 加 Maven 模块与 BOM 依赖  
2. Flyway `bpm_process_instance_ext`  
3. 启动部署 BPMN  
4. 前端审批页  
5. 回滚：server 去掉 bpm-biz 依赖

## Open Questions

1. Flowable schema 由引擎自动建还是 Flyway？→ **ACT_ 引擎自动；`bpm_` Flyway**  
2. 审批入口放 Rail 还是任务 Tab？→ **独立 Rail「审批」**，与飞书一致

## 验证

```bash
openspec validate bpm-v1 --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# E2E：A 提交 → B 待办列表 → 通过 → A 发起列表状态已通过
```

## 看板登记

| 切片 | 页面 | 端点 |
|------|------|------|
| bpm-approval | `/app/approvals` | `/app-api/bpm/*` |

## 子 change 边界

| 子 change | 范围 |
|-----------|------|
| `bpm-schema-v1` | pom、Flowable 配置、Flyway ext 表、BPMN 部署 |
| `bpm-approval-web` | UI + Mock + `openspec/lanes/bpm-approval/contract.md` |
| `bpm-approval-api` | REST + Flowable 编排 + `ImBotApi(approval-bot)` |
| `bpm-approval-integrate` | 联调 + 看板 done |
