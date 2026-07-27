## Why

母 change `workspace-plus-create-v1` 已锁定工作台「+」创建枢纽。需要先交付可交互 UI 壳层与契约草案，后续 `-api` / 邀请域切片才能对接。

## What Changes

- Rail 头像旁「+」菜单：创建群组、添加企业成员、添加外部联系人、创建文档
- 「添加企业成员」按本地/临时 `canInvite` 显隐（默认对齐 `ADMIN_ONLY` → `isAdmin`）
- 创建群组：增强成员选择器 UI（组织内钻取 / 外部 / 我管理的群组）；创建仍可走既有 IM API
- 添加企业成员 / 外部联系人：弹窗骨架（四 Tab / 精确搜索），数据可 store 临时
- 创建文档：复用 `docs.createPage` 并跳转 `/app/docs`
- 起草 `openspec/lanes/workspace-create-hub/contract.md`；更新对接看板

## Capabilities

### New Capabilities

- （无独立新域；行为增量落在既有 capability）

### Modified Capabilities

- `workspace-create-hub`：本切片落地 Create Hub 菜单与四项前端路径（相对母 change delta 的可演示子集）

## Impact

- **仅 `web/`** + OpenSpec lane/看板；不改 Java
- 依赖既有 IM 建群、docs 创建；邀请/外部申请真 API 留给后续切片
