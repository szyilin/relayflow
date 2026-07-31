> **执行门禁**：想法已写入本 change，但 **禁止实现业务代码**，直至用户明确要求 `/opsx:apply`（或等价「按 tasks 开始实现」）。  
> 实现时必须 **严格按下列章节顺序**：先列表行与成员态基础 → 右键能力 → 分组栏 → 未读窗口 → `-api` → `-integrate`。不得跳步先做分组。

## 1. 规划收口（仅文档；可先勾）

- [x] 1.1 确认本批 scope（置顶 / inbox 双水位 / 窗口加载 / 旗标 / 免打扰 / 自定义标签 / 分组九类+编辑分组；不做移除会话、云文档/话题/已完成分组）
- [x] 1.2 `openspec validate im-conversation-pin-unread-v1 --strict` 通过
- [x] 1.3 用户明确下令开始实现后，再进入第 2 节（本勾选表示「开干许可」，非功能完成）

## 2. `-web`：列表行版式（基础 UI，无依赖后续能力也可先做）

- [x] 2.1 会话列表：身份类型标签移到用户名后；头像相对双行文本垂直居中
- [x] 2.2 `pnpm build` + `pnpm typecheck`（本节独立验证）

## 3. `-web`：置顶 + 旗标 + 免打扰（成员态临时数据）

- [x] 3.1 右键：置顶/取消置顶；置顶区样式区分 + 可折叠横条（折叠聚合未读）；折叠态接 preference（可先本地/临时）
- [x] 3.2 右键：旗标/取消；列表标题行右侧旗 + 聊天标题名后旗
- [x] 3.3 右键：免打扰/取消；静音铃铛 + 弱未读小红点；导航汇总排除 mute
- [x] 3.4 起草/更新 contract 中 pin / flag / mute / `settings.im.pinnedSectionCollapsed` 字段

## 4. `-web`：inbox 标记已读/未读 + 标签打标

- [x] 4.1 右键：标记已读 / 标记未读（固定露出 1；仅 inbox，不影响 `read_seq`）；store 临时
- [x] 4.2 右键：标签二级菜单（历史可滚动 + 底部新建）；用户标签库临时数据；会话行不展示自定义标签文案
- [x] 4.3 更新 contract：inbox mark、tag library / assign

## 5. `-web`：消息侧分组栏（依赖 3–4 的判定字段）

- [x] 5.1 分组栏九类筛选（@我仅未读 @；标签只能点具体子项；其余不区分已读）
- [x] 5.2 编辑分组弹层：展示/隐藏/排序，默认全开；偏好 `sidebarGroupOrder` / `sidebarGroupHidden`
- [x] 5.3 更新 contract 分组偏好；本节后 `pnpm build` + `pnpm typecheck`

## 6. `-web`：未读定位与窗口滚动

- [x] 6.1 打开未读：首条未读 +「以下为新消息」+ 剩余未读/回最新
- [x] 6.2 窗口滑动与回最新假滑动；进入推进 + 停留续报（前端行为）
- [x] 6.3 消息窗口 API contract（around/before/after/limit）；`pnpm build` + `pnpm typecheck`

## 7. `-api`：成员水位、置顶/旗标/免打扰、标签、偏好

- [x] 7.1 Flyway：`badge_seq`、`pinned_at`、`flagged`；规范 `mute_until`；`im_user_tag` + `im_conversation_user_tag`
- [x] 7.2 pin / flag / mute / tag / inbox mark API；列表字段支撑分组过滤
- [x] 7.3 进入推进 + 停留续报写 `read_seq`+`badge_seq`；`unread_count` 物化
- [x] 7.4 偏好：`pinnedSectionCollapsed` + 分组 order/hidden；`./mvnw -pl relayflow-server -am compile`

## 8. `-api`：消息窗口列表

- [x] 8.1 message list around/before/limit 硬上限
- [x] 8.2 发消息未读递增与 `read.updated` 回归；curl 验证

## 9. `-integrate` 与验证

- [x] 9.1 前端接真实 API，删除临时 pin/flag/mute/inbox/tag/窗口数据
- [x] 9.2 浏览器按能力从置顶→旗标→免打扰→inbox→标签→分组→跳未读走通
- [x] 9.3 `pnpm build` + `pnpm typecheck`；需要时 validate / compile
