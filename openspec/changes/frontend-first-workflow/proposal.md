# 提案：前端优先工作流（frontend-first-workflow）

## 背景

单人 AI 开发、无 Figma 原型时，**先做出可点击的前端界面** 再实现后端 API，比「先堆接口再对接」更符合迭代节奏。本 change **只改文档与 OpenSpec 规则**，不改运行时行为（统一登录见 `unified-login-slice`）。

## 范围

| 模块 | 内容 |
|------|------|
| `docs/dev/` | 新增 `frontend-first-workflow.md`；更新 vertical / parallel / api-integration-board |
| `AGENTS.md`、`.cursor/rules/` | 引用前端优先顺序 |
| `openspec/config.yaml` | tasks / apply 规则 |

## 非目标

- 重写已归档 change 的历史 tasks
- 取消 `-api`/`-web`/`-integrate` 命名

## 结果

新切片默认顺序：**`-web`（UI+Mock+contract 草案）→ `-api` → `-integrate`**。
