## Context

母 change `admin-product-settings-v1` 已交付产品设置壳层与 IM 组织策略；云文档仅为占位页「组织级设置将在后续版本开放」。

## Goals / Non-Goals

**Goals**

- 定义云文档组织级设置范围（对标飞书「文档」产品设置中组织可控项，取 V1 子集）
- 打通 Admin UI ↔ Admin API ↔ 工作台 docs 写路径强制
- 保持个人偏好与组织策略分层

**Non-Goals**

- 一次会话吞全栈；须 `-web` → `-api` → `-integrate`
- 群组管理、IM 设置项

## Open Questions（深化前拍板）

1. V1 首批设置项清单（分享范围默认、外链、水印、导出等）以飞书哪些页为准？
2. 策略存 `docs_tenant_settings` 单表 JSON，还是分项表？
3. 权限码：复用产品设置查询/写，还是 `docs:settings:*` 独立码？

## Decisions（骨架）

| ID | 决策 | 说明 |
|----|------|------|
| D1 | 路由 | 继续挂 `/admin/products/docs`，二级侧栏按设置分组 |
| D2 | 分层 | 仅组织策略；不编辑 `user-preference` |
| D3 | 交付 | 可再拆 `admin-docs-product-settings-web` / `-api` / `-integrate` |

## Risks

| 风险 | 缓解 |
|------|------|
| 设置项膨胀 | V1 锁最小集；其余另开 change |
| 与分享 ACL 重叠 | design 深化时画清「组织默认」vs「单文档 ACL」 |
