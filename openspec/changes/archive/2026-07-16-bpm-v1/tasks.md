# Tasks：bpm-v1（母 change · 执行路线图）

> **用法**：本文件是审批工作流 V1 的 **总路线图**。实际编码按 **子切片** 分批；每次会话 ≤10 条 checkbox。  
> **顺序**：`[平台] schema` 可先行；业务 UI 默认 **前端优先**（`-web` → `-api` → `-integrate`）。  
> **设计真源**：`design.md`；**行为**：`specs/bpm`。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate bpm-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `bpm-approval`（planned）
- [x] 0.4 更新 `openspec/config.yaml`：bpm 模块 V1.1 启用说明
- [x] 0.5 `specs-sync-checklist.md`（归档前核对清单）

---

## 1. [平台] bpm-schema-v1

**目标**：Maven 模块、Flowable、扩展表、内置 BPMN。  
**范围**：Java + Flyway + BOM；无 `web/`。

- [ ] 1.1 根 `pom.xml` + `relayflow-module-bpm`（api + biz）+ `relayflow-server` 依赖
- [ ] 1.2 BOM 增加 Flowable 版本与 `flowable-spring-boot-starter-process`
- [ ] 1.3 Flyway `bpm_process_instance_ext` + codegen `bpm_process_instance_ext`
- [ ] 1.4 `resources/processes/general_approval.bpmn20.xml` + 启动部署
- [ ] 1.5 Flowable 配置（`database-schema-update`、async executor 关闭或最小化）
- [ ] 1.6 `./mvnw -pl relayflow-server -am compile`；启动无报错

**验证**：compile + 本地启动日志见流程部署成功。

---

## 2. bpm-approval-web（前端 lane）

**目标**：`/app/approvals` + Rail 入口 + contract。  
**可与 §1 并行**（Mock 不依赖引擎）。

- [x] 2.1 起草 `openspec/lanes/bpm-approval/contract.md`
- [ ] 2.2 `pages/app/approvals/index.vue`：待我审批 / 我发起 Tab
- [ ] 2.3 `stores/approvals.ts` + `api/app/bpm.ts`（Mock）
- [ ] 2.4 新建 `UModal` 提交；`UDrawer` 详情 + 通过/驳回
- [ ] 2.5 `useWorkspaceNav` 增加「审批」rail 项
- [ ] 2.6 `cd web && pnpm build`
- [ ] 2.7 浏览器：`/app/approvals` Mock 流程可走通

**验证**：`pnpm build` + 浏览器路径。

**完成后**：看板 web → `ui_ready`。

---

## 3. bpm-approval-api（后端 lane）

**依赖**：§1 schema；§2 contract

- [ ] 3.1 `BpmApprovalService`：submit / todo page / mine page / get / approve / reject
- [ ] 3.2 `AppBpmController` under `/app-api/bpm`
- [ ] 3.3 默认审批人解析（`system-api` 查 super_admin 或文档策略）
- [ ] 3.4 新待办：`ImBotApi.send(approval-bot, SINGLE)` + `dedupeKey` + deep link（`im-api` only；best-effort）
- [ ] 3.5 单测：提交、通过、驳回、无权；curl 示例写入 contract
- [ ] 3.6 `./mvnw -pl relayflow-module-bpm-biz -am test` 或 server compile

**完成后**：看板 api → `ready`。

---

## 4. bpm-approval-integrate（联调）

- [ ] 4.1 前端去 Mock，接真实 API
- [ ] 4.2 E2E：A 提交 → B 待办 → 通过 → A 状态已批准
- [ ] 4.3 （可选）与 messages 联调：B 的 `approval-bot` bot_dm 出现待办提醒 + deep link 进审批
- [ ] 4.4 `openspec validate bpm-v1 --strict`
- [ ] 4.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`
- [ ] 4.6 看板 `bpm-approval` → **done**

---

## 5. 母 change 归档前

- [ ] 5.1 README / `openspec/config.yaml` bpm 已启用表述一致
- [ ] 5.2 `openspec archive bpm-v1`
- [ ] 5.3 `./mvnw verify`（如适用）

---

## 执行顺序速查

```text
Session 1   §0 + §1 schema
Session 2   §2 web（可与 Session 1 并行）
Session 3   §3 api
Session 4   §4 integrate + §5 归档
```

**依赖提示**：§3.4 触达依赖 [`im-bot-notify-foundation`](../im-bot-notify-foundation/proposal.md) 已落地的 `ImBotApi` / `approval-bot`；Bot 投递失败时待办列表仍须可用。

## 后续 change

| Change | 说明 |
|--------|------|
| `bpm-designer-v1` | 管理端流程设计 |
| `bpm-form-attach` | 动态表单 + 附件 |
| `im-bot-interactive-card` | 审批同意/拒绝卡片回调（后置） |

---

## 会话开场白模板

```text
Using change: bpm-schema-v1（bpm-v1 子切片 · 平台）
Read: openspec/changes/bpm-v1/design.md §D1–D3
Tasks: bpm-v1/tasks.md §1
```
