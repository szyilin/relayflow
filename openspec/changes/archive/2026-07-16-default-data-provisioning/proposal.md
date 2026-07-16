## Why

成员入企、租户创建、用户偏好等场景都会产生「默认数据」需求。若散落各处随意预插行，或造一个跨域上帝填充器，会与表前缀分域、禁止 `*-biz → *-biz` 冲突，且难以演进默认值。需要一份可复用的 A/B/C 惯例，指导后续实现（含用户设置）。

## What Changes

- 新增开发文档 `docs/dev/default-data-provisioning.md`：定义默认数据三类策略（必须物化 / 默认即无行 / 偏好读合并）。
- 约定生命周期钩子只派发 **A 类** ensure；禁止跨域 `DefaultDataService` 写遍各前缀表。
- 在 `docs/dev/architecture.md`（或相关索引）增加指向该文档的链接。
- **不**改业务表、**不**改运行时 API（本 change 为文档惯例）。

## Capabilities

### New Capabilities

- `default-data-provisioning`: 默认数据初始化与 ensure 策略（文档级行为约定，供后续 change 遵守）

### Modified Capabilities

- （无）不修改已归档产品域 REQUIREMENTS。

## Impact

- **文档**：`docs/dev/default-data-provisioning.md`；可选更新 `AGENTS.md` / `architecture.md` 文档地图。
- **代码 / Flyway / web/**：无。
- **回滚**：删除或 revert 文档即可。
- **关联**：为 `user-preference-api`（C 类）与既有根部门 ensure（A 类）、系统 Bot（B 类）提供统一分类语言。
