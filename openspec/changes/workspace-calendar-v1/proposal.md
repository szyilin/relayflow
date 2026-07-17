# 提案：工作台日历 V1（workspace-calendar-v1 · 母 change）

## Why

工作台主导航仍缺 **日历** 实质能力（对照飞书：多日历图层 + 日/周/月 + 快捷建日程 + 邀约提醒）。消息、任务、通讯录已可用；补齐日历后形成「沟通 + 待办 + 时间安排」最小协作闭环。现有 `ImBotApi` 与用户偏好（企业内个人设置）可直接承接提醒与日历偏好，适合现在做。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。`[平台]` 可先行。

1. **新 Maven 域** `relayflow-module-calendar`（`*-api` + `*-biz`），表前缀 `cal_`
2. **日历容器**：入企 ensure 主日历；用户可新建/改名改色/删除自有日历（主日历不可删）
3. **日程**：挂在日历上的 CRUD（标题、描述、起止、全天、提醒）；**可邀请租户内成员**；被邀请人可见；组织者可改删
4. **触达**：`calendar-bot` + `ImBotApi`（邀约 / 变更 / 取消 / 开始前提醒），best-effort
5. **前端** `/app/calendar`：飞书式侧栏（迷你月历 + 我管理的日历勾选）+ 日/周/月网格 + 快捷创建弹层
6. **设置**：日历偏好（周起始日、默认时长、默认提醒等）进入 **全局设置窗** 新分类「日历」，持久化到现有 `user-preference`（`(tenant_id, user_id)`）
7. **看板**：登记 `workspace-calendar` 切片与 lane contract

## Capabilities

### New Capabilities

- `calendar`：日历容器、日程、参与人、提醒触达与工作台 `/app/calendar` 行为

### Modified Capabilities

- `user-preference`：增加 `settings.calendar`（周起始、默认时长、默认提醒等）；设置窗增加「日历」分类
- `im`：平台种子增加 system 型 `calendar-bot`（日历助手）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| Maven | 根 `pom.xml`、`relayflow-server` | 引入 `relayflow-module-calendar-biz` |
| DB | Flyway `V0.1.0.x` | `cal_calendar` / `cal_event` / `cal_attendee`；`im_bot` 种子 `calendar-bot` |
| 后端 | `relayflow-module-calendar-*` | 新建；依赖 `system-api`、`im-api` |
| 后端 | `user-preference` 合并默认 | JSON 增加 `calendar` 段 |
| 前端 | `web/` Rail、`/app/calendar`、设置窗、store/api | 新页面 + preference UI |
| 文档 | `database.md` 表前缀、`api-integration-board` | 登记 `cal_` 与切片 |
| 迁移 | 仅增量 Flyway | 可回滚删除模块依赖；表可保留空 |

## 非目标（V1）

- 整本日历共享 / 订阅同事 / 仅忙闲（**V1.1**）
- 侧栏「我的任务」虚拟日历图层
- 重复日程（RRULE）/ 例外日程
- 会议室、视频会议、公共/全员日历、CalDAV/第三方同步
- 拖拽改期（可用弹层改时间）；管理端日历管理

## 拍板结论（2026-07-17）

| 项 | 结论 |
|----|------|
| 邀请参与人 | **V1 要** |
| 整日历共享 / 订阅同事 | **V1.1** |
| 侧栏「我的任务」图层 | **不要** |
| 重复日程 | **V1 不出** |
| 日历设置入口 | **全局设置窗**（头像 → 设置）；企业内当前用户独有，后续同类设置统一放此 |

## 子 change 切片（实现顺序）

```text
workspace-calendar-v1                 ← 本 change（规划母版）
├── calendar-schema-v1                [平台] Flyway + Maven + codegen + calendar-bot 种子
├── workspace-calendar-web            UI + Mock/store 临时数据 + contract + 设置窗日历分类
├── workspace-calendar-api            REST + Bot 触达 + preference 默认键
└── workspace-calendar-integrate      去临时数据联调
```
