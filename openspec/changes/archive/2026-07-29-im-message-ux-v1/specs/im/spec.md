## ADDED Requirements

### Requirement: Bot catalog group reply mention mode

`im_bot` MUST 持久化 `group_reply_mention_mode`，取值 `trigger` 或 `none`，缺省为 `trigger`。Bot Runtime 群回复 MUST 读取该字段执行平台 @ 策略（见 `im-bot-group-reply-mention`）。

#### Scenario: Default mode is trigger

- **WHEN** 新建或未显式配置的 bot 行
- **THEN** `group_reply_mention_mode` 为 `trigger`
