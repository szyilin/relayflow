## 1. OpenSpec

- [x] 1.1 `openspec validate im-message-reactions-stickers-v1 --strict`

## 2. 前端先行（-web）

- [x] 2.1 消息 reaction 条 + 右键/快捷栏；store Mock toggle 与聚合展示
- [x] 2.2 右键「转发」+ 会话选择器；Mock 写入目标会话预览
- [x] 2.3 Composer 表情包面板（系统包 Mock）+ 气泡渲染 sticker
- [x] 2.4 起草 `openspec/lanes/im-message-reactions-stickers-v1/contract.md`；`pnpm build` + `pnpm typecheck`

## 3. 后端（-api）

- [x] 3.1 Flyway：`im_message_reaction` / `im_sticker_pack` / `im_sticker`；`im_message.type` 加 `sticker`；DO/Mapper diff 合入；系统包种子（可最小）
- [x] 3.2 实现 reaction toggle + `reaction.updated`、forward、sticker packs/send；列表字段 `reactions` / `forwardedFrom`
- [x] 3.3 单元或集成测试覆盖 toggle/撤回拒绝/转发拒绝 card；`./mvnw -pl relayflow-server -am compile`

## 4. 联调（-integrate）

- [x] 4.1 去 Mock，对接 contract API 与 WS；浏览器验证路径写明
- [x] 4.2 更新 api-integration-board（若适用）；全量校验通过
