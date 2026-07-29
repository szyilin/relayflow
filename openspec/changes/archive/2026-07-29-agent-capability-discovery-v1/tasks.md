## 1. 元数据与 Catalog

- [x] 1.1 扩展 `SkillDescriptor`：`domain`、`tier(HOT|DEFERRED)`；现有 `task.*` / `im.*` BuiltinSkill 补齐元数据
- [x] 1.2 Catalog API（及前端类型）按域分组或返回 domain/tier 字段，供分组展示

## 2. 热路径包与装配

- [x] 2.1 实现能力包注册表：至少 `im-carrier`、`workspace-default`；`im-carrier` 含三个 `im.read_*`
- [x] 2.2 `AgentBuilder` / Carrier inbound：按 Channel 合并热路径包；**移除**对 `im.read_*` 的硬编码强制 merge
- [x] 2.3 AgentDef：capability profile（hotPacks + deferredDomains）持久化与读写 VO；兼容仅有 `skillIds` 的旧数据

## 3. 二级发现 Meta 工具

- [x] 3.1 BuiltinSkill：`capability.search` — 仅在授权域内检索，返回 code/domain/description/schema 摘要
- [x] 3.2 BuiltinSkill：`capability.use` — 白名单 + ACL 后分发；热路径已有 code 纠偏；IM 读保持会话绑定
- [x] 3.3 Runtime 工具列表 = 热路径 ∪ Meta（当存在 deferredDomains 或 Channel 需要时）；Deferred 默认不扁平注册
- [x] 3.4 Builder 注入已授权域短摘要到 system 附加段

## 4. 前端最小对接与验证

- [x] 4.1 技能目录 UI/类型支持 domain 分组展示；Agent 保存兼容 profile 字段（UI 可先只读或简单多选）
- [x] 4.2 `./mvnw -pl relayflow-server -am compile`；`cd web && pnpm build && pnpm typecheck`；`openspec validate agent-capability-discovery-v1 --strict`
