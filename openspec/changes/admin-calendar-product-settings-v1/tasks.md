# Tasks: admin-calendar-product-settings-v1

> **骨架**。深化 design/Open Questions 后再 `/opsx:apply`。勿一次做完全栈。

## 1. 深化规格

- [ ] 1.1 拍板 Open Questions；补全 design 设置项清单与表结构
- [ ] 1.2 补全 delta specs（admin-calendar-product-settings / calendar / web-admin）；`openspec validate admin-calendar-product-settings-v1 --strict`

## 2. 实现（可拆子 change）

- [ ] 2.1 **-web**：替换占位页；表单 + store/Mock；产品注册表 `calendar.adminEnabled=true`
- [ ] 2.2 **-api**：Flyway + Admin GET/PUT + 工作台强制
- [ ] 2.3 **-integrate**：去 Mock；看板；`pnpm build` + `pnpm typecheck` + `mvn compile`
