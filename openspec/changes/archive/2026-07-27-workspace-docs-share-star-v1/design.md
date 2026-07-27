## Context

- `/app/docs` 已有：个人文档库（Library）、最近、云盘 Drive；侧栏「与我共享」「星标」为 disabled +「即将推出」
- 现行鉴权：**仅 `owner_user_id` 可读可写**；规格明确 Library V1 不要求分享
- 日历已有对象级类比：`cal_calendar_share`（整日历 READ）；任务/邀请触达已走 `ImBotApi` + system Bot（`task-bot` / `org-assistant` / `calendar-bot`）
- 业务触达真源：Bot → `bot_dm`，禁止复活 `infra_notify` / Rail 铃铛

本母 change 规划 **文档对象级分享 ACL + 与我共享 + 星标 + docs-bot 触达**；实现拆 `-web` / `-api` / `-integrate`。

## Goals / Non-Goals

**Goals:**

- 钉死「分享 ≠ 换 Placement」：文档仍住在所有者容器；接收方只在「与我共享」可见
- 统一文档读写鉴权：`owner` ∪ `有效 share`
- 个人星标（偏好，非权限）
- 「最近」升级为个人访问足迹（含分享打开与失权/已删墓碑）
- 分享成功后 best-effort `docs-bot` 触达
- 前端侧栏两入口可用，可演示分享 / 打开 / 星标 / 最近墓碑闭环

**Non-Goals:**

- 链接分享、外链、部门/群组授权、文件夹继承 ACL、Wiki 空间成员
- Drive `FILE` / 整文件夹分享（后置）
- 快捷方式、「分享文档挂进我的云盘仍同一本体」、另存副本
- 回收站 UI / 定时硬删（本 change 只定义软删对 Recent/Share 的影响）
- CRDT 实时协作、评论
- 管理端文档治理 UI

## Decisions

### D1：分享对象粒度 = `doc_object`（非 Library 节点、非文件夹）

```text
DocObject（本体）── Placement（Library / Drive / 未来 Wiki）
       │
       └── doc_share（对象级 ACL，与 Placement 正交）
```

| 选择 | 理由 |
|------|------|
| 绑 `object_id` | 跨容器移动后授权仍有效；与「本体只住一处」一致 |
| 不绑 `library_node` | 节点是所有者树私有结构，接收方不应依赖对方树路径 |
| V1 可分享类型 | **仅 `RICH_DOC`**（含已迁入 Drive 的 RICH_DOC）；`FILE` 后置 |

**备选否决**：文件夹继承 ACL（云盘协作后续母 change）；Wiki 空间成员（知识库母 change）。

### D2：权限档位 `VIEW` | `EDIT`；管理权仅所有者

| 能力 | owner | VIEW | EDIT |
|------|--------|------|------|
| 读 body / 导出 MD | ✓ | ✓ | ✓ |
| 写 body（乐观锁） | ✓ | ✗ | ✓ |
| 改 title（若走独立 API） | ✓ | ✗ | ✓（与 body 同档） |
| 管理协作者（增删改权限） | ✓ | ✗ | ✗ |
| 删文档 / 移 Placement / 改树 | ✓ | ✗ | ✗ |
| 星标（自己的） | ✓（若可访问） | ✓ | ✓ |

- **不可**把自己分享给自己；**不可**降级/撤销所有者（所有者不是 share 行）
- grantee 必须是当前租户 ACTIVE 成员（`system-api` 校验）

**备选**：首版仅 `VIEW`——更安全但产品半残；见 Open Questions Q1。

### D3：表模型

```text
doc_share
  id, tenant_id
  object_id
  grantee_user_id
  permission            -- VIEW | EDIT
  granted_by_user_id
  + 公共字段（含 soft delete）

  UNIQUE (tenant_id, object_id, grantee_user_id) WHERE deleted = 0
  INDEX (tenant_id, grantee_user_id) WHERE deleted = 0   -- 「与我共享」列表
  INDEX (tenant_id, object_id) WHERE deleted = 0         -- 所有者管理协作者

doc_star
  id, tenant_id
  user_id
  object_id
  + 公共字段

  UNIQUE (tenant_id, user_id, object_id) WHERE deleted = 0
  INDEX (tenant_id, user_id, create_time DESC) WHERE deleted = 0

doc_user_visit   -- 「最近」真源（见 D7）
  id, tenant_id
  user_id
  object_id
  last_opened_at
  title_snapshot
  source                 -- OWNER | SHARED（可选）
  + 公共字段

  UNIQUE (tenant_id, user_id, object_id) WHERE deleted = 0
  INDEX (tenant_id, user_id, last_opened_at DESC) WHERE deleted = 0
```

