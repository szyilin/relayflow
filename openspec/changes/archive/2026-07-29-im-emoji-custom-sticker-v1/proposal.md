## Why

工作台表情入口目前只有库里种子的 6 个 Unicode「贴纸」，体验远低于飞书：无完整系统 emoji、无最近常用、无个人自定义图/GIF、无法从他人贴纸消息收藏。需要按飞书形态拆开「系统 emoji」与「自定义表情」，并补齐管理与收藏。

## What Changes

- Composer 表情面板改为飞书式底部 Tab：**系统 emoji** / **自定义表情** / **设置**
- 系统 emoji：前端完整目录；顶部「最近使用」最多两行（按最近选用重排）；点选插入输入框（非整条 sticker 消息）
- 自定义表情：用户上传 **图片/GIF**（禁视频）；空态「点击添加」；点选发送 `type=sticker` 消息
- 设置页：自定义表情排序、批量删除
- 仅对 **贴纸消息** 右键提供「添加到表情」；收藏后进入本人自定义列表
- Composer 触发图标由 `sticker` 换为线框笑脸（`smile`）
- 后端：用户自定义表情表 + CRUD/排序/从消息收藏 API；发送路径支持用户贴纸 ID
- 既有 `im_sticker` 系统种子包可保留兼容，UI 不再依赖其作为主 emoji 源

## Capabilities

### New Capabilities

- `im-user-sticker`: 用户自定义表情（图/GIF）上传、列表、排序、批量删除、从贴纸消息收藏

### Modified Capabilities

- `im-sticker-pack`: 面板能力扩展为系统 emoji + 自定义 + 设置；系统 emoji 不依赖 DB 种子
- `im-message-ux`: 右键菜单对贴纸消息增加「添加到表情」；composer 图标与面板交互

## Impact

- 域：`relayflow-module-im`（biz/api）、`web/` 消息页与 IM 组件
- DB：新表 `im_user_sticker`（Flyway `V0.1.0.62`）；文件走既有 `infra_file`
- API：`/app-api/im/sticker/mine*`；扩展 send 校验用户贴纸
- 回滚：回退代码；表可留；开发期 `0.x` 可破坏
