## Why

Create Hub 与通讯录已有「添加外部联系人」弹窗骨架，但仍用 createHub 演示种子，且无拒绝申请、无通讯录入口联动。本切片把发起添加流做成可演示的 `-web` 真源（store），并扩展契约，供后续 `-api`。

## What Changes

- 契约：精确搜索、`POST request`、`reject`；更新 enrich contract「V1 不在范围」
- `stores/contactAdd.ts`（或扩 contacts）：搜索草稿、发出申请、已添加判断
- 增强 `WorkspaceAddExternalContactModal`；通讯录外部空态/顶栏入口
- 「新的联系人」支持拒绝（`-web` 本地改状态；API 预留）
- 看板 `workspace-contact-add-external` → ui_ready

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-contacts-directory`：发起添加外部联系人 + 拒绝申请（web 可演示子集）

## Impact

- 仅 `web/` + OpenSpec；不改 Java