- 星标前置条件：调用时用户对该 object **已有读权限**；撤销分享后星标行可残留，但列表 API **必须**过滤无权限项（或同时软删）
- 文档软删后：share 列表不得返回；visit 可保留并以 `DELETED` 墓碑展示（见 D7）
- `doc_object.last_opened_at`：迁移期可双写；Recent API 以 visit 为准
### D4：鉴权中心化（服务端）

在 docs-biz 内收敛单一入口（示意名 `DocAccessService`）：

```text
resolveAccess(userId, objectId) → NONE | VIEW | EDIT | OWNER
```

| API 族 | 规则 |
|--------|------|
| Library / Drive 树 CRUD、Placement 移动 | **仅 OWNER**（不变） |
| `GET/PUT document body`、export | `OWNER` 或对应 share |
| share 管理 API | **仅 OWNER** |
| shared-with-me 列表 | 当前用户为 grantee |
| star API | 当前用户 + 读权限 |

**禁止**：在 Controller 散落 `owner_user_id == me` 判断而不走 ACL。

**BREAKING 相对旧规格**：非 owner 持有效 share 时，`GET document` 成功（旧行为一律 forbidden）。

### D5：「与我共享」是查询视图，不是第四容器

- **不是** Placement；不写入接收方 `doc_library_node` / `doc_drive_item`
- 列表字段建议：`objectId`、`title`、`type`、`permission`、`ownerUserId`、`grantedByUserId`、`sharedAt`、可选 `starred`
- 排序：默认 `sharedAt DESC`（contract 冻结）
- 打开：`/app/docs?docId=`；UI 在 Shared 面板选中，**不**强行展开对方 Library 树

### D6：接收方「移出与我共享」= 自行离开

- 接收方可 soft-delete **自己的** `doc_share` 行（leave）→ 立即失去访问
- 所有者仍可在协作者列表看到历史？**V1：leave 后所有者列表不再显示该 grantee**（行已删）
- 所有者 revoke = 同一效果（owner 删行）

**不做**：仅「隐藏」但仍保留 ACL（易造成幽灵权限）。

### D7：「最近」= 个人访问足迹（含失权/已删墓碑）— 修订

#### D7.0 现状（as-is · 截图里那份列表）

| 项 | 现状 |
|----|------|
| 数据源 | `doc_object` where `owner_user_id = me`，按 `last_opened_at` |
| 软删 | `BaseDO.@TableLogic` → **已删除的自有文档不会出现在「最近」** |
| 分享 | 尚无；非 owner 打不开，也就进不了「最近」 |
| 字段 | 列表只有标题；**没有**「已删除 / 无权限」态 |

因此：截图里的「未命名文档 / 公司介绍 / move-me」都是 **仍存在且你拥有** 的文档，不是幽灵行。

#### D7.1 产品问题（你提的）

一旦有分享，会出现：

1. 我打开过别人分享给我的文档 → 「最近」要不要出现？
2. 对方后来把原文档删了 → 「最近」还要不要留着提醒「我曾经编过」？
3. 对方只是撤销分享 / 我 leave → 算不算「删除」？
4. 我在此期间「放到自己云盘 / 另存副本」→ 跟原分享文档是什么关系？

这不是小 UX，而是 **「最近」语义** 选型：内容目录 vs 个人足迹。

#### D7.2 市面常见解法（对照）

| 产品 | 「最近」本质 | 删了 / 失权后 | 分享 vs 放进自己空间 |
|------|--------------|---------------|----------------------|
| **飞书** | 最近使用足迹（可筛格式/所属人/打开时间）；「与我共享」另入口 | 删除进回收站（约 30 天）；删别人的内容会通知对方；协作者失权后通常无法再打开正文，列表侧多为「打不开 / 已失效」类体验（非永久保留正文） | 分享 ≠ 换所有者；可在条件允许下 **移动**；另存/复制才是新本体 |
| **Google Drive** | Recent = 个人访问活动 | Owner 进 Trash 后，协作者在 **彻底删除前** 往往仍可访问（Google 特例）；彻底删后链接失效；非 owner「Remove」多半是去掉自己侧入口，不一定删本体 | 「Add shortcut to Drive」= **快捷方式**，所有权不变；复制才产生新文件 |
| **Notion** | 侧栏 / 搜索近期 | 进 Trash ≈30 天；Trash 内 owner 仍可看；协作者一般随删除失权 | Workspace 页树 + 权限；不是「分享进我的网盘」模型 |

