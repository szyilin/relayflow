## 1. 母 change 锁定与文档

- [ ] 1.1 审阅并锁定产品决策：Create Hub 四项；邀请设置对齐飞书（OFF / ADMIN_ONLY / ALL_MEMBERS + 范围/有效期/审批）；四渠道 QR/链接/码/手机号；邀请实体推倒 redesign（消费时写 membership）
- [ ] 1.2 确认 Open Questions：共享链接人数上限（建议不限）；待审批入口位置；邀请码字符集；`user/invite` 兼容保留；「我管理的群组」角色规则
- [ ] 1.3 `openspec validate workspace-plus-create-v1 --strict` 通过
- [ ] 1.4 （可选）更新 `AGENTS.md`「下一优先」指向本母 change

## 2. 契约草案与看板

- [ ] 2.1 起草 `openspec/lanes/workspace-create-hub/contract.md`（Hub 菜单、capability、四项前端路径）
- [ ] 2.2 起草 `openspec/lanes/org-invite/contract.md`（settings CRUD、四渠道、consume/accept、capability；含 curl）
- [x] 2.3 起草/扩展联系人发起申请契约（精确搜索 + `POST request` + `reject`）
- [ ] 2.4 更新 `docs/dev/api-integration-board.md`：登记本母 change 与计划子切片行

## 3. 开子 change（按 design D1 顺序，每个独立 validate）

- [x] 3.1 开 `workspace-create-hub-web`（「+」菜单壳 + 四项跳转/弹窗骨架 + 成员选择器 UI；临时数据仅 store）
- [x] 3.2 开 `org-invite-settings-web` → 随后 `-api` → `-integrate`（管理端邀请设置页）
- [x] 3.3 开 `org-invite-channels-web` → `-api` → `-integrate`（工作台四渠道 + 加入流 + 迁移 NOT_JOINED）
- [x] 3.4 开 `workspace-contact-add-external-web` → `-api` → `-integrate`（搜索/发起/拒绝）
- [x] 3.5 开 `workspace-member-picker` 相关子 change（建群选人增强 + 必要时 managed-groups API）
- [ ] 3.6 开 `workspace-create-hub-integrate`（四项入口联调收口）

## 4. 组织邀请域验收闸门（子 change 完成后在母 change 勾选）

- [ ] 4.1 管理端：三种 invite_mode 切换后，工作台「添加企业成员」显隐与创建权限正确
- [ ] 4.2 四渠道：QR/链接/码/手机号均可加入（或审批流可测）；重置后旧凭证失效
- [ ] 4.3 非管理员：范围外部门拒绝；有效期过期不可用；需审批时未批不可 ACTIVE
- [ ] 4.4 注册/Bot 接受走新邀请实体；历史 `NOT_JOINED` 迁移后仍可激活
- [ ] 4.5 旧 `POST /admin-api/system/user/invite` 仍可用（委托）

## 5. Create Hub 与其它三项验收

- [ ] 5.1 「+」：创建群组（选人含组织内/外部/我管理的群组）→ 进群会话
- [ ] 5.2 「+」：添加外部联系人 → 精确手机/邮箱搜索 → 发起申请 → 对方可同意/拒绝
- [ ] 5.3 「+」：创建文档 → 文档库新文档并打开编辑
- [ ] 5.4 浏览器路径写明：`/app` Hub、`/app/contacts`、`/app/messages`、`/app/docs`、`/admin/system/invite-settings`

## 6. 归档

- [ ] 6.1 子 change 全部归档且主 specs 已同步后，`openspec archive workspace-plus-create-v1`（或 sync + archive 流程）
- [ ] 6.2 看板对应行 → done；更新 `AGENTS.md` 下一优先
