# Illustrations

工作台插画资源目录。取图与调整约定见 [`docs/dev/workspace-illustrations.md`](../../../docs/dev/workspace-illustrations.md)。

- 来源：**Storyset**（[storyset.com](https://storyset.com/)），画风锁定 **Rafiki**
- 在官网统一 style / 主色 / 背景 / 图层后再下载 SVG 到本目录
- 免费使用须保留 Storyset 署名（见官网 FAQ）
- 前端目录真源：`web/src/utils/workspaceIllustrations.ts`（按场景分组；消息未选会话用 `MESSAGES_IDLE_ILLUSTRATIONS`）

## 当前文件（messages idle 池）

| 文件 | Storyset slug | 用途 |
|------|---------------|------|
| `messages-idle-chatting.svg` | chatting | 消息主区未选会话（随机池） |
| `messages-idle-messaging.svg` | messaging | 同上 |
| `messages-idle-conversation.svg` | conversation | 同上 |
| `messages-idle-group-chat.svg` | group-chat | 同上 |
| `messages-idle-mail.svg` | mail | 同上 |
| `messages-idle-coffee-break.svg` | coffee-break | 同上 |
| `messages-idle-celebration.svg` | celebration | 同上 |
| `messages-idle-nature.svg` | nature | 同上 |
| `messages-idle-online-world.svg` | online-world | 同上 |
| `messages-idle-social-dashboard.svg` | social-dashboard | 同上 |
| `messages-idle-taking-notes.svg` | taking-notes | 同上 |

换色 / 去背景：回 [storyset.com](https://storyset.com/) 打开对应 slug 的 Rafiki 页调完后覆盖同名文件。
