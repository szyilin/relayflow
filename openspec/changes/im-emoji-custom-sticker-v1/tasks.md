## 1. Frontend picker & recent emoji

- [x] 1.1 Add static system emoji catalog module and recent-used helper (localStorage, max 12 / 2 rows)
- [x] 1.2 Rewrite `ImStickerPicker` with tabs: system emoji / custom / settings; composer icon → `i-lucide-smile`
- [x] 1.3 Wire draft insert for system emoji; send custom sticker; empty-state upload (image/gif only)
- [x] 1.4 Settings UI: multi-select batch delete + reorder controls
- [x] 1.5 Context menu: 添加到表情 only for sticker messages; toast on success/fail
- [x] 1.6 API client + store methods for mine list / add / from-message / reorder / delete / send userStickerId
- [x] 1.7 `pnpm build` + `pnpm typecheck`

## 2. Backend user stickers

- [x] 2.1 Flyway `V0.1.0.62__im_user_sticker.sql` + DO/Mapper (codegen or match existing pattern)
- [x] 2.2 Service: list/add/from-message/reorder/batch-delete; MIME + count limits
- [x] 2.3 Controller under `/app-api/im/sticker/mine*`
- [x] 2.4 Extend send path to resolve `userStickerId` into sticker content block
- [x] 2.5 Error codes; `./mvnw -pl relayflow-server -am compile`

## 3. Integrate & verify

- [x] 3.1 Connect web store to real mine APIs (no Mock)
- [x] 3.2 Manual path notes: `/app/messages` picker tabs, upload, collect, settings
- [x] 3.3 `openspec validate im-emoji-custom-sticker-v1 --strict`

### Manual verify (`/app/messages`)

1. Composer 笑脸图标 → 面板底部 Tab：系统 emoji / 自定义 / 设置
2. 系统 emoji：点选插入输入框；「最近使用」最多 12；刷新后仍在
3. 自定义：空态点「+」上传图/GIF；点选发送 sticker 消息
4. 设置：多选删除、单选上移/下移排序
5. 贴纸消息右键 →「添加到表情」→ toast；纯 Unicode 无 file 的贴纸不出现该项
6. 消息右键「回应」→ 快捷表情条出现在指针附近（不贴左栏）
