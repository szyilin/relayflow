## Context

母 change `workspace-docs-ia-redesign-v1` 要求跨 docs/task 的所有者芯片。名片能力已存在（`WorkspaceBusinessCard` + `useClickAnchoredPopover`）。

## Goals / Non-Goals

**Goals:** 可复用芯片；docs 三列表 + 任务负责人接线；点击不触发行打开。

**Non-Goals:** UserBasicDTO 扩头像（属 `home-ia-api`）；主页 IA / Drive 滑入（属 `home-ia-web`）。

## Decisions

- 显示名：解析真实昵称；当前用户可旁标「我」，禁止仅显示「我」
- 头像：`avatarUrl` prop → auth/contacts 推断 → 字头像
- 多负责人：最多 3 个 chip + `+N`

## Risks / Trade-offs

- contacts 未加载时仅有 userId → 字头像 + userId/昵称回退；名片仍可按 userId 打开