共性：

1. **最近 ≠ 所有权目录**；它更接近「我碰过什么」。
2. **分享 / 快捷方式 / 副本** 三者必须拆开，否则「放到自己云盘」会语义爆炸。
3. **软删有回收站窗口**；窗口内与窗口外对协作者是否可读，各家不同（Google 偏松，多数协作产品偏紧：删了就不可读）。

#### D7.3 我们的概念拆分（必须钉死）

```text
DocObject（唯一内容本体，有且仅有一个 owner）
    ├─ Placement：Library / Drive /（未来 Wiki）—— 放哪儿
    ├─ Share ACL：谁能看/编 —— 不改变 Placement / owner
    ├─ Star：个人偏好 —— 不改变权限
    └─ Recent visit：个人打开足迹 —— 不改变权限，可保留墓碑
```

| 用户口头说法 | 系统真实动作 | V1 |
|--------------|--------------|-----|
| 分享给你 | 写 `doc_share` | **做** |
| 出现在「与我共享」 | 按 grantee 查 share | **做** |
| 放到我的云盘（快捷方式） | 新 Placement/shortcut，owner 不变 | **不做**（与快捷方式同后置） |
| 另存为我的副本 / 复制到文档库 | **新建** `doc_object`，我是 owner，内容拷贝 | **不做**（proposal 已列非目标）；做了则与原 doc **无 ACL 继承关系** |
| 所有者把文档从 Library **移动**到自己的 Drive | 换 Placement，`object_id` 不变，share **仍有效** | 已有跨容器 move |
| 删除 | 软删 object（进「逻辑回收」）；协作者立即不可读正文 | V1 已有软删；**回收站 UI / 30 天恢复** 另切，本母 change 只定义对 Recent/Share 的影响 |

#### D7.4 场景矩阵（Recent × Share × Delete）

记：`A` = 所有者，`B` = 被分享人（曾打开过）。

| # | 事件 | B 的「与我共享」 | B 的「最近」 | B 点击打开 | A 的「最近」 |
|---|------|------------------|--------------|------------|--------------|
| S1 | A 分享给 B，B 未打开 | 有 | 无 | — | 不变 |
| S2 | B 打开分享文档 | 有 | **有**（来源=共享） | 正常编辑/阅读 | 不因 B 打开而改 A 的 `last_opened` 语义 |
| S3 | A 撤销 / B leave | 无 | **保留墓碑** `NO_ACCESS` | Toast：无权访问 | 不变 |
| S4 | A 软删文档（回收站窗口内） | 无（或标已删） | **保留墓碑** `DELETED` | Toast：文档已删除 | A 侧：见 Q6（建议也留墓碑或进回收站入口） |
| S5 | 文档彻底清除（硬删/过期清库） | 无 | 墓碑可保留至 TTL 后清洗，或立即去掉 | 已删除 | 同左 |
| S6 | B「另存副本」到自己的库（未来） | 原分享仍按 ACL | 原条目 + **新 object** 各自一条 | 副本走 B 的 owner 权限 | 无关 |
| S7 | B 仅加星标后失权 | 无 | 星标列表隐藏/失效；最近仍可有墓碑 | 同 S3/S4 | — |
| S8 | A 把文档 Library→Drive | 仍有（同 object） | 仍有，placement 元数据可变 | 正常 | 正常 |

**关键结论**：你希望的「删了还应该出现在最近，点开再提示已删除」= **Recent 必须是个人足迹表，不能只靠 `doc_object.last_opened_at` + 存活 join**。

#### D7.5 建议默认方案（写入本母 change）

**采用个人访问表 `doc_user_visit`（名可微调）：**

```text
doc_user_visit
  id, tenant_id
  user_id
  object_id
  last_opened_at
  title_snapshot          -- 打开时标题快照，删后列表仍能显示名字
  source                  -- OWNER | SHARED | …（可选，便于 UI 角标）
  + 公共字段
  UNIQUE (tenant_id, user_id, object_id) WHERE deleted = 0
```

行为：

1. **任意成功打开**（owner 或有效 share）→ upsert visit（刷新 `last_opened_at` + `title_snapshot`）
2. **「最近」列表** = 我的 visit，按 `last_opened_at` DESC；LEFT JOIN object/share，计算：

