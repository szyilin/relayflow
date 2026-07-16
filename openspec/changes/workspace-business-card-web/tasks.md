## 1. 共享个人名片 UI

- [x] 1.1 新增 `WorkspaceBusinessCard`：封面区、叠压头像、显示名/企业、签名区、操作区；支持 `mode: self | peer`
- [x] 1.2 self：封面点击选图预览；签名可编辑；隐藏语音/视频与「给自己备注」
- [x] 1.3 peer：签名只读；备注与描述编辑弹层（备注名 + 描述）；消息接通；语音/视频占位 toast

## 2. 入口接线

- [x] 2.1 Rail `WorkspaceProfileCard`：移除「个性签名」菜单项；「我的个人名片」打开 self 名片（去占位）
- [x] 2.2 `/app/contacts`：用 `WorkspaceBusinessCard`（peer）替换内联迷你 popover；保留点人定位与发消息

## 3. Store / 契约 / 文档

- [x] 3.1 `stores/businessCard`（或等价）：签名/封面/备注本地暂存；页面不直接 mock import
- [x] 3.2 起草 `openspec/lanes/workspace-business-card/contract.md`（签名/封面/他人资料/备注字段与端点草案）
- [x] 3.3 更新 `docs/dev/workspace-ui-patterns.md`（个人名片 vs 账号菜单；通讯录弹层）与 `api-integration-board.md`

## 4. 验证

- [x] 4.1 `cd web && pnpm build && pnpm typecheck`
- [x] 4.2 浏览器：Rail → 我的个人名片（换封面/改签名）；通讯录点他人（只读签名、备注、消息、语音视频占位）
- [x] 4.3 `openspec validate workspace-business-card-web --strict`
