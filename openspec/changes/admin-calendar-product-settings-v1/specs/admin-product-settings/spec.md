## MODIFIED Requirements

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

#### Scenario: Enter calendar settings

- **WHEN** 管理员点击「日历」
- **THEN** 进入可配置的日历组织设置（含全员日历管理能力，见 `admin-calendar-product-settings`）
- **AND** MUST NOT 再仅展示「组织级设置将在后续版本开放」占位

#### Scenario: Docs placeholder remains

- **WHEN** 管理员点击「云文档」且云文档组织设置尚未交付
- **THEN** 仍可进入占位页（或已交付则进入可配置页，以 `admin-docs-product-settings` 为准）
