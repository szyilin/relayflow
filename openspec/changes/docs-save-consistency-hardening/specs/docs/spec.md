## ADDED Requirements

### Requirement: Metadata writes preserve document body and version

系统必须（MUST）对打开记录、重命名及其他 doc_object 元数据写入仅更新明确允许的目标字段。正文及 content_version 只能由既有正文 CAS 保存路径修改；元数据操作不得覆盖、递增或降低正文版本。现有租户、逻辑删除和 owner / VIEW / EDIT 授权必须保留。

#### Scenario: Open races with an editor save
- **GIVEN** 打开操作已读到正文 A / 版本 N，另一个有 EDIT 权限的窗口保存正文 B / 版本 N+1
- **WHEN** 打开记录在正文保存之后提交
- **THEN** 正文保持 B / N+1，访问记录正常更新，不把旧整行写回

#### Scenario: Rename races with an editor save
- **GIVEN** 重命名与正文保存并发
- **WHEN** 两者以任一次序提交
- **THEN** 新标题和新正文都保留，正文版本仅因成功 CAS 保存递增

#### Scenario: Stale body save remains a conflict
- **GIVEN** 当前正文版本为 N+1
- **WHEN** owner 或 EDIT 成员提交版本 N 的正文
- **THEN** 返回既有 `DOC_VERSION_CONFLICT`，不覆盖正文；VIEW 或已失权成员仍不能保存

### Requirement: Draft revisions and serial autosave

客户端必须（MUST）按 userId / tenantId / objectId 隔离草稿，区分本地修订号、已确认修订和服务端版本。每文档最多一份保存请求在途，期间只保留最新待保存快照；防抖必须绑定编辑发生时的文档和身份。响应只能确认其请求快照，不得覆盖更晚输入。

#### Scenario: Typing while a save is in flight
- **GIVEN** 修订 R1 正在保存，用户继续输入形成 R2
- **WHEN** R1 的慢响应返回
- **THEN** 编辑器仍显示 R2，R1 只推进确认状态；R2 随后使用返回的新版本串行提交

#### Scenario: Multiple pending edits are coalesced
- **GIVEN** R1 在途时先后编辑为 R2、R3
- **WHEN** R1 成功
- **THEN** 下一次提交最新 R3，不并行提交相同版本的 R2 / R3，不丢失最新正文

#### Scenario: Debounce is bound to the original document
- **GIVEN** 文档 A 有待保存快照，随后发起打开文档 B
- **WHEN** A 的防抖定时器触发或加载响应迟到
- **THEN** A 的正文不能提交给 B，B 的加载也不能重置 A 的未保存内容

#### Scenario: Saved indicator reflects confirmation
- **GIVEN** 存在更新草稿、待派发队列或在途请求
- **WHEN** UI 展示保存状态
- **THEN** 区分待保存 / 保存中；只有当前修订被确认且队列为空才显示已保存

#### Scenario: Metadata response does not reset dirty body
- **GIVEN** 当前草稿有未确认输入
- **WHEN** 打开、重命名或保存响应更新 store 的 activeDocument
- **THEN** 不以响应中的旧 body 或 title 无条件重建当前编辑草稿

### Requirement: Navigation preserves or explicitly discards pending edits

客户端必须（MUST）在切换文档、返回列表及 SPA 离开前 flush 并等待当前保存完成；失败时保留原草稿，允许重试或明确放弃后离开。浏览器刷新 / 关闭必须对未确认草稿触发标准离开提醒，不把请求已发出当作保存成功。

#### Scenario: Switch waits for flush
- **GIVEN** 当前文档仍在防抖窗口内
- **WHEN** 点击另一文档或返回列表
- **THEN** 先保存原文档并确认，再完成导航

#### Scenario: Failed save blocks silent navigation
- **GIVEN** 离开前保存失败
- **WHEN** 用户尚未明确选择放弃
- **THEN** 保持当前文档与草稿，显示可重试错误，不静默跳转或清空内容

#### Scenario: Explicit discard or browser unload
- **GIVEN** 存在未确认草稿
- **WHEN** 用户明确放弃，或触发浏览器关闭 / 刷新
- **THEN** SPA 仅在确认放弃后离开；浏览器卸载显示标准提示，不承诺异步卸载保存必然成功

### Requirement: Save errors and identity transitions preserve boundaries

客户端必须（MUST）在网络失败或版本冲突时保留本地草稿与原 CAS 基线，禁止通过自动取得新版本号强存旧正文。登出、账号或租户切换必须停止旧队列派发、取消客户端请求并忽略迟到响应，不能用新身份提交旧草稿。

#### Scenario: Conflict recovery keeps the local draft
- **GIVEN** 保存返回 `DOC_VERSION_CONFLICT`
- **WHEN** 用户查看恢复操作
- **THEN** 草稿仍可复制 / 导出；加载服务器版本须明确提示可能放弃本地内容，不能自动覆盖或盲目重试

#### Scenario: Permission loss stops queued writes
- **GIVEN** 文档已删除或成员失去 EDIT 权限
- **WHEN** 保存失败
- **THEN** 显示明确错误并保留可恢复内容，停止后续自动派发，不绕过 ACL

#### Scenario: Account or tenant changes during save
- **GIVEN** 旧身份有在途请求与待保存草稿
- **WHEN** 切换账号 / 租户或登出
- **THEN** 不在新身份下派发旧草稿，不向新身份展示旧正文，迟到响应不污染新状态；已提交的服务端事务不作伪回滚
