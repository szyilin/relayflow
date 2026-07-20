# 设计：工作台云文档 · 我的文档库 · 前端 lane

## Context

- 母 change：[`workspace-docs-library-v1`](../workspace-docs-library-v1/design.md) D6/D7/D9
- 现状：`/app/docs` 占位；`docs-schema-v1` 已就绪，无 App API
- 约束：前端优先；遵循 [`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)；登录门禁 `/app/login`

## Goals / Non-Goals

**Goals:**

- 可演示的 `/app/docs`：Library 树 + Recent + TipTap 编辑 + MD 导出入口
- Lane contract 定稿，供 `-api` 实现
- Store 内临时数据撑满 UI 交互，integrate 时整体替换为 API

**Non-Goals:**

- Java / REST 实现；真实持久化；分享；云盘 / Wiki 功能；Word/PDF 导出

## Decisions

### D1：临时数据与 store

- Pinia `stores/docs`（或等价命名）持有：Library 树节点、文档对象（title、body、bodyFormat、contentVersion）、最近列表
- 页面与组件 **禁止** `import` 常驻 `mocks/`；种子数据可在 store 初始化或 action 内
- integrate 时删除临时种子逻辑，改调 `api/app/docs.ts`

### D2：TipTap 懒加载

- `/app/docs` 路由使用 **动态 import** 加载编辑器 chunk（如 `DocsEditor.vue`），避免 TipTap 进入主 bundle
- V1 最小块集（StarterKit 子集 + Link 等）：段落、H1–H3、有序/无序/任务列表、粗/斜体、代码块、引用、分割线、链接
- 「+ 插入」菜单骨架：未支持块 **disabled**，名称可预留（流程图、画板等）
- 图片块：无上传链路则 **disabled** 或不出现在 V1 菜单

### D3：保存形状（对齐 contract，本地模拟）

- 标题 inline 编辑；正文防抖 1–2s + 失焦触发 store 更新
- 本地维护 `contentVersion` 递增；为 integrate 预留冲突 UI 钩子（`-web` 可不演示冲突）

### D4：深链

- 采用 query：`/app/docs?docId={objectId}`
- 进入页读取 `docId`，选中树节点并加载编辑器；无效 id 显示空态或 toast

### D5：信息架构

```text
/app/docs?docId=
  左栏（WorkspaceShell #panel 或 docs 专用侧栏）：
    我的文档库（树：新建/重命名/移动/删除）
    最近
    与我共享 | 星标 | 云盘 | 知识库 → disabled / 「即将推出」
  主区：
    文档标题 + TipTap
    导出 Markdown（下载 .md）
```

### D6：Markdown 导出（-web 阶段）

- UI 文案：**「导出为 Markdown」**（强调非主存）
- `-web`：客户端 TipTap JSON → MD 临时序列化（如 `@tiptap/html` + turndown 或轻量自写 walker），触发浏览器下载
- contract 定义 API 形状：`GET /app-api/docs/documents/{objectId}/export?format=md`；integrate 替换为真实请求

### D7：树操作（本地）

- 新建：根或选中节点下创建 node + 空 `RICH_DOC`
- 重命名：改 linked object title
- 移动：改 `parentId`，防环（客户端校验）
- 删除：从树与 recent 移除（软删语义本地模拟）

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| TipTap 包体积 | 路由懒加载；扩展集收敛 |
| 本地 MD 导出与 API 导出不一致 | contract 写明基础块规则；integrate 以 API 为准 |
| 用户误以为数据已持久 | 刷新后若仅内存数据会丢失——可 localStorage 可选增强；integrate 前 README/空态提示「演示数据」 |

## Migration Plan

- 本 change 仅增前端文件与 contract；无 DB 迁移
- integrate：store 改 API；移除客户端假导出（若与 API 重复）

## Open Questions

| # | 项 | 决定 |
|---|-----|------|
| 1 | 深链 | **`?docId=`**（本 lane 定稿） |
| 2 | 图片块 | V1 **disabled**（无上传链路） |
