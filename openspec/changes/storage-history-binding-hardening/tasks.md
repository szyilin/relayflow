单 change 前端优先；依赖 private-file-access-hardening 的下载授权。用户已明确批准非破坏性迁移及隔离库验证；实际业务环境映射须按维护窗口与原配置确认计划执行。本 change 不处理 P2 上传确认幂等 / 并发。

## 1. 前端（web/）— 先做

- [x] 1.1 阅读管理端 UI / 前端规范，先在既有 `openspec/lanes/infra-storage-config/contract.md` 冻结保存但未启用、生效切换、引用保护、同位置凭据轮换、未解析历史错误及权限语义；更新对接看板，不向 UI 返回 secret。
- [x] 1.2 在 `/admin/infra/storage` 实现配置操作影响提示与状态，区分新上传目标和历史位置，明确轮换影响与引用拒绝，采用 store 内临时夹具展示失败 / 未解析场景；运行 `pnpm --dir web build` / `typecheck` 并记录浏览器证据。

## 2. 后端 — 后做

- [x] 2.1 先按 AGENTS.md 取得新增 Flyway 许可，读 CONTRIBUTING / database / codegen 后确定增量表、引用列、索引与加密方案；备份并验证非破坏性迁移，CLI codegen 临时目录 diff 合入，不改历史迁移 / 版本号。
- [x] 2.2 实现不可由普通保存改定位的位置版本、Bootstrap 按需记录、配置保存与启用分离、文件 / 未过期会话引用保护及同位置显式凭据轮换；验证 tenant 隔离、无 secret 响应 / 日志及轮换故障恢复。
- [x] 2.3 上传会话在 PUT 前绑定位置，confirm HEAD 按会话绑定、文件继承引用；盘点并统一实际 GET / PUT / HEAD / 清理入口，授权先于下载解析，缺失绑定不回退当前默认，保留既有逻辑删除与原 key。
- [x] 2.4 在签名前适配受管 internal / public 路由并保留 bucket / region / key / 选定凭据；验证 localhost 端口变化不改变对象身份，独立存储不被平台地址误替换。
- [x] 2.5 实现历史 dry-run、固定范围与目标的显式映射、幂等分批 / 续跑及未解析错误；原配置缺失或多义只报告，未解析 pending 会话过期 / 重建；在副本验证不覆盖已有引用、不改 key / 对象字节、不输出秘密。

## 3. 联调与迁移演练

- [x] 3.1 去除本切片临时夹具并接真实 API；浏览器 `/admin/infra/storage` 和现有附件上传 / 下载入口验证 Bootstrap A + 未启用 tenant B、切换期间 confirm、位置 / 前缀改变、引用保护与凭据轮换，重启后旧文件仍可读。
- [x] 3.2 使用测试副本与明确确认的映射计划演练备份、维护窗口、增量迁移、分批中断恢复及严格读取启用；核对已绑定 / 未解析 / 异常数量，记录旧二进制不兼容的回退边界，更新部署说明、contract / 看板，不触碰未经授权的真实数据。
- [x] 3.3 运行相关自动化回归、`./mvnw -pl relayflow-server -am compile`、`pnpm --dir web build`、`pnpm --dir web typecheck`、适用 Docker 配置 / 启动回归及 `openspec validate storage-history-binding-hardening --strict`，保存定位一致性与无数据覆盖证据。

验收与隔离演练记录见 [verification.md](verification.md)，部署流程见 [storage-history-upgrade.md](../../../deploy/storage-history-upgrade.md)。
