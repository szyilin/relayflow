# Tasks：admin-docs-files-storage-optimize-v1

> 母 change。**一次会话只做一组（Wave）**，通常 ≤10 条。顺序：W1 → W2 → W3；W4 可选另开。  
> 前端优先：每波 `-web`（UI + contract 增量）→ `-api` → `-integrate`。  
> 验证：含 web 须 `pnpm build` + `pnpm typecheck`；后端 `./mvnw -pl relayflow-server -am compile`；`openspec validate admin-docs-files-storage-optimize-v1 --strict`。

## 0. 范围确认

- [x] 0.1 审阅 design D1–D8 与飞书截图对齐点；确认 W3 主部门语义、W4 上限提示占位
- [x] 0.2 更新 `openspec/lanes/admin-docs-product-settings/contract.md` 增量大纲（W1 计量字段 / W2 配额页 / W3 deptRules）

## 1. Wave W1 — 文件 UX + 计量架构

- [x] 1.1 **web**：文件管理去标题下 objectId、所有者改 `WorkspaceOwnerChip`；所有者筛选改为成员搜索选人；转移/恢复新主改选人；`pnpm build` + `typecheck`
- [x] 1.2 **web**：存储用量列表成员列改 `WorkspaceOwnerChip`；搜索占位「姓名、邮箱或手机号」
- [x] 1.3 **api**：Flyway `doc_object.size_bytes`；Drive 注册写入；Admin files 读该字段；去掉逐行 `getFile`；用量 SUM 改 `size_bytes`；删除 ExtMapper `JOIN infra_file`
- [x] 1.4 **api**：`FileApi` 批量查询（若缺）+ 历史 `size_bytes` 回填入口/任务；`mvn compile`
- [x] 1.5 **integrate**：回填后文件列表 FILE 有大小；用量与列表一致；无 docs→infra 表 JOIN；浏览器验 `/admin/products/docs/files` 与 `/storage`

## 2. Wave W2 — 飞书式用量 + 成员上限页

- [x] 2.1 **web**：用量页飞书化（已用/上限/占比/查看云文档）；主按钮进 `/admin/products/docs/storage/quota`；成员 Tab：租户默认 GB + 白名单选人（chip）；去掉「仅 Modal」为唯一编辑态
- [x] 2.2 **api**：usage 补 `avatarUrl`/`deptName`；keyword 含邮箱；quota GET/PUT 保持兼容并支持 GB 语义文档化；白名单 ≤20 校验
- [x] 2.3 **integrate**：保存租户默认/成员白名单后用量行上限与触顶行为正确；权限码不变

## 3. Wave W3 — 部门规则与优先级

- [x] 3.1 **web**：上限管理「部门」Tab（选部门 + 不限制/N GB）；文案优先级「成员 > 部门」
- [x] 3.2 **api**：`doc_storage_quota_dept_rule` + 生效优先级成员>部门>租户；触顶用新 effective
- [x] 3.3 **integrate**：无成员规则时部门上限生效；有成员白名单时覆盖部门

## 4. Wave W4（可选 / 可另开 change）

- [x] 4.1 「上限提示设置」占位或最小实现；用户组白名单明确后置说明
- [x] 4.2 看板 `api-integration-board.md` 更新；母 change 全部勾选后 `openspec validate --strict` 并考虑 archive
