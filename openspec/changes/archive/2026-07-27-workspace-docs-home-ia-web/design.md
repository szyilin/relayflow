## Context

见母 change `workspace-docs-ia-redesign-v1/design.md`。本切片只做 web + contract 草案。

## Goals / Non-Goals

**Goals:** 主页 / Library 列表 / Drive 滑入 / 上传进云盘 / contract。

**Non-Goals:** 详情检视栏；VO 补齐后端（`-api`）；模板库实装。

## Decisions

沿用母 change R1–R4 与 D2–D4。OwnerChip 来自 `workspace-owner-chip-web`。

## Risks / Trade-offs

API 字段缺口用 store 临时拼装或省略列，contract 写明，`-api` 补齐。
