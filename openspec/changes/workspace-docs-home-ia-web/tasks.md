## 1. 主页与导航

- [x] 1.1 中栏改为：主页 / 我的文档库 / 云盘› / 知识库占位；默认 `home`+`recent`
- [x] 1.2 主页：快捷卡（新建 / 上传进云盘 / 模板 toast）+ Tab（最近|与我共享|星标）+ 统一表布局
- [x] 1.3 统一表使用 OwnerChip；列对齐母 change

## 2. 文档库与云盘

- [x] 2.1 文档库主区树形缩进列表 + 行操作 + 打开编辑返回；窄宽截断
- [x] 2.2 云盘悬停›、滑入中栏树、右侧 listing（所有者/改时）；返回云文档
- [x] 2.3 主页上传 → driveExplorer + 拉起上传

## 3. 契约与验证

- [x] 3.1 起草 `openspec/lanes/workspace-docs-ia-redesign/contract.md`
- [x] 3.2 更新看板 web=`ui_ready`（owner-chip + ia-redesign）
- [x] 3.3 `cd web && pnpm build && pnpm typecheck`
- [x] 3.4 `openspec validate workspace-docs-home-ia-web --strict`
