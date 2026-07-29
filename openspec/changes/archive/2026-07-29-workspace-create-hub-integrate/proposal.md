## Why

Create Hub「+」四项与子切片（邀请、外部联系人、选人器）已基本落地；需开收口切片核对联调、去掉「待 integrate」残留，并关闭母 change `workspace-plus-create-v1` 的交付闸门。

## What Changes

- 核对四项入口均走真 API / 既定路径（建群、邀成员、加外部、建文档）
- 「我管理的群组」确认 V1 使用 IM 前端聚合（专用 `GET …/group/managed` 可选延后，不阻塞）
- 更新看板：`workspace-create-hub` / `workspace-member-picker` → done；母 change 行 → done
- 浏览器路径写明；为母 change 验收与 archive 提供依据

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `workspace-create-hub`：去掉「子能力仍可草稿」例外；四项入口 MUST 以已对接 API / 既有 docs·im 路径为真源

## Impact

- **前端**：仅核对与必要小修；不重做布局
- **文档**：看板、workspace-ui-patterns、create-hub contract
- **不改**：Java（除非联调发现缺口另开修补）
