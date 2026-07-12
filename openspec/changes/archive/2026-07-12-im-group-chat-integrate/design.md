# 设计：群聊联调（im-group-chat-integrate）

## 做法

- 群聊 API 直接调用，不再 `isGroupApiUnavailable` 回退 Mock
- 群消息发送与单聊共用 `sendMessage` + WS
- 系统消息经 REST 拉取或 WS `message.new` 展示

## 验证

浏览器：`/app/messages` → 建群 → 发消息 → 邀请成员 → 查看系统消息
