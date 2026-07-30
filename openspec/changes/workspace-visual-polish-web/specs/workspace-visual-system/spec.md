## ADDED Requirements

### Requirement: Color placement boundary

员工工作台视觉增强 MUST 把颜色用在身份、状态与空态表达上，MUST NOT 用高饱和色或大面积渐变污染画布、列卡片壳层、正文与常规工具栏线框图标。主色（`--ui-primary` / 用户主题色）继续负责按钮与选中态；模块识别色与主题色 MUST 分层使用。

#### Scenario: Chrome stays calm

- **WHEN** 用户查看工作台三列壳层（画布、Rail、列表栏、主区卡片）
- **THEN** 这些区域的背景与分割线 MUST 继续使用既有灰阶 `--ws-*` 分层
- **AND** MUST NOT 为列卡片整体施加装饰性彩色渐变或霓虹描边

#### Scenario: Color on people status and empty

- **WHEN** 界面展示默认字母头像、语义标签、主导航模块图标、空状态或业务状态点
- **THEN** 这些元素 MUST 按本规格使用 tint / 渐变 / soft 标签（见后续需求）
- **AND** 正文段落与多数线框工具图标 MUST 保持 muted / 默认文字色

---

### Requirement: Contrasting semantic tint tokens

工作台 MUST 在 `workspace.css`（或同等 `--ws-*` 真源）中定义明暗两套对照语义色 token：至少覆盖 info、success、warning、danger，以及身份类 official、bot、agent、external、coming-soon；每类 MUST 提供可用于 soft 底的 muted 变体。明暗切换时 MUST 保持同一 hue 族，仅调整亮度/透明度，避免观感上「换了一套色相」。

#### Scenario: Tokens exist for light and dark

- **WHEN** 开发者查看工作台 CSS token 定义
- **THEN** `:root` 与 `.dark`（或等效）MUST 均定义上述 `--ws-tint-*`（及 muted）变量
- **AND** `docs/dev/workspace-ui-tokens.md` MUST 列出用途说明

#### Scenario: No hard-coded rainbow in feature pages

- **WHEN** 业务页面需要语义标签或状态色
- **THEN** MUST 引用 `--ws-tint-*` 或封装组件，MUST NOT 在页面散落任意 hex 作为常规手段

#### Scenario: Theme switch keeps hue family

- **WHEN** 用户在浅色与深色主题间切换
- **THEN** 同一语义（如 warning、agent）的色相族 MUST 可辨认为同一语义
- **AND** 文字与 soft 底在两种模式下 MUST 保持可读

---

### Requirement: Deterministic gradient letter avatars

当成员无可用头像 URL 时，工作台 MUST 显示显示名首字（或既有 `avatarText` 规则）叠在双色渐变圆形底上。渐变色组 MUST 由稳定标识哈希选择（优先 `userId`，否则名称字符串），同一标识跨页面 MUST 得到同一色组。有有效头像 URL 时 MUST 显示图片，MUST NOT 再套渐变底。

#### Scenario: Same user same gradient

- **WHEN** 同一 `userId` 在 Rail、所有者芯片与列表中均无头像 URL
- **THEN** 各处字母头像 MUST 使用同一哈希色组渐变

#### Scenario: Photo takes precedence

- **WHEN** 成员存在可解析的头像 URL
- **THEN** 头像 MUST 显示该图片
- **AND** MUST NOT 在图片上叠加默认渐变底作为常态

#### Scenario: Readable on both themes

- **WHEN** 用户分别在浅色与深色主题下查看字母头像
- **THEN** 首字与渐变底 MUST 保持可辨对比
- **AND** MUST NOT 使用近灰无色相的纯中性底作为默认字母头像方案

---

### Requirement: Semantic badge component

工作台 MUST 提供可复用的语义标签封装（基于 Nuxt UI Badge 或等效），通过 kind/语义枚举映射到 `--ws-tint-*` soft 样式。至少支持：官方、机器人、智能体、外部、角色类、即将推出、以及 info/success/warning/danger。产品态「即将推出」MUST 使用该标签样式，MUST NOT 仅用 muted 纯文字冒充标签。

#### Scenario: Coming soon is a live tag

- **WHEN** 导航或入口标记功能尚未开放（如知识库「即将推出」）
- **THEN** UI MUST 渲染语义为 coming-soon 的 soft 标签
- **AND** 该标签在明暗主题下 MUST 仍可辨认为标签而非正文注释

#### Scenario: Identity tags beside names

- **WHEN** 消息列表、通讯录或会话头展示官方 / 机器人 / 智能体 / 外部等身份
- **THEN** MUST 使用语义标签组件（或同等 soft 样式）
- **AND** MUST NOT 为每个页面复制互不一致的硬编码 pill 色

---

### Requirement: Module tint on workspace rail

左侧主导航各模块 MUST 拥有固定的模块识别色（module tint）。未选中时图标 MUST 仍能靠 tint 区分模块；选中时 MUST 保持清晰（active 底 + tint 或与 primary 协调）。导航文字 MUST NOT 整行使用高饱和彩虹色。

#### Scenario: Unselected icons are distinguishable

- **WHEN** 用户查看 Rail 且当前不在某一模块
- **THEN** 该模块图标 MUST 呈现其模块 tint（非全站统一 muted 灰）
- **AND** 相邻模块 tint MUST 可区分

#### Scenario: Selected state remains clear

- **WHEN** 用户选中某一 Rail 模块
- **THEN** 该项 MUST 显示明确的选中态（既有 active 底或等效）
- **AND** 图标识别色 MUST 仍然可读

---

### Requirement: Unified empty state

工作台 MUST 提供统一空状态组件，包含可选轻量插画（线描或等效，主色 + 至多一个辅色）、标题、可选描述与可选行动按钮。列表无数据、主区无内容、设置占位分类等空场景 MUST 优先使用该组件，避免大块冷底仅留一行 muted 字且无视觉锚点。MUST NOT 用假数据填满空白来「假装丰富」。

#### Scenario: Empty list uses shared empty state

- **WHEN** 某工作台列表查询结果为空且非加载中
- **THEN** 区域 MUST 展示统一空状态（至少标题；推荐含插画或等效视觉锚点）

#### Scenario: Placeholder category not bare gray text

- **WHEN** 设置或功能入口处于占位「即将推出」且主区无业务内容
- **THEN** 界面 MUST 结合统一空状态与 coming-soon 语义标签（或同等活力标签）
- **AND** MUST NOT 仅显示无标签的灰色说明句作为唯一视觉

---

### Requirement: Visual system documentation

本变更落地的 token、渐变头像规则、标签 kind、模块 tint 映射与空状态用法 MUST 同步写入 `docs/dev/workspace-ui-tokens.md` 与 `docs/dev/workspace-ui-patterns.md`，作为后续页面实现真源。

#### Scenario: Docs match implementation

- **WHEN** 实现者完成视觉系统代码变更
- **THEN** 上述文档 MUST 包含新增 `--ws-tint-*`、头像哈希渐变约定、Badge kind 与 Rail tint 表
- **AND** 后续工作台页面 MUST 遵循文档而非页面私有色表
