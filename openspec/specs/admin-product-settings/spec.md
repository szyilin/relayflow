# admin-product-settings Specification

## Purpose

管理端「产品设置」：组织级基础产品目录（云文档 / 即时消息 / 日历）、共享产品图标注册表，以及与个人偏好的边界。

## Requirements

### Requirement: Admin product settings catalog

管理端 SHALL 提供「产品设置」入口，列出当前企业可配置的基础产品。第一版产品目录 MUST 包含 **云文档**、**即时消息**、**日历**；每个条目 MUST 展示统一产品图标与名称，并导航至该产品的组织设置页（或占位页）。

#### Scenario: Open product settings

- **WHEN** 具备相应管理权限的管理员打开产品设置入口
- **THEN** 系统展示至少云文档、即时消息、日历三个产品项
- **AND** 每项使用产品图标注册表中的图标与标题

#### Scenario: Enter IM settings

- **WHEN** 管理员点击「即时消息」
- **THEN** 进入即时消息组织设置壳层，并可导航至群规模管控、群权限管控、消息权限管控
- **AND** MUST NOT 展示「群组管理」入口（本版不做）

#### Scenario: Enter docs settings

- **WHEN** 管理员点击「云文档」
- **THEN** 进入云文档管理壳层（权限与分享 / 文件管理 / 存储空间）
- **AND** MUST NOT 再展示整页「组织级设置将在后续版本开放」占位作为唯一内容

#### Scenario: Enter calendar settings

- **WHEN** 管理员点击「日历」
- **THEN** 进入可配置的日历组织设置（含全员日历管理能力，见 `admin-calendar-product-settings`）
- **AND** MUST NOT 再仅展示「组织级设置将在后续版本开放」占位

### Requirement: Shared product icon registry

前端 MUST 提供单一产品图标/元数据注册表，供工作台主导航、管理端产品设置入口及其他复用处引用。更换某一产品图标时，MUST 仅需修改该注册表（及相关资源），不得在多个页面硬编码不同图标名。

#### Scenario: Workspace and admin share icons

- **WHEN** 工作台导航与产品设置目录渲染同一产品（如即时消息）
- **THEN** 两者使用同一注册表条目的 `icon` 字段

#### Scenario: Change one product icon

- **WHEN** 维护者更新注册表中 `im`（或等价 id）的 icon
- **THEN** 所有引用该条目的 UI 展示新图标，无需逐页修改

### Requirement: Org product settings vs user preference

产品设置 MUST 仅表达 **当前组织** 对该产品的整体策略或管理能力。用户头像菜单中的个人偏好 MUST 继续仅影响该用户在当前企业内的使用体验，MUST NOT 被产品设置页直接编辑个人偏好字段。

#### Scenario: Boundary

- **WHEN** 管理员修改即时消息组织策略（如默认群人数上限）
- **THEN** 该变更作用于租户内相关 IM 行为
- **AND** MUST NOT 改写任意用户的 `user-preference` 文档

### Requirement: Admin product settings routes

产品设置相关管理端路由 MUST 使用 `/admin` 前缀，建议挂载于 `/admin/products/**`，并纳入管理端导航与 permission 门禁。

#### Scenario: Unauthenticated redirect

- **WHEN** 未登录用户访问 `/admin/products` 或其子路径
- **THEN** 前端按既有管理端规则重定向至登录

#### Scenario: Non-admin denied

- **WHEN** 已登录但 `isAdmin=false` 的用户访问产品设置路由
- **THEN** 前端拒绝进入管理端壳层（与既有门户准入一致）

### Requirement: Admin docs file ops human search and identity UX

管理端云文档「文件管理」页 SHALL 以标题关键词、类型、状态及**成员搜索选人**筛选所有者；MUST NOT 以「所有者用户 ID」文本框作为主筛选控件。标题列 MUST NOT 默认在标题下展示 objectId。所有者列 MUST 使用 `WorkspaceOwnerChip`。转移/恢复指定新所有者 MUST 通过成员搜索选人（姓名/手机/邮箱），而非仅手填用户 ID。

#### Scenario: Owner filter via member search

- **WHEN** 管理员通过姓名、手机号或邮箱选中某成员并查询
- **THEN** 列表按该成员 `ownerUserId` 过滤
- **AND** 主 UI MUST NOT 要求管理员记忆数字用户 ID

#### Scenario: Title column without object id subtitle

- **WHEN** 文件列表渲染标题列
- **THEN** 主文案为文档标题
- **AND** MUST NOT 在标题正下方默认展示 `ID {objectId}`

#### Scenario: Deep link ownerUserId still works

- **WHEN** 从存储用量跳转携带 `?ownerUserId=`
- **THEN** 文件管理页应用该所有者过滤并展示对应用户 chip（若可解析）

### Requirement: Docs product storage navigation split

云文档管理壳层 SHALL 区分「存储用量详情」与「存储上限管理」入口（上限管理可为用量页主按钮进入的子路由）。文案 MUST 避免与基础设施「存储」配置混淆。

#### Scenario: Quota management is reachable from docs storage

- **WHEN** 管理员在云文档存储用量页
- **THEN** 可进入存储上限管理
- **AND** MUST NOT 依赖 `/admin/infra/storage` 完成成员配额编辑

