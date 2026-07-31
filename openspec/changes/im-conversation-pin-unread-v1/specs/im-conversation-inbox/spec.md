## ADDED Requirements

### Requirement: Separate inbox badge watermark from peer-visible read receipt

系统 MUST 为每个会话成员维护两套水位：对端可见的真实已读水位（`read_seq`）与仅影响本人会话列表红点的 inbox 水位（`badge_seq` 或等价字段）。列表 `unreadCount` MUST 由 inbox 水位推导。气泡/回执查询所暴露的成员已读 MUST 继续基于 `read_seq`，MUST NOT 因列表「标记已读/未读」而单独变化。

#### Scenario: Mark read on list without opening

- **WHEN** 成员未打开会话，仅在列表将会话标记为已读
- **THEN** 其列表未读红点清除（或 unreadCount 变为 0）
- **AND** 该成员 `read_seq` 不变，其他成员查询到的其已读回执不变

#### Scenario: Mark unread after having read

- **WHEN** 成员已真实读过（`read_seq` 已至最新）后在列表标记未读
- **THEN** 其列表未读数变为 **1**（固定露出一条）
- **AND** 其 `read_seq` 不变，对端仍视为已读

#### Scenario: Mark unread when already having multiple unread

- **WHEN** 成员会话 inbox 未读数大于 1 时执行标记未读
- **THEN** 列表未读数收敛为 **1**（不保留原未读条数）
- **AND** `read_seq` 不变

### Requirement: Entering a conversation advances true read

当成员的会话消息主面板成为当前活动会话时，系统 MUST 将该成员的 `read_seq` 与 inbox 水位（`badge_seq`）推进到当时会话最大 seq，MUST NOT 要求用户滚动浏览全部未读气泡。会话保持前台活动期间若有新消息，系统 MUST 继续上抬两水位。系统 MUST NOT 将「离开会话时 flush」作为正确性所依赖的唯一或必选路径（断电/强杀无可靠 leave）。中途未滚完即离开，MUST NOT 回退已推进的真实已读水位；若有短暂漏报，下次进入该会话时 MUST 能再次推进并纠正。

#### Scenario: Open conversation clears receipt and badge

- **WHEN** 成员从列表打开一个有未读的会话且消息面板激活
- **THEN** 其 `read_seq` 与 inbox 水位均推进到进入时的 max seq
- **AND** 对端可通过既有已读查询/推送感知 `read_seq` 提升

#### Scenario: New messages while conversation stays active

- **WHEN** 成员仍停留在该会话面板且有新消息到达
- **THEN** 两水位继续推进到新的 max seq

#### Scenario: Leave without scrolling all unread still counts as read

- **WHEN** 成员打开未读很多的会话后未滚到底即切换走
- **THEN** 系统仍保持已推进的真实已读水位，不因未滚完而回退

#### Scenario: Abrupt client death does not depend on leave hook

- **WHEN** 客户端在已成功完成进入上报之后异常退出（断电、强杀等）
- **THEN** 服务端已落库的 `read_seq` / inbox 水位保持有效
- **AND** 正确性 MUST NOT 依赖未发出的 leave 上报

### Requirement: Conversation list mark read and mark unread actions

工作台会话列表 MUST 提供标记已读与标记未读操作（右键菜单或等价入口）。操作 MUST 只修改当前用户的 inbox 水位（及派生未读数），MUST NOT 单独用于伪造对端可见回执。

#### Scenario: Context menu exposes mark actions

- **WHEN** 成员在会话列表行上打开上下文菜单
- **THEN** 根据当前 inbox 态提供可用的标记已读或标记未读（或二者按态显示）

### Requirement: Jump to first unread with divider and windowed loading

打开存在 inbox 未读的会话时，工作台 MUST 自动定位到首条未读消息附近，并在分界处展示「以下为新消息」（或等价文案）。消息加载 MUST 基于 seq 窗口（围绕首条未读、向前/向后分页、最新窗口），MUST NOT 为展示未读而一次加载全部历史或全部未读。当用户请求回到最新时，客户端 MUST 加载最新窗口并可使用过渡动画，MUST NOT 为填空洞而拉取中间全部消息。

#### Scenario: Open with unread lands on first unread

- **WHEN** 成员打开 inbox 未读数 > 0 的会话
- **THEN** 视口定位到首条未读附近且可见新消息分界提示

#### Scenario: Scroll loads adjacent windows

- **WHEN** 成员在锚点窗口上方向或下方向滚动触发加载
- **THEN** 系统按 before/after（或等价）窗口追加消息且遵守服务端条数上限

#### Scenario: Jump to latest loads latest window only

- **WHEN** 成员在存在巨大 seq 空洞时点击回到最新
- **THEN** 界面到达最新消息区域，且请求仅为最新窗口（不拉全中间历史）

### Requirement: Remaining unread affordance while not at bottom

当进入会话时存在未读且视口未贴底时，工作台 MUST 提供下方未读数量或等价「还有 N 条未读 / 回到最新」提示，避免用户不知道最新侧仍有内容。

#### Scenario: Show unread below hint

- **WHEN** 成员停留在首条未读附近而下方仍有未读消息未进入视口
- **THEN** UI 展示剩余未读或回到最新的可操作提示
