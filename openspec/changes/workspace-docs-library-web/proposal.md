# 提案：工作台云文档 · 我的文档库 · 前端 lane（workspace-docs-library-web）

## Why

母 change [`workspace-docs-library-v1`](../workspace-docs-library-v1/proposal.md) 已拍板「我的文档库」MVP：TipTap Block JSON 真源、个人页面树、V1 仅 Markdown 导出。[`docs-schema-v1`](../docs-schema-v1/proposal.md) 已完成 `doc_*` 表与 Maven 模块，但 `/app/docs` 仍是空壳。

按 **前端优先** 纵向切片，需先交付可演示的工作台 UI 与 lane contract，让 `-api` 按契约实现 REST，避免后端先行无界面堆叠。

## What Changes

本 change **仅 `web/` 与 lane contract**；**不含 Java / Flyway / 后端 API 实现**。

- 起草 `openspec/lanes/workspace-docs-library/contract.md`（树、文档 body + `bodyFormat` + `contentVersion`、最近、MD 导出、错误码、curl 示例）
- `api/app/docs.ts` + Pinia docs store：**临时数据仅 store 内**（禁止常驻 `mocks/` 目录 import）
- `/app/docs`：侧栏「我的文档库」树 + 「最近」；云盘 / 知识库 / 与我共享 / 星标 **占位或 disabled**
- 接入 **TipTap**（可新增 npm 依赖）：路由 **懒加载** 编辑器 chunk；V1 最小块集见 design
- 新建 / 重命名 / 树内移动 / 删除 UI（对临时 store 数据）
- **Markdown 导出入口**（工具栏或「…」菜单）；`-web` 阶段可用客户端临时序列化演示，integrate 必须走 API
- 深链 `?docId=` 打开指定文档（contract 定稿）
- 验证：`pnpm build` + `pnpm typecheck`；浏览器可演示树、编辑、导出入口

## Capabilities

### New Capabilities

- （无新 capability 名；增量写入既有 `docs` 域 UI 需求）

### Modified Capabilities

- `docs`：工作台文档库 UI、TipTap 编辑器、MD 导出入口（前端 lane 增量）

## Impact

| 层 | 路径 | 变更 |
|----|------|------|
| 前端 | `web/` | `/app/docs` 页面、TipTap 组件、docs store、`api/app/docs.ts` |
| npm | `web/package.json` | TipTap 相关依赖（已拍板） |
| 契约 | `openspec/lanes/workspace-docs-library/contract.md` | 新建 lane contract |
| 看板 | `docs/dev/api-integration-board.md` | `workspace-docs-library` web → `ui_ready`（实现时更新） |

**不涉及**：根 `pom.xml`、`relayflow-module-docs-*`、Flyway、PostgreSQL。

## 非目标（本 change）

- 后端 REST 实现（`-api` lane）
- 去 Mock / 联调真实 API（`-integrate` lane）
- Word / PDF 导出
- 云盘、知识库、分享、星标真数据
- 图片上传块（无 infra 上传链路时可 disabled）
- 管理端文档治理

## 依赖与顺序

- **前置**：`docs-schema-v1` 完成（schema ready）
- **后续**：`workspace-docs-library-api` → `workspace-docs-library-integrate`
