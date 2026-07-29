## ADDED Requirements

### Requirement: Message type sticker

The system MUST allow `im_message.type` value `sticker` in addition to existing types (`text`, `image`, `file`, `system`, `card`). Sticker messages MUST use structured `content_json` with a `sticker` content block.

#### Scenario: Persist sticker type

- **WHEN** a sticker message is saved
- **THEN** `type=sticker` and `content_json.blocks` contain a `sticker` block

### Requirement: Reaction realtime event type

The IM realtime channel MUST recognize `reaction.updated` as a first-class `domain=im` event type alongside `message.new`, `message.updated`, and `message.recalled`.

#### Scenario: Publisher emits reaction.updated

- **WHEN** a reaction toggle succeeds
- **THEN** `RealtimeEventPublisher` emits `domain=im, type=reaction.updated` to conversation User members

## MODIFIED Requirements

### 需求：消息内容块结构

系统必须使用结构化 `content_json`（Content Block 数组）存储消息内容；须支持 `text`、`file`、`mention`、`sticker` 块类型，并预留 `link`、`deeplink`、`card`、`actions` 扩展。地基期 Bot 触达允许 `text` + `deeplink` 元数据；可交互 card callback 属既有切片。

#### 场景：纯文本消息

- 给定 用户发送文本内容
- 当 消息持久化
- 那么 `content_json.blocks` 含至少一个 `type=text` 块

#### 场景：文件消息引用 infra

- 给定 用户发送附件且 `fileId` 属于当前租户
- 当 消息持久化
- 那么 `content_json.blocks` 含 `type=file` 且引用 `infra_file.fileId`
- 并且 消息 API 不得代理 multipart 文件字节

#### 场景：Sticker 块

- 给定 用户发送表情包
- 当 消息持久化
- 那么 `content_json.blocks` 含 `type=sticker` 且含 `stickerId` 与 `fileId`