| `accessState` | 含义 | UI |
|---------------|------|-----|
| `OK` | 仍可打开 | 正常行 |
| `DELETED` | object 软删（或硬删后仅快照） | 灰显 +「已删除」；点击 → 明确提示，不进编辑器 |
| `NO_ACCESS` | object 在，但非 owner 且无有效 share | 灰显 +「无权限」；点击 → 提示被取消分享或已离开 |

3. **`doc_object.last_opened_at`**：可继续给 owner 侧辅助/兼容；**真源改为 visit 表**后，Recent API 不再「只查我拥有的未删除 object」
4. **墓碑保留**：建议 TTL **90 天**（或与未来回收站 30 天对齐后再洗）
5. 用户可「从最近移除」只删 visit 行，不删文档；**删除文档**仅 owner，与移除分列菜单项
6. **安全**：`DELETED` / `NO_ACCESS` 行 **不得**返回 body；列表最多 title_snapshot + state + 元数据列
7. **与我共享列表**：仍只展示 **当前有效** share（S4/S5 从 Shared 消失）；「曾经共享过」靠 Recent 墓碑表达，避免 Shared 变成坟场

#### D7.6 明确不做 / 易混点（答复「放到自己云盘」）

- V1 **没有**「把别人的分享文档搬进我的云盘仍指向同一本体」——那是快捷方式，后置。
- V1 **没有**「另存副本」。若用户自行复制正文新建文档：那是 **新 `object_id` + 新 owner**，原文档被删不影响副本；Recent 上会出现两条（原墓碑 + 新 OK）。
- 所有者 **移动** 自己的文档（Library↔Drive）≠ 分享给别人后对方拥有；share 跟着 `object_id` 走。

#### D7.7 实现分期（避免一次做爆）

| 阶段 | 内容 |
|------|------|
| **本母 change 必做** | 定模型 + `-web`/`-api`：visit 表、Recent 返回 `accessState`、分享打开写入 visit、删/撤后墓碑点击提示 |
| **可紧随** | 回收站 UI、30 天恢复、删文档通知协作者（docs-bot） |
| **后置** | 快捷方式 Placement、另存副本、硬删清理任务 |

### D8：触达 — `docs-bot` + `ImBotApi`（对齐日历/任务）

```text
owner 创建/更新 share
  → 持久化 doc_share（事务成功）
  → best-effort ImBotApi.send(
        botCode=docs-bot,
        target=SINGLE { tenantId, userId=grantee },
        card|text + /app/docs?docId=…,
        dedupeKey=docs-share:{tenant}:{object}:{grantee}:{perm} 或含版本后缀
     )
  → Bot 失败不回滚 share，不向客户端暴露 Bot 错误细节
```

- Flyway 种子：`im_bot` 行 `code=docs-bot`，`type=system`，显示名「文档助手」（与 `calendar-bot` 同模式）
- `docs-biz` **仅**依赖 `im-api`，禁止碰 `im_*` Mapper
- 卡片：优先 `generic.v1` 深链；若 interactive card 成本高，V1 允许纯文本 + URL（contract 写死一种）
- **撤销 / leave 不要求**发 Bot（避免骚扰）；后置可做

**备选否决**：用 `org-assistant` 发文档分享——职责混杂；新建 `docs-bot` 更清晰。

### D9：前端信息架构与「最近」列表列

```text
/app/docs 左栏快捷访问
  最近          ← 本切片：列表列 + 行操作菜单（见 D13）
  与我共享      ← 启用：列表主区
  星标          ← 启用：列表主区
空间
  我的文档库    （已有树 + 节点操作）
  云盘          （已有浏览器）
  知识库        （仍占位）
```

#### D9.1 「最近」列表列（对齐飞书主页「最近访问」，按现状裁剪）

主区「最近」从「仅标题行」升级为可扫读表格/多列表（窄屏可折叠次要列）：

| 列 | 字段（contract） | 说明 |
|----|------------------|------|
| 标题 | `title` / `titleSnapshot` + type 图标 | 墓碑灰显；可加「已删除」「无权限」徽标 |
| 位置 | `locationLabel` | 枚举文案：`我的文档库` / `云盘` / `与我共享` / `已删除` / `未知`（按 Placement + 是否 grantee 推导；Wiki 后置） |
| 所有者 | `ownerDisplayName`（+ `ownerUserId`） | 需 `system-api` 批量解析显示名；列表禁 N+1 |
| 创建时间 | `createTime` | object 创建时间；墓碑若 object 已硬删则用快照或隐藏该列 |
| 最近访问 | `lastOpenedAt` | visit 表 |
| （隐） | `accessState` | `OK` \| `DELETED` \| `NO_ACCESS` — 驱动样式与点击 |

