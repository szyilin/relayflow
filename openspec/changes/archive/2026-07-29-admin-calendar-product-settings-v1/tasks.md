# Tasks: admin-calendar-product-settings-v1

> **母 change**。实现按子切片拆分；默认 `-web` → `-api` → `-integrate`。勾选前先读 `design.md`。

## 1. 规格深化（本文档会话）

- [x] 1.1 拍板 Open Questions；补全 design（全员日历、本机色、`···`/设置面板、组织薄策略）
- [x] 1.2 补全 delta specs（`admin-calendar-product-settings` / `calendar` / `admin-product-settings` / `user-preference`）
- [x] 1.3 `openspec validate admin-calendar-product-settings-v1 --strict` 通过

## 2. 工作台：日历设置 UX + 本机色（`workspace-calendar-settings`）

- [x] 2.1 **-web**：侧栏 `···`（分享 / 修改颜色=本机色 / 日历设置）；设置面板按角色读写；list 展示 `effectiveColor`；起草/扩展 lane contract；更新 `workspace-ui-patterns` 日历节草案说明
- [x] 2.2 **-api**：`cal_calendar_user_pref` + App 本机色 API；日历 update 仍仅所有者改默认元数据；list 返回默认色/本机色/有效色
- [x] 2.3 **-integrate**：去临时数据；`pnpm build` + `pnpm typecheck` + `mvn compile`；浏览器冒烟

## 3. 管理端：组织壳 + 全员日历（`admin-calendar-all-staff`，可与薄策略同切片）

- [x] 3.1 **-web**：`/admin/products/calendar` 去占位；全员日历列表/创建编辑（名称、描述、默认色、可见范围）；产品注册表 `calendar.adminEnabled=true`；权限码门禁 UI
- [x] 3.2 **-api**：Flyway `ALL_STAFF` + `cal_all_staff_scope` + `cal_tenant_settings`；Admin product-settings / all-staff API；App list/事件可见性按 scope 自动订阅；`allowMemberCreateOwnedCalendar` 强制；权限种子
- [x] 3.3 **-integrate**：Admin↔App 联调（创建全员 → 成员侧栏「我订阅的」→ 默认色/本机色）；看板；验证命令

## 4. 收尾

- [x] 4.1 更新 `docs/dev/workspace-ui-patterns.md` 日历节与 `api-integration-board.md`
- [x] 4.2 扩展或新建 `openspec/lanes/**/contract.md` 与实现一致（本机色 + admin-calendar-product-settings contract）
- [x] 4.3 母 change `openspec validate --strict`；确认非目标未误做（公共日历、五级 ACL、导出）
