# Tasks: admin-product-settings-v1

> **母 change 路线图**。实现时按阶段推进；单次 apply 会话只做 **一个阶段**（约 ≤10 条），或另立子 change（`-web` → `-api` → `-integrate`）后在本清单勾选对应阶段。  
> 云文档 / 日历：本母 change **仅占位**；详规与实现待 IM 全阶段完成后再立 change。  
> **不做**：群组管理。

## 1. 产品设置壳层 + 图标注册表（-web）

- [x] 1.1 新增前端产品注册表（如 `web/src/products/registry.ts`）：`im` / `docs` / `calendar` 的 id、label、icon、adminSettingsPath、adminEnabled；工作台导航改为引用该表
- [x] 1.2 `useAdminNav` + 路由：`/admin/products` 总览；`/admin/products/im` 壳层二级侧栏（群规模 / 群权限 / 消息权限，**无**群组管理）；docs/calendar 占位页
- [x] 1.3 占位页文案：「组织级设置将在后续版本开放」；permission 码草案写入导航过滤；`pnpm build` + `pnpm typecheck`
- [x] 1.4 起草 `openspec/lanes/admin-product-settings/contract.md`（壳层无业务写 API；IM 子页合同可分期追加）；更新 `api-integration-board.md`

## 2. 群规模管控（-web → -api → -integrate）

- [x] 2.1 **Web**：群规模页 — 企业默认上限（5/10/30/50 + 自定义，默认 50）；用户规则集（部门+用户多选 + 上限）；群组规则集（本租户对话群选择器 + 上限）；取消/保存；store 临时数据
- [x] 2.2 **API**：Flyway `im_tenant_settings` + `im_group_size_user_rule` + `im_group_size_group_rule`；Admin GET/PUT；建群/加成员写路径按「群组规则 > 用户/部门规则 > 默认」强制；部门群不受默认/用户规则约束；codegen → diff 合入
- [x] 2.3 **Integrate**：去 Mock；超限拒绝；下调上限不踢人；`mvn compile` + 前端 build/typecheck；看板标记

## 3. 群权限管控配套能力 + Admin（公开群 / 部门群 / 导出）

- [ ] 3.1 **公开群**：`im_group` 可搜索标记；群设置开关；同租户搜索 + 直接加入；组织「禁止公开」时隐藏开关并关闭已公开（按 design D5）
- [ ] 3.2 **部门群最小模型**：`group_kind` + `dept_id`；创建入口（建群向导或部门页二选一落地）；退出受组织策略约束；成员全量自动同步可另开 change
- [ ] 3.3 **成员导出**：按策略（禁止 / 群主管理员 / 指定成员且为群主管理员）导出 CSV；字段对齐名片可见范围
- [x] 3.4 **Admin UI + API + 联调**：群权限三块表单（进群 / 部门群退出 / 导出）读写与工作台强制执行打通（Admin 读写已通；工作台强制执行随 3.1–3.3）

## 4. 消息权限管控（撤回 / 编辑）

- [ ] 4.1 **能力**：消息撤回、编辑 App API + WS fanout；`im_message` 撤回/编辑元数据；撤回占位；编辑保 `seq`；群主/管理员撤回豁免组织「不允许撤回」开关
- [x] 4.2 **Admin + 生效策略**：消息权限页（允许/不允许 + 时间窗枚举，默认 24h）；`GET /app-api/im/settings/effective`；工作台菜单显隐；写路径以后端为准（Admin + effective API 已通；撤回/编辑能力与菜单显隐随 4.1）
- [ ] 4.3 **联调收尾**：三块 IM 设置端到端；`openspec validate admin-product-settings-v1 --strict`；看板 done；docs/calendar 仍仅占位

## 5. 后续（本母 change 不实现，仅登记）

- [ ] 5.1 IM 完成后另立 `admin-docs-product-settings-*` / `admin-calendar-product-settings-*` 详规（本清单勾选表示「已立 change」，非本母 change 编码完成）
