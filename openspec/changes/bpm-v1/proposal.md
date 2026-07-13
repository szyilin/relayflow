# 提案：审批工作流 V1（bpm-v1 · 母 change · 执行路线图）

## Why

[`bootstrap-v1-foundation`](../archive/2026-06-30-bootstrap-v1-foundation/design.md) 将 **审批工作流** 标为 **V1.1** 能力；当前仓库 **无** `relayflow-module-bpm` 实现，工作台也无审批入口。飞书「审批」是办公面核心：**发起申请 → 待我审批 → 结果通知**。

本 change 启用 `relayflow-module-bpm`，以 **嵌入式 Flowable** 交付 **单模板通用审批**（提交 → 单人审批 → 通过/驳回），并对接 [`notify-inbox-v2`](../notify-inbox-v2/proposal.md) 的 `APPROVAL_PENDING` 通知类型。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。`[平台]` 子 change 可先行。

1. **Maven 域**：`relayflow-module-bpm`（`*-api` + `*-biz`），`relayflow-server` 加载 `bpm-biz`；BOM 引入 Flowable Spring Boot Starter
2. **数据**：`bpm_` 业务扩展表 + Flowable 引擎表（`ACT_*`，随引擎自动建表或 Flyway 管理，见 design）
3. **流程 V1**：内置一条 BPMN「通用审批」— 申请人提交 → 指定审批人（发起人自选或默认租户管理员）→ 结束
4. **App API**：`POST /app-api/bpm/instance/submit`、`GET .../todo/page`、`GET .../mine/page`、`POST .../task/approve`、`POST .../task/reject`
5. **通知**：待审批任务创建时 `NotifyInboxApi.push(APPROVAL_PENDING)`（依赖 `notify-inbox-v2` 类型目录；可降级为仅 REST 列表）
6. **前端**：工作台新增 Rail 入口「审批」`/app/approvals`（或并入任务区 Tab，见 design）；待办列表 + 详情抽屉 + 通过/驳回
7. **管理端（V1 最小）**：`/admin/bpm/process` 只读展示已部署流程定义列表（可选，非阻塞 MVP）

## Capabilities

### New Capabilities

- `bpm`：审批实例、待办任务、Flowable 嵌入式引擎、工作台审批 UI

### Modified Capabilities

- `infra`：`NotifyInboxApi` 消费方示例扩展为 `APPROVAL_PENDING`（生产方在 bpm-biz；类型目录由 notify-inbox-v2 定义）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| Maven | 根 `pom.xml`、`relayflow-dependencies` BOM | 新模块 + Flowable 版本 |
| DB | Flyway + Flowable schema | `bpm_*` 扩展表；`ACT_*` 引擎表 |
| 后端 | `relayflow-module-bpm-*` | 新建；`bpm-biz` → `system-api`、`infra-api` |
| 前端 | `web/src/pages/app/approvals/`、`useWorkspaceNav` | 新路由与导航项 |
| 规格 | `openspec/specs/bpm`（新建） | 归档时同步 |
| 配置 | `openspec/config.yaml` | 启用 bpm 模块说明 |

**回滚**：从 `relayflow-server/pom.xml` 移除 `bpm-biz` 依赖即可停用；Flowable 表可保留。审批入口前端可 feature-flag。

## 非目标

- 可视化 BPMN 设计器、多节点会签、条件分支、子流程
- 与 IM 会话卡片联动、附件表单、电子签
- 管理端流程建模、权限到按钮级流程授权
- 独立 `bpm-server`（Phase 2）
- 替换或复用 `task_` 模块做审批（审批走 `bpm_` + Flowable）

## 前置

- 统一登录、多租户 JWT、`NotifyInboxApi` 基础实现（[`org-member-invite-notify`](../archive/2026-07-12-org-member-invite-notify/proposal.md)）
- **推荐** [`notify-inbox-v2`](../notify-inbox-v2/proposal.md) §1–§2 完成后再做审批通知联调；MVP 可先仅待办列表

## 子 change 切片（实现顺序）

```text
bpm-v1                           ← 本 change（规划母版）
├── bpm-schema-v1                [平台] Maven 模块 + Flowable + bpm_ 扩展表 + 内置 BPMN
├── bpm-approval-web             工作台审批 UI + Mock + contract
├── bpm-approval-api             提交/待办/审批 REST + NotifyInbox 钩子
└── bpm-approval-integrate       去 Mock、E2E、看板 done
```

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `bpm-designer-v1` | 管理端流程设计器 |
| `bpm-form-attach` | 动态表单 + 附件 |
| `bpm-delegate` | 转交、加签 |
| `workspace-search-v2` | 审批单纳入搜索 |
