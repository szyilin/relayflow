## Why

管理端「产品设置」已为云文档露出占位入口（`admin-product-settings-v1`）。IM 组织设置已交付，需另立 change 详规并实现云文档的组织级策略（对齐飞书文档管理类设置），替换占位页。

## What Changes

- 将 `/admin/products/docs` 从占位升级为可配置组织级云文档设置壳层与具体策略项
- 按需新增 `docs` 域租户策略持久化、Admin API、工作台强制执行
- 前端优先：`-web` → `-api` → `-integrate`（可再拆子 change）
- **不做**：改动 IM 产品设置；个人偏好（`user-preference`）仍不迁入本页

## Capabilities

### New Capabilities

- `admin-docs-product-settings`: 管理端云文档组织设置（导航、表单、权限码）与策略语义

### Modified Capabilities

- `docs`: 组织级策略读写与工作台侧强制执行（具体项在 design 深化）
- `web-admin`: 产品设置 docs 子路由由占位改为可配置页
- `admin-product-settings`: docs 产品由 `placeholder` 升为 `adminEnabled`

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `/admin/products/docs/**`；产品注册表 `docs.adminEnabled` |
| `relayflow-module-docs-*` | 策略表 / Admin API / 读路径强制（待 design 拍板） |
| Flyway | `docs_` 前缀策略表（若需要） |
| 前置 | 母 change `admin-product-settings-v1` 已归档或 IM 阶段完成 |

## 非目标

- 本骨架阶段不定死全部飞书对等字段；深化会话补 design/specs 后再 `/opsx:apply`
- 不实现日历组织设置（见 `admin-calendar-product-settings-v1`）
