## Context

G1 群 Bot 目录只查 `type=system`。Agent carrier 为 `type=tenant` + `owner_agent_id`，provision 时已有 `ownerUserId`（用户 enablement + bot_dm），但 `im_bot` 未持久化主人用户 ID，目录也无法按主人过滤。System noop bot 仅作投递，不应出现在「添加机器人」。

## Goals / Non-Goals

**Goals:**

- Catalog 排除 system 投递 bot（`handler_kind=noop` 或全部 system，V1 采用：不返回 system）
- Catalog 返回当前用户作为主人的 agent carrier（`owner_agent_id IS NOT NULL` 且 `owner_user_id = 当前用户`）
- `im_bot.owner_user_id` 在 provision 写入；存量回填
- `addBot` 对 carrier 校验主人；非主人不可加

**Non-Goals:**

- 管理端配置可嵌入 bot 白名单
- 非主人授权他人代加
- 改变群主才能打开「添加机器人」入口（保留）
- IM 流式回复

## Decisions

1. **`im_bot.owner_user_id` 列**  
   - 与 `owner_agent_id` 并列；carrier 必填主人；system 为 null  
   - 理由：目录查询可直接 `WHERE owner_user_id = ?`，无需跨域读 agent_def  
   - 备选：仅靠 user enablement → tenant enablement 存在时 reach 过宽；跨域查 agent → 增加耦合

2. **V1 目录候选 = 仅本人 carrier**  
   - 不返回任何 `type=system`  
   - 理由：当前 system 全为 noop 投递，无入群服务能力

3. **add 权限**  
   - 仍 `requireOwner`（群主）  
   - 额外：若 bot 有 `owner_agent_id`，则 `owner_user_id` 必须等于当前用户  
   - 群主 ∩ 智能体主人 才能把该 bot 加进群

4. **存量回填**  
   - Flyway：从 `agent_def.owner_user_id` 按 `im_bot.owner_agent_id = agent_def.id` 更新；若无匹配则从 `im_bot_user_enablement` 取最早一条 user_id 作为近似

## Risks / Trade-offs

- [已加进群的 system bot 仍在成员列表] → 可保留；只是不能从新目录再添加；移除仍走原 remove  
- [回填失败留下 null owner] → catalog 不展示；可手工修数据或重新 provision  
- [BREAKING 目录] → 前端空态文案改为「暂无可用的智能体机器人」

## Migration Plan

1. Flyway `V0.1.0.57__im_bot_owner_user.sql`：加列 + 回填  
2. DO / provision / catalog / addBot  
3. 前端空态文案  
4. 回滚：忽略列；恢复旧 catalog 查询（不推荐与新前端混用）

## Open Questions

无。
