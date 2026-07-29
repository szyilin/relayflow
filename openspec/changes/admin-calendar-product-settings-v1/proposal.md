## Why

管理端「产品设置」已为日历露出占位入口（`admin-product-settings-v1`）。IM 组织设置已交付，需另立 change 详规并实现日历的组织级策略，替换占位页。个人日历偏好继续由 `user-preference` 承载。

## What Changes

- 将 `/admin/products/calendar` 从占位升级为可配置组织级日历设置
- 按需新增日历域租户策略持久化、Admin API、工作台强制执行
- 前端优先：`-web` → `-api` → `-integrate`（可再拆子 change）
- **不做**：IM / 云文档组织设置；不把个人偏好迁入本页

## Capabilities

### New Capabilities

- `admin-calendar-product-settings`: 管理端日历组织设置（导航、表单、权限码）与策略语义

### Modified Capabilities

- `calendar`: 组织级策略读写与工作台侧强制执行（具体项在 design 深化）
- `web-admin`: 产品设置 calendar 子路由由占位改为可配置页
- `admin-product-settings`: calendar 产品由 `placeholder` 升为 `adminEnabled`
- `user-preference`: 明确边界——个人日历段不被本页编辑

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `/admin/products/calendar/**`；产品注册表 |
| `relayflow-module-calendar-*` | 策略表 / Admin API / 强制（待 design） |
| 前置 | `admin-product-settings-v1` IM 阶段完成 |

## 非目标

- 骨架不定死全部设置字段；深化后再 apply
- 不实现 docs 组织设置（见 `admin-docs-product-settings-v1`）
