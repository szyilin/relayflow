## Context

V1 已落地 `im_sticker_pack` / `im_sticker` 与 `type=sticker` 发送，但系统包仅 6 个 Unicode 占位，Composer 用 `i-lucide-sticker` 打开单列表。用户要求对齐飞书：系统 emoji 全集 + 最近常用、自定义图/GIF、设置（排序/批量删）、贴纸消息右键收藏。

## Goals / Non-Goals

**Goals:**
- 飞书式三栏面板：系统 emoji / 自定义 / 设置
- 最近使用：localStorage，按最近点击重排，最多两行（grid 6 列 → 12 个）
- 自定义表情：用户级图/GIF，落库 + infra_file；发送仍为 sticker 消息
- 从贴纸消息收藏到本人列表；右键项仅贴纸消息可见
- Composer 入口图标改为线框笑脸

**Non-Goals:**
- 表情商店 / 「+」下载第三方包
- 视频自定义表情
- 租户运营系统贴纸包管理端
- Reaction 快捷栏改造（仍用 Unicode）

## Decisions

### D1 · 系统 emoji 前端静态目录

- 不把 Unicode 全集写入 DB；`web/src/constants/im-system-emojis.ts` 维护常用子集（覆盖飞书默认量级：微笑/手势/心情等，约 100–200）
- 点选：**插入 draft**（非立即发 sticker）
- 最近使用：`localStorage` key `rf:im:recent-emojis:{userId}`，MRU，上限 12

**备选（否）**：继续扩大 Flyway 种子 → 迁移膨胀且与真贴纸混淆。

### D2 · 自定义表情独立表 `im_user_sticker`

```text
im_user_sticker
  id, tenant_id, user_id, file_id, name?, sort, source_message_id?,
  create_time, update_time, deleted
```

- 每用户上限 200；MIME 仅 `image/*`（含 gif）；单文件 ≤ 2MB
- 发送：`type=sticker`，`content_json` 引用 `userStickerId` 或复用 `stickerId` 命名空间（前缀区分）— **采用**：扩展 block 字段 `userStickerId`；校验走用户表；系统 pack sticker 路径保留

### D3 · API

| Method | Path | 说明 |
|--------|------|------|
| GET | `/app-api/im/sticker/mine` | 我的自定义列表（含 downloadUrl） |
| POST | `/app-api/im/sticker/mine` | `{ fileId, name? }` 上传确认后登记 |
| POST | `/app-api/im/sticker/mine/from-message` | `{ messageId }` 从贴纸消息复制 fileId |
| PUT | `/app-api/im/sticker/mine/reorder` | `{ ids: Long[] }` 全量排序 |
| DELETE | `/app-api/im/sticker/mine` | `{ ids: Long[] }` 批量软删 |
| POST | send | 支持 `userStickerId` 发 sticker |

### D4 · UI

- 重写 `ImStickerPicker.vue`：底栏 Tab（smile / heart / settings）
- 设置态：多选 + 删除 + 拖拽或上下排序（V1 用「移到最前/最后」按钮 + 批量删，避免引入 dnd 库）
- 消息右键：`isStickerMessage` 时追加「添加到表情」

### D5 · 切片

单 change 内：web UI → api → 联调；一次会话尽量交付可演示路径。

## Risks / Trade-offs

- [emoji 集不全] → 先交常用集，后续可扩静态文件
- [收藏他人贴纸权限] → 须为会话成员且源消息为 sticker 且有 fileId；纯 Unicode 系统 sticker 种子无 file 时不可收藏为自定义图
- [排序并发] → reorder 覆盖写 sort，最后写赢

## Migration Plan

1. Flyway `V0.1.0.62__im_user_sticker.sql`
2. 发布前后端；旧系统包 API 可保留不展示
3. 回滚代码即可；表可留

## Open Questions

- 无（与飞书截图对齐已拍板）
