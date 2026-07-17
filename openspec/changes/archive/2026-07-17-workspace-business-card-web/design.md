## Context

现状两套「名片」互不共享：

| 面 | 组件 | 能力 |
|----|------|------|
| Rail | `WorkspaceProfileCard` | 自用账号菜单；个性签名 / 我的个人名片为占位 |
| 通讯录 | `contacts/index.vue` 内联 popover | 他人迷你卡：头像字、昵称、单按钮「消息」 |

飞书参考：封面背景 + 叠压头像 + 签名（本人）/ 备注（他人）+ 消息/语音/视频。后端尚无 `signature` / `cover` / 联系人备注字段；本切片仅前端 + contract 草案。

约束：前端优先；Nuxt UI；个人资料与偏好（`sys_user_preference`）分离；备注属「查看者 → 被查看者」私有数据，不是对方 profile。

## Goals / Non-Goals

**Goals:**

- 共享飞书式个人名片组件，`self` / `peer` 两模式。
- 封面可替换（self）；签名在名片内（self 可写，peer 只读）。
- Rail「我的个人名片」打开 self 名片；去掉菜单「个性签名」。
- 通讯录点人打开 peer 名片；消息已通；语音/视频占位；备注与描述可编辑（本地/Mock）。
- 起草 contract，标明后续 API 边界。

**Non-Goals:**

- 本切片不写 Java / Flyway。
- 不做真实语音/视频通话、状态（+状态）、个人链接/二维码、外部联系人。
- 不把 Rail 账号菜单（登录更多账号、设置、退出）并入个人名片。
- 不实现 `user-preference-integrate`。

## Decisions

### D1. 组件拆分：账号菜单 vs 个人名片

- **`WorkspaceProfileCard`**：继续做 Rail 账号入口（昵称/头像快捷编辑、菜单）。
- **`WorkspaceBusinessCard`**：飞书式个人名片，由「我的个人名片」与通讯录复用。

备选：把封面塞进 `WorkspaceProfileCard` → 与菜单职责冲突，放弃。

### D2. self / peer 可编辑矩阵

| 元素 | self | peer |
|------|------|------|
| 封面 | 点击替换（文件选择 → 本地预览；API 后走上传） | 只读 |
| 头像 | 可沿用既有更换（或名片内入口） | 只读 |
| 个性签名 | 可编辑 | 只读展示（无则空态/占位文案） |
| 备注名 / 描述 | 不展示（备注是对他人的） | 可编辑（弹层；本地 store） |
| 消息 | 可选隐藏（跟自己聊无意义）或隐藏 | 接通 `openDirectChat` |
| 语音 / 视频 | 不展示 | 占位 toast「功能即将推出」 |

### D3. 本切片数据策略（C 类偏好式临时本地）

- 签名 / 封面：`stores/businessCard`（或扩展 `profile`）+ localStorage，键含 `userId`；契约字段对齐后续 API。
- 备注：按 `(viewerUserId, targetUserId)` 存本地；**禁止**写进对方 profile。
- integrate 时删除临时本地真源，改 API。

### D4. 后续 API 边界（仅 contract，本切片不实现）

| 数据 | 建议归属 | 说明 |
|------|----------|------|
| `signature`、`coverFileId` | `sys_user` 或 profile VO | 本人可写；他人只读 GET |
| 他人资料 | `GET …/user/profile/{userId}` 或 `?userId=` | 租户内 ACTIVE 成员 |
| `remarkName`、`description` | 新表如 `sys_contact_remark` | `(tenant_id, owner_user_id, target_user_id)` 唯一 |

默认数据：签名/封面属用户资料扩展（读默认可空 = B/C 混合，无行即空）；备注属 C 类（无行即无备注）。

### D5. 通讯录接入

替换内联迷你 popover 为 `WorkspaceBusinessCard`（`mode=peer`）。列表仍可仅有 `avatarText`；名片优先用已有字段，缺封面/签名用默认/空，待 API 补齐。

### D6. 视觉

对齐工作台 token（`--ws-*`）；封面区固定高度；头像叠在封面与正文交界；操作区 peer 为图标按钮行（消息 / 语音 / 视频），非仅一条宽按钮亦可（实现择一，保持飞书感）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 本地签名/备注换设备丢失 | contract + 看板标明待 `-api`；UI 可提示「本地暂存」仅 DEV 可选 |
| 通讯录无真实 avatar URL | 继续 `avatarText`/占位；API 切片补 avatar |
| 与 Rail 名片两套入口混淆 | 文档区分「账号菜单」vs「个人名片」 |
| 备注误做成公开字段 | design/contract 明确 viewer-scoped |

## Migration Plan

- 纯前端上线；无 DB 迁移。
- 回滚：还原组件与 contacts 弹层。
- 菜单文案：去掉「个性签名」行；「我的个人名片」去「（占位）」。

## Open Questions

- self 名片是否保留「消息」按钮：建议 **隐藏**（默认不跟自己开单聊）。
- 封面默认图：纯色渐变 token vs 内置插画 —— 实现时选轻量默认即可。