**不做（本切片）**：飞书式「置顶 / 筛选 / 显示设置 / 卡片网格」——可后置；V1 列表 + 基础排序（最近访问 DESC）即可。

文档工具栏（打开正文后）：

- **分享**（仅 owner 的 RICH_DOC）：成员搜索 + VIEW/EDIT + 协作者列表 + 撤销
- **星标**：toggle（可读即可）
- **导出 Markdown**（已有，保持）

`-web`：store 临时数据撑 UI + 起草 `openspec/lanes/workspace-docs-share-star/contract.md`。

### D10：按面板的文档行操作菜单（环境贴合 · 按现状取舍）

原则：

1. **菜单随面板变化**（最近 ≠ 文档库 ≠ 与我共享 ≠ 云盘）
2. **同一动作语义固定**：`从最近移除` ≠ `删除文档`；`离开分享` ≠ `撤销他人权限`
3. **只做已有能力能支撑的项**；飞书菜单里其余项标后置，UI 可隐藏或 disabled+「即将推出」

#### D13.1 动作词典

| 动作 ID | 含义 | 权限前提 | 现状能力 |
|---------|------|----------|----------|
| `open` | 打开 | `accessState=OK` | ✅ |
| `remove_from_recent` | 只删 visit 行，文档本体不动 | 有 visit | 本切片 **新增** |
| `delete_document` | 软删 object（+ Library 节点或 Drive 项） | **仅 owner** | ✅ Library/Drive 已有；Recent 菜单可调同一删除流 |
| `share` | 管理协作者 | **仅 owner** + `RICH_DOC` | 本切片 **新增** |
| `star` / `unstar` | 星标切换 | 当前可读 | 本切片 **新增** |
| `export_md` | 导出 Markdown | 可读 | ✅ |
| `rename` | 改标题 | owner（树/云盘上下文） | ✅ Library/Drive |
| `move_in_library` | 文档库内移动 | owner + Library 放置 | ✅ |
| `move_to_drive` | Library → Drive | owner | ✅ |
| `move_to_library` | Drive → Library（RICH_DOC） | owner | ✅（若已有） |
| `leave_share` | 接收方放弃授权 | grantee | 本切片 **新增** |
| `download_file` | 下载 FILE | owner（后：可读） | ✅ Drive |
| `copy_link` | 复制 `/app/docs?docId=` | 可读或墓碑仍可知 id | 本切片 **薄能力**（前端 clipboard） |
| `create_copy` | 另存副本 | — | ❌ 后置 |
| `add_shortcut` | 快捷方式 | — | ❌ 后置 |
| `pin` / `offline` / `follow` | 置顶/离线/关注 | — | ❌ 后置 |

#### D13.2 「最近」行菜单（你图中红框场景）

```text
打开（或整行点击；墓碑则拦截提示）
──
分享                 ← 仅 owner && RICH_DOC && OK
复制链接             ← OK 或仍有 objectId
导出 Markdown        ← OK && RICH_DOC
星标 / 取消星标      ← OK
──
从「最近」中移除     ← 始终（有 visit 即可）；**不**删文档
删除…                ← 仅 owner && OK；二次确认；删后本行变墓碑或直接从最近消失（与 Q6 一致：建议删后变 DELETED 墓碑，用户仍可再「从最近移除」）
```

判断逻辑（服务端 + 前端双重）：

| 条件 | 从最近移除 | 删除文档 |
|------|------------|----------|
| 我是 owner，文档仍在 | ✓ | ✓ |
| 我是 grantee，文档仍在 | ✓ | ✗（无删除项） |
| 墓碑 DELETED / NO_ACCESS | ✓ | ✗ |

#### D13.3 其他面板菜单（本母 change 一并钉死，避免实现时乱加）

**我的文档库（树节点 · 已有为主 + 补分享/星标）**

```text
打开
重命名 / 树内移动 / 移动到云盘
分享（RICH_DOC）
星标
导出 Markdown
删除
```

不出现：`从最近移除`（不在最近上下文）；不出现副本/快捷方式。

**与我共享**

```text
打开
复制链接
星标
导出 Markdown
离开分享（移出与我共享）
```

不出现：删除文档、重命名、移动 Placement、分享管理（非 owner）。

**星标**

