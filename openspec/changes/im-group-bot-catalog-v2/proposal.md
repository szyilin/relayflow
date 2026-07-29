## Why

「添加机器人」目录当前只返回 `type=system` 投递型 bot（多为 `handler_kind=noop`），既不可入群服务用户，又把 agent carrier（`type=tenant`）排除在外，导致新建智能体机器人搜不到。应改为：排除纯投递 system bot；仅展示当前用户作为主人的 agent carrier bot。

## What Changes

- **BREAKING（目录语义）**：`GET /app-api/im/group/bots/catalog` 不再返回 system noop 投递 bot；改为返回当前用户拥有的、可嵌入群的 agent carrier bot
- `im_bot` 增加并维护 `owner_user_id`（provision 时写入 agent 主人）；存量 carrier 回填
- Catalog / add 校验：agent carrier 仅主人可见可加；群主操作入口可保留，但候选集按主人过滤
- 前端 `ImAddGroupBotModal` 文案与列表适配（含空态）；可选轻量搜索（若列表仍短可仅客户端过滤）

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `im`: 群 Bot 目录与入群资格（过滤投递 bot；agent carrier 按主人可见）

## Impact

- **领域**：`relayflow-module-im`（biz + 可选 api DTO）、Flyway、`web/` 添加机器人弹窗
- **依赖**：agent provision 已传 `ownerUserId`；需落到 `im_bot.owner_user_id`
- **回滚**：回退代码 + 可选忽略新列；目录语义与旧客户端不兼容（旧 UI 期望 system 列表）
