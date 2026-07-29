# im-bot-group-reply-mention Specification

## Purpose

Platform policy for auto-@trigger user on group bot replies.

## Requirements

### Requirement: Platform group reply mention policy

群会话中由 Bot Runtime 发出的回复消息，在机器人配置为 `group_reply_mention_mode=trigger`（默认）且存在触发用户时，MUST 由 **IM 平台**在落库前向消息内容插入对该触发用户的结构化 `mention`（`subjectType=user`），MUST NOT 依赖智能体 / LLM 输出决定是否 @。配置为 `none` 时 MUST NOT 自动插入该 @（用于群助手广播、定时整理等）。`bot_dm` / 单聊机器人会话 MUST NOT 因本策略插入 @。

#### Scenario: Interactive group bot replies with @trigger

- **WHEN** 用户在群内 @ 某机器人且该 bot 的 `group_reply_mention_mode` 为 `trigger`
- **AND** Bot Runtime 产生文本回复
- **THEN** 持久化消息内容包含对该用户的 mention 块，且正文为平台包装后的结果（智能体仅提供正文）

#### Scenario: Broadcast bot does not auto-mention

- **WHEN** 群助手类 bot 的 `group_reply_mention_mode` 为 `none`
- **AND** Bot Runtime（或等价会话内 bot 发送路径）发出文本
- **THEN** 消息 MUST NOT 因本策略自动 @ 任一用户

#### Scenario: Agent cannot opt out of trigger mention

- **WHEN** `group_reply_mention_mode=trigger` 且存在 triggerUserId
- **THEN** 即使 Agent 返回的纯文本不含 @，平台仍 MUST 插入 mention