```text
打开
复制链接
取消星标
──
若 owner：分享 / 导出 / （可选）删除
若仅 grantee：离开分享（可选）/ 导出
```

**云盘（已有浏览器）**

```text
打开 RICH_DOC / 下载 FILE
重命名文件夹或项、移动、删除（owner）
RICH_DOC：分享、星标、导出、移回文档库（已有则保留）
FILE：本切片不分享
```

不在云盘菜单塞「从最近移除」（除非该行同时也是 recent 视图——我们不是同一视图）。

#### D13.4 打开正文后的顶栏「…」（跨面板统一）

与行菜单子集对齐，避免两套语义：

- 共通：星标、导出 MD、复制链接  
- owner：分享、删除（Library/Drive 上下文）  
- grantee：离开分享  
- 若从「最近」打开：额外「从最近移除」

### D11：Maven / 跨域

```text
docs-biz → system-api（成员 ACTIVE 校验）
docs-biz → im-api（ImBotApi）
禁止 docs-biz → im-biz / system-biz
```

若当前 `docs-biz` pom 未引入 `im-api`，在 `-api` 子 change 补上。

### D12：子 change 与验证

```text
workspace-docs-share-star-web         UI + Mock + contract
workspace-docs-share-star-api         Flyway + ACL + REST + docs-bot 种子 + ImBotApi
workspace-docs-share-star-integrate   去 Mock 联调
```

Schema 可单独 `docs-share-star-schema-v1`，或并入 `-api`（推荐并入以减少空表窗口）。

验证：`openspec validate`；`mvn compile`；`pnpm build` + `typecheck`；浏览器：A 分享给 B → B 在与我共享打开 → Bot 会话可见 → 星标 → 撤销后 B 不可读。

### D13：与「管理后台」无关

本能力是 **工作台 `/app` 产品面**；鉴权是文档 ACL，**不是** `sys_permission` RBAC。不新增 `/admin` 文档治理页（除非日后单独 change）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| EDIT + 无实时协作 → 频繁 `DOC_VERSION_CONFLICT` | UI 明确冲突提示；Q1 可降为仅 VIEW |
| 分享后仍以为进了对方「我的文档库」 | 文案/空状态说明；不写接收方 Placement |
| Bot 触达失败用户无感 | best-effort + 与我共享列表仍可见；日志可观测 |
| 星标残留在失权文档 | 星标列表 join 权限过滤；Recent 用墓碑态 |
| ACL 检查漏网导致越权 | 单测覆盖 owner/view/edit/none；集中 `DocAccessService` |
| Drive FILE 用户期望能分享 | UI 对 FILE 隐藏分享或提示「即将支持」 |
| Recent 墓碑泄露敏感标题 | 仅快照标题；TTL 清洗；禁止返回 body |
| 用户以为「最近里有 = 文件还在」 | 行内「已删除/无权限」态 + 点击拦截 |

## Migration Plan

1. 增量 Flyway：`doc_share`、`doc_star`、`doc_user_visit`、`docs-bot` 种子
2. 部署新 docs-biz + 前端；旧客户端无分享入口仍可工作；Recent 切换 visit 表后需一次性回填（可选：从现有 `last_opened_at` 为 owner 生成 visit）
3. 回滚：停用分享/星标 API 与 UI 回占位；表可保留；`docs-bot` 可 `status=0`

## Open Questions（已拍板 · 2026-07-27）

> 用户确认按建议默认开工；下列为冻结结论。

### Q1 — 首版是否开放 `EDIT`？

- **结论**：开放 `VIEW` + `EDIT`，写冲突走既有乐观锁 + Toast

### Q2 — Drive `FILE` 是否纳入本母 change？

- **结论**：**不纳入**；仅 `RICH_DOC`

### Q3 — Bot 触达是否与 ACL 同一切片必须交付？

- **结论**：同母 change 规划；与 `-api` 同交付（best-effort）

### Q4 — 接收方能否「移出与我共享」（leave）？

- **结论**：**能**（删自己的 share 行 = 放弃访问）

### Q5 — 「最近」是否包含分享文档 + 失权/已删墓碑？

- **结论**：**要**。`doc_user_visit` + `accessState`；「与我共享」仍只显示有效授权

### Q6 — 所有者自己删的文档，自己的「最近」要不要墓碑？

- **结论**：**要**；并提供「从最近移除」

### Q7 — 墓碑保留多久？

- **结论**：**90 天**自动清洗；用户可手动「从最近移除」
