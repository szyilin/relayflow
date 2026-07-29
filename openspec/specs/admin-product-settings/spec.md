# admin-product-settings Specification

## Purpose

管理端「产品设置」：组织级基础产品目录（云文档 / 即时消息 / 日历）、共享产品图标注册表，以及与个人偏好的边界。

## Requirements

### Requirement: Admin product settings catalog

管理端 SHALL 提供「产品设置」入口，列出当前企业可配置的基础产品。第一版产品目录 MUST 包含 **云文档**、**即时消息**、**日历**；每个条目 MUST 展示统一产品图标与名称，并导航至该产品的组织设置页（或占位页）。

#### Scenario: Open product settings

- **WHEN** 具备相应管理权限的管理员打开产品设置入口
- **THEN** 系统展示至少云文档、即时消息、日历三个产品项
- **AND** 每项使用产品图标注册表中的图标与标签

#### Scenario: Enter IM settings

- **WHEN** 管理员点击「即时消息」
- **THEN** 进入即时消息组织设置壳层，并可导航至群规模管控、群权限管控、消息权限管控
- **AND** MUST NOT 展示「群组管理」入口（本版不做）

#### Scenario: Docs and calendar placeholder

- **WHEN** 管理员点击「云文档」或「日历」
- **THEN** 进入占位页，说明组织级设置将在后续版本开放
- **AND** MUST NOT 要求已存在对应域的策略 API

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

