# 设计：聊天附件联调（im-message-file-integrate）

## 联调清单

1. 单聊：发 PNG → 列表缩略图 → 新标签打开
2. 群聊：发 PDF → 文件卡片 → 下载
3. 失败场景：超大文件、非法 fileId（toast 错误）
4. WS `message.new` 含 file 消息时对方实时展示

## 验证

```bash
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
```
