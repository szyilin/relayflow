## Why

工作台已有 Rail「资料名片」菜单与通讯录迷你弹层，但缺少飞书式「个人名片」：封面背景、个性签名、本人/他人差异化编辑、他人备注，以及消息旁的语音/视频入口。个性签名仍停在 Rail 菜单占位，与名片展示面割裂；通讯录点人看到的卡片也远弱于参考形态。

## What Changes

- 新增共享 **个人名片** UI（封面区 + 头像叠压 + 姓名/企业 + 签名或备注区 + 操作按钮），本人与他人同一组件、按 `mode` 区分可编辑性。
- **封面背景**可点击替换（仅本人）；他人只读展示。
- 将 Rail 菜单中的 **个性签名** 迁入个人名片：本人可编辑；查看他人时签名只读。
- Rail「我的个人名片」打开上述个人名片（不再 toast 占位）；菜单中移除独立「个性签名」占位项。
- **通讯录**点成员改为展示同一套个人名片（替换当前迷你 popover）。
- 查看他人时：提供 **备注与描述** 入口（参考飞书备注弹层；本切片 store 本地/Mock）；操作区含 **消息**（已接通）+ **语音 / 视频**（显式占位）。
- 起草 `openspec/lanes/workspace-business-card/contract.md`，供后续 `-api` 持久化签名/封面/备注。
- 本切片 **不写 Java**（前端优先 `-web`）。

## Capabilities

### New Capabilities

- `workspace-business-card`: 飞书式个人名片（本人/他人模式、封面、签名、备注 UI、语音视频占位）的产品行为

### Modified Capabilities

- `workspace-profile-menu`: Rail 菜单去掉「个性签名」占位；「我的个人名片」改为打开真实个人名片而非占位 toast

## Impact

- **前端 `web/`**：新 `WorkspaceBusinessCard`（或等价）组件；`WorkspaceProfileCard` 菜单调整；`/app/contacts` 弹层替换；`workspace-ui-patterns.md`；contract 草案。
- **后端**：无（本 change）；持久化见后续 `workspace-business-card-api`（签名/封面属用户资料；备注属查看者对联系人的私有数据）。
- **回滚**：纯前端 revert；无 Flyway。
