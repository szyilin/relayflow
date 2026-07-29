## ADDED Requirements

### Requirement: Message context menu

工作台 IM 消息 MUST 支持桌面端右键上下文菜单作为消息操作主入口：在指针位置列出可用操作（至少：复制文本、引用；对本人消息在策略允许时含编辑 / 撤回；对文件 / 图片含下载）。悬停三点菜单 MUST 保留为辅入口，且 MUST 与右键共用同一操作集合与权限判断。系统消息 MUST NOT 展示操作菜单。

#### Scenario: Right-click opens actions at pointer

- **WHEN** 用户在文本 / 图片 / 文件气泡上右键
- **THEN** 系统在指针附近展示上下文菜单，且浏览器默认菜单不出现在该区域

#### Scenario: Hover ellipsis remains available

- **WHEN** 用户悬停同一气泡并点击三点
- **THEN** 展示与右键等价的可用操作列表

### Requirement: Media preview and file open

图片消息单击 MUST 打开灯箱预览（可关闭、可下载）。PDF 等 V1 可预览类型单击 MUST 先打开应用内预览；不可预览类型单击 MUST 触发下载并给出成功或失败反馈。

#### Scenario: Image opens lightbox

- **WHEN** 用户左键单击图片气泡中的图片
- **THEN** 系统以灯箱展示大图，用户可关闭或下载

#### Scenario: Non-previewable file downloads

- **WHEN** 用户左键单击不可预览的文件附件
- **THEN** 系统开始下载并提示结果

### Requirement: Composer send keyboard and focus

消息输入框 MUST 使用 Enter 发送、Shift+Enter 换行。发送成功或失败后，输入框 MUST 保持或立即恢复键盘焦点，除非用户主动离开该页。

#### Scenario: Enter sends and keeps focus

- **WHEN** 输入框有内容且用户按下 Enter（无 Shift）
- **THEN** 系统发送消息，清空已发送内容，且输入框仍可继续键入

#### Scenario: Shift+Enter inserts newline

- **WHEN** 用户按下 Shift+Enter
- **THEN** 系统插入换行且不发送

### Requirement: Quote jump

消息气泡内嵌的引用块 MUST 可点击：点击后滚动到被引消息并短暂高亮。被引消息不在当前已加载列表时，系统 MUST 给出明确提示（或拉取历史后定位，若实现已支持）。

#### Scenario: Click quote scrolls to original

- **WHEN** 用户点击一条仍在列表中的引用块
- **THEN** 列表滚动到被引消息并短暂高亮
