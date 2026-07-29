## Context

- Hub UI、org-invite、contact-add、member-picker-web 已交付
- contact-add-external-integrate 本会话完成
- member-picker：spec 允许会话+members 聚合；专用 managed API 非阻塞

## Goals / Non-Goals

**Goals:** 四项入口验收；看板 done；支撑母 change 归档。

**Non-Goals:** 新功能；实现 `GET /group/managed`（记为可选后续）。

## Decisions

1. **managed groups**：V1 保持 `memberPicker.loadManagedGroups` 前端聚合。
2. **验收路径**：`/app` Hub、`/app/contacts`、`/app/messages`、`/app/docs`、`/admin/system/invite-settings`、`/app/invite/join`。

## Risks

| 风险 | 缓解 |
|------|------|
| 群过多时聚合慢 | 已有 30 群上限；后续再上 managed API |
