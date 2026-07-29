## MODIFIED Requirements

### Requirement: Org-level calendar product settings reserved

系统 SHALL 持久化并执行当前租户的日历组织级策略（V1 薄集，见 design：至少 `allowMemberCreateOwnedCalendar`）及全员日历管理能力。管理端产品设置日历入口 MUST 为可配置体验，MUST NOT 再仅展示占位文案。工作台 calendar 相关写路径（如创建 OWNED 日历）MUST 以后端策略为准。个人日历偏好继续由 `user-preference` 的日历段承载；单日历本机显示色由日历域用户偏好表承载，二者均 MUST NOT 被管理端组织设置页直接编辑为个人值。

#### Scenario: Effective policy applies

- **WHEN** 租户已保存日历组织策略（例如禁止成员新建 OWNED 日历）
- **THEN** 相关 calendar App API / 服务端强制执行该策略

#### Scenario: Personal preference unchanged by org page

- **WHEN** 成员在工作台全局设置中编辑日历个人偏好
- **THEN** 行为仍遵循既有 `user-preference` 规格
- **AND** 管理端组织设置页 MUST NOT 作为该个人偏好的写入口

#### Scenario: Admin configures calendar product

- **WHEN** 具备权限的管理员打开 `/admin/products/calendar`
- **THEN** 系统展示可配置的组织策略与/或全员日历管理（非占位）

## ADDED Requirements

### Requirement: All-staff calendar type and auto-subscribe

系统 SHALL 支持日历类型 `ALL_STAFF`。全员日历 MUST 仅能由具备日历产品设置写权限的管理员经 Admin API 创建/变更/删除，MUST NOT 由普通成员 App「新建日历」创建。可见范围 MUST 通过 scope 持久化（全员或指定主体），list 时解析；V1 MUST NOT 为范围内每一成员物化 `cal_calendar_share` 行。范围内成员 MUST 自动获得与 READ 共享等价的事件可读可见性。

#### Scenario: Member lists all-staff in scope

- **WHEN** 用户属于某全员日历的可见范围
- **AND** 请求日历 list
- **THEN** 响应包含该日历，来源标识为全员/订阅（非 OWNED）
- **AND** 区间事件查询包含该日历上其有权查看的事件

#### Scenario: Member outside scope

- **WHEN** 用户不在某全员日历可见范围
- **THEN** list 与事件查询 MUST NOT 因该全员日历暴露其内容

#### Scenario: Member cannot create all-staff via app

- **WHEN** 普通成员调用 App 创建日历 API 并试图指定 `ALL_STAFF`
- **THEN** 系统拒绝（忽略类型或返回业务错误）

### Requirement: Per-user display color override

系统 SHALL 允许对用户可见的日历（自有、READ 共享、全员自动订阅）持久化可选的**本机显示色**（每租户每用户每日历至多一行）。有效展示色 MUST 为 `display_color ?? calendar.color`（默认色）。更新本机色 MUST NOT 修改 `cal_calendar.color`。清除本机色（置空）后 MUST 回退到默认色。无日历名称/描述编辑权的用户仍 MUST 被允许设置本机色。

#### Scenario: Default color maps to subscribers

- **WHEN** 所有者或管理员将日历默认色改为 C
- **AND** 订阅者 U 未设置本机色
- **THEN** U 的 list/网格对该日历使用颜色 C

#### Scenario: Personal override is private

- **WHEN** 订阅者 U 将本机色设为 C_u
- **AND** 订阅者 V 未设置本机色
- **THEN** U 看到 C_u，V 仍看到日历默认色
- **AND** `cal_calendar.color` 保持为默认色

#### Scenario: No edit permission still can change display color

- **WHEN** 用户对日历仅有订阅/READ 可见权（不可改名称与描述）
- **AND** 用户提交本机显示色
- **THEN** 系统接受并持久化本机色
- **AND** 若用户试图修改名称或描述，系统拒绝

### Requirement: Workspace calendar row menu and settings panel

工作台 `/app/calendar` 侧栏对「我管理的」与「我订阅的」日历行 SHALL 提供 `···` 菜单，收敛至少：**分享**（仅所有者）、**修改颜色**（本机色快捷）、**日历设置**。MUST 以设置面板（或等价弹层）编辑/查看：名称、描述、颜色、共享人（按角色）。所有者 MUST 能在设置中改默认色与元数据；订阅者 MUST 见只读元数据并可改本机色。侧栏 MUST NOT 再依赖「仅 hover 露出共享+删除两枚裸按钮」作为唯一入口（删除可保留在设置内或菜单，但分享与设置 MUST 经 `···` 可达）。

#### Scenario: Owner opens calendar settings

- **WHEN** 所有者从 `···` 打开日历设置
- **THEN** 可编辑名称、描述、默认颜色，并管理共享人（PRIMARY/OWNED）
- **AND** 保存后 list 反映元数据变更

#### Scenario: Subscriber opens calendar settings

- **WHEN** 订阅者从 `···` 打开日历设置
- **THEN** 名称与描述为只读（或等价不可提交）
- **AND** 颜色控件写入本机色
- **AND** 共享人列表只读（若展示）

#### Scenario: Share via menu

- **WHEN** 所有者选择 `···` → 分享
- **THEN** 可增删 READ 共享（行为与既有 share API 一致）

### Requirement: Sidebar subscribed grouping

工作台日历侧栏 SHALL 区分「我管理的」与「我订阅的」。「我订阅的」MUST 包含经 READ 共享获得的日历与可见范围内的全员日历。客户端 MAY 用类型/来源字段区分展示，MUST NOT 要求用户手动搜索才能看到全员日历。

#### Scenario: All-staff appears under subscribed

- **WHEN** 用户在全员日历可见范围内
- **THEN** 该日历出现在「我订阅的」（或规格等价分组）
- **AND** 不出现在「我管理的」，除非该用户同时为日历所有者/管理员身份且产品将管理入口另列
