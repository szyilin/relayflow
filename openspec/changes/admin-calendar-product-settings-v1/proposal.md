## Why

管理端「产品设置」日历入口仍为占位（`admin-product-settings-v1`）。工作台日历已交付个人多日历、邀约、整日历 READ 共享、RRULE、拖拽与任务图层，但对照飞书仍缺三块紧密相关的能力：

1. **组织面**：管理员可配置的日历产品设置，以及**全员日历**（管理端创建、可见范围自动订阅）
2. **工作台面**：侧栏将「共享 / 设置」收敛为日历行 `···` 菜单，并提供与飞书同构的**单日历设置**面板
3. **显示色分层**：全员/共享日历的**默认颜色**由所有者（或管理员）设定并映射到所有订阅者；各账号可**仅对本机视图**覆盖颜色，未覆盖者仍见默认色；无日历编辑权时仍允许改本机色

个人偏好（周起始、默认时长、`showTaskLayer` 等）继续由 `user-preference` 的 `settings.calendar` 承载，不迁入本页。

## What Changes

- 将 `/admin/products/calendar` 从占位升级为可配置组织级日历设置（含**全员日历**管理）
- 新增日历域：全员日历类型、可见范围自动订阅、订阅者本机显示色
- 工作台 `/app/calendar`：侧栏 `···` 菜单 +「日历设置」面板（基本信息 / 共享人；按角色读写）
- 前端优先；本 change 为**母 change**，实现按子切片 `-web` → `-api` → `-integrate`
- **不做**：公共日历（可搜可订）、五级默认权限 ACL 全集、组织外共享、CalDAV/导出、会议室日历

## Capabilities

### New Capabilities

- `admin-calendar-product-settings`: 管理端日历组织设置 IA、权限码、全员日历管理 UI 与 Admin API 语义

### Modified Capabilities

- `calendar`: 全员日历容器与自动订阅；本机显示色；单日历设置读写边界；组织策略强制（若有）
- `web-admin`: `/admin/products/calendar/**` 由占位改为可配置；产品注册表 `calendar.adminEnabled=true`
- `admin-product-settings`: calendar 产品由 `placeholder` 升为 `adminEnabled`
- `user-preference`: 明确边界——个人日历段与「单日历本机色」均不被管理端编辑；本机色不进 preference JSON

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `/admin/products/calendar/**`；`/app/calendar` 侧栏 `···` + 设置弹层；产品注册表 |
| `relayflow-module-calendar-*` | 全员日历 / 订阅或可见范围 / 本机色表；Admin + App API |
| Flyway | `cal_` 前缀增量（类型扩展、偏好/订阅表等，见 design） |
| 契约 / 看板 | 扩展 `workspace-calendar` 或新建 lane；登记子切片 |
| 前置 | `admin-product-settings-v1` 已归档；工作台日历 V1 / V1.1 已归档 |

## 非目标

- 公共日历（任意成员可建、组织内可搜订阅）— 可另立项
- 飞书五级默认权限（私密 / 游客 / 订阅者 / 编辑者 / 管理员）完整模型 — V1 仅 owner 管理 + READ 订阅；本机色例外
- 组织外日历、导出 ICS、CalDAV、会议室资源日历
- 把周起始等个人偏好迁入管理端
- 一次会话吞全栈；须按 tasks 子切片交付
