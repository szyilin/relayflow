## ADDED Requirements

### Requirement: Workspace org invite channels web

工作台「添加企业成员」MUST 提供企业二维码、企业链接、企业邀请码、手机号四种方式的可交互 UI。重置 MUST 更换本地凭证。复制链接/码 MUST 可用。`-web` 阶段数据 MAY 仅存于客户端 store。

#### Scenario: Reset invite link

- **WHEN** 用户在企业链接 Tab 点击重置
- **THEN** 链接 token 变更，旧 token 在本地加入校验中失败或不再匹配

#### Scenario: Mobile invite draft

- **WHEN** 用户输入合法手机号并发送邀请
- **THEN** 系统记录一条本地草稿邀请并提示成功（不要求后端）

### Requirement: Join page for invite token or code web

系统 MUST 提供加入页，支持查询参数中的邀请 token 或页面内输入邀请码；在 `-web` 下对本地草稿做校验并给出成功/失败反馈。

#### Scenario: Join with valid draft code

- **WHEN** 已登录用户在加入页输入当前有效的本地邀请码并确认
- **THEN** UI 显示加入成功或等价提示（无需真实 membership 写入）
