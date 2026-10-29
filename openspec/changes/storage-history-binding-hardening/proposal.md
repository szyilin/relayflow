## Why

P1 扫描项 B3：上传按生效来源选配置，下载却优先取已保存的租户配置。探针已确认，保存但未启用的配置即可使上传走平台桶 A、历史下载走租户桶 B；现有 provider 名和 storage_uri 也不足以恢复被覆盖的历史 endpoint。

## What Changes

- 将“下一次上传的生效配置”与“已有对象的存储位置绑定”分开，上传会话创建时固定位置版本，确认、HEAD、下载与清理沿用同一绑定。
- 规划持久存储位置版本及 file / upload-session 引用；位置的来源、endpoint / bucket 等定位信息不能被普通保存覆盖，凭证轮换与部署路由映射按显式规则处理。
- 管理端前端先冻结保存、切换、引用保护和未解析历史记录提示，再接后端；保存草稿配置不影响当前上传或已有文件。
- 提供历史数据的 dry-run / 显式映射 / 幂等绑定与升级前检查；不能判定来源时返回可恢复错误，禁止按 provider 名或同名桶猜测。
- 保留已完成 Docker change 的 internal / public endpoint 解析，预签名之前应用受管路由，不把 localhost 或浏览器端口当作历史对象身份。
- **BREAKING**：被引用的位置不再能被普通配置保存改指向其他桶；不明确的历史文件不再静默尝试当前默认存储。

## Capabilities

### New Capabilities

无；归入现有基础设施存储规格。

### Modified Capabilities

- `infra`：存储位置版本、上传 / 下载来源一致性、引用保护、历史绑定与双地址兼容。

## Impact

- 类型：**用户可感知切片，单 change 前端优先**；涉及 `web/` 管理存储配置、infra-api / infra-biz、OSS starter 的运行时解析及 server 迁移 / 运维入口。`deploy/` 仅文档契约同步，不重写 Compose。
- 已取得用户明确许可，新增非破坏性 `V0.1.0.3` 与 CLI codegen diff；迁移及历史映射仅在隔离库和备份副本验证。不改历史 Flyway、版本号或清空文件记录。真实环境仍按维护窗口、备份及明确映射计划升级。
- 不引入新存储产品、Maven / npm 依赖或对象搬迁服务；既有对象 key、fileId、storage_uri 与业务引用保留。
- 自部署：无法从现存 provider / URI 唯一推断历史 endpoint，方案明确要求操作者补充原配置和确认映射；先预演与备份再切换，不能宣称全自动恢复。升级 / 回滚边界见 design。
- 前置先完成 A1 私有文件授权，新的位置解析不能成为绕过 ACL 的下载入口。本轮不做上传确认幂等、文件搬迁或 P2 项。
