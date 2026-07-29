## ADDED Requirements

### Requirement: Overview statistics replace empty placeholder

管理端概览页 MUST 用企业身份与统计卡替换「统计数据将在接入 API 后展示」空状态，并在加载失败时给出可读错误提示。

#### Scenario: Placeholder removed when data loaded

- **WHEN** overview API 成功返回
- **THEN** 不再展示「统计数据将在接入 API 后展示」文案

### Requirement: Enterprise info page logo control

企业信息「基础信息」区 MUST 支持点击/按钮更换企业头像（需 `system:enterprise:update`），上传走管理端文件会话；无更新权限时仅展示。

#### Scenario: Updater can change logo

- **WHEN** 具备 update 权限的管理员在企业信息页选择图片并上传成功
- **THEN** 基础信息区头像立即更新为新图
