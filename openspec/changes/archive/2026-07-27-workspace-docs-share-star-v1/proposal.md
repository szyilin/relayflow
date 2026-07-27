## Why

工作台 `/app/docs` 侧栏「与我共享」「星标」仍是「即将推出」占位；文档库与云盘已可用，但文档仍是 **仅所有者读写**，无法把一篇文档交给同事查看/协作，也无法收藏常用文档。分享是协作产品的基础能力，且触达应走已拍板的 **Bot / `bot_dm`** 路径，不宜临时用铃铛或旁路表硬塞。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。Schema 可 `[平台]` 先行或随 `-api`。

1. **文档对象级 ACL**：所有者可将文档分享给租户内指定成员（授权 `VIEW` / `EDIT`）；被授权人出现在「与我共享」；撤销后立即失去访问
2. **「与我共享」入口**：侧栏可用；列表展示他人分享给我的文档（含分享者、权限、时间等元数据）；打开走既有 `?docId=` 深链，**不**写入接收方的 Library 树
3. **星标**：用户可对「自己可访问」的文档加/取消星标；侧栏「星标」列表；星标是个人偏好，不是 ACL
4. **「最近」升级为个人访问足迹**：`doc_user_visit`；含分享打开；失权/已删墓碑；列表列含位置/所有者/创建时间/最近访问；「从最近移除」≠ 删除文档（owner 才可删）
5. **按面板的行操作菜单**：最近 / 与我共享 / 星标 / 文档库 / 云盘各自贴合环境（见 design D10）；不做副本/快捷方式/置顶/离线/关注
6. **分享触达**：`docs-bot` + `ImBotApi.send`（`SINGLE`）向被分享人发 bot_dm（卡片或文本 + 深链）；失败 **best-effort**，不回滚 ACL
7. **权限模型修订**：`GET/PUT /documents/{id}` 从「仅 owner」改为「owner **或** 有效 share grant」；Library 树 / Drive 树仍仅 owner
8. **看板与 lane contract**：登记 `workspace-docs-share-star` 切片

**BREAKING（行为）**：原先「非 owner → 一律 DOC_FORBIDDEN」改为 ACL 判定；owner-only 树 API 不变；「最近」从「仅自有存活文档」改为「个人足迹（可含墓碑）」。

### 非目标（本母 change / V1）

- 组织内链接分享、公开外链、密码链接
- 按部门 / 群组批量授权；文件夹继承 ACL；知识库空间成员
- 云盘 FILE / 整文件夹分享（可后置；design 写清边界）
- 快捷方式 Placement、「把分享文档挂进我的云盘仍指向同一本体」
- 接收方「另存副本 / 复制到我的文档库」（另存会产生新 `object_id`，与原分享脱钩——后置）
- 回收站 UI / 定时彻底删除任务（软删行为与 Recent 墓碑在 design 定义；恢复 UI 另切）
- 实时多人光标 / CRDT；评论系统
- 管理端文档治理 / 审计后台 UI（服务端审计字段可预留）

## Capabilities

### New Capabilities

- （无独立新 capability 文件名；行为并入既有 `docs` / 增量 `im`）

### Modified Capabilities

- `docs`：对象级分享 ACL、「与我共享」列表、星标、个人访问足迹「最近」（列信息 + 移除/删除分流 + 墓碑）、按面板行菜单、文档读写鉴权从 owner-only 扩展为 ACL
- `im`：平台 Bot 目录增加 `docs-bot`（系统 Bot · 文档分享触达）；用法对齐 `task-bot` / `org-assistant`

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway `V0.1.0.x` | `doc_share`、`doc_star`、`doc_user_visit`；`im_bot` 种子 `docs-bot` + 租户 enablement |
| 后端 | `relayflow-module-docs-*` | Share/Star 服务；文档鉴权改为 ACL；依赖 `im-api`（`ImBotApi`）+ `system-api`（成员校验） |
| IM | `relayflow-module-im-*` | 仅种子 Bot；无新写模型（走既有 `ImBotApi`） |
| 前端 | `web/` `/app/docs` | 启用「与我共享」「星标」面板；分享对话框；星标切换 |
| 契约 | `openspec/lanes/workspace-docs-share-star/contract.md` | `-web` 起草 |
| 看板 | `docs/dev/api-integration-board.md` | 登记切片 |
| 回滚 | 卸 share/star API + UI 回占位；表可保留空；停用 `docs-bot` |

### 子 change 建议拆分

```text
docs-share-star-schema-v1          [平台] 两表 + docs-bot 种子（可并入 -api）
workspace-docs-share-star-web      UI + Mock + contract
workspace-docs-share-star-api      REST + ACL + ImBotApi
workspace-docs-share-star-integrate 去 Mock 联调
```
