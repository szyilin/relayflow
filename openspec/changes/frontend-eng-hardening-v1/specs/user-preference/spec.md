## ADDED Requirements

### Requirement: Frontend preference store is server-backed

工作台前端的用户偏好 Pinia store MUST 在登录成功与租户/账号切换后通过偏好 GET API 水合状态。权威设置 MUST 来自 API 合并结果。localStorage MUST NOT 作为跨会话、跨设备的写权威；若使用缓存，键 MUST 包含当前 `tenantId` 与 `userId`。PUT 失败时 store MUST 向调用方或 UI 暴露错误，MUST NOT 静默忽略。

#### Scenario: Hydrate after tenant switch

- **WHEN** 成员从租户 A 切换到租户 B
- **THEN** 偏好 store 清空 A 的内存状态并 GET B 的偏好
- **AND** 主题与日历等设置反映 B 的合并结果（或代码默认）

#### Scenario: Types shared with API module

- **WHEN** 前端编译偏好相关代码
- **THEN** store 使用的 settings 类型与 `api/app/userPreference`（或共享 types）一致
- **AND** MUST NOT 存在两套字段互相漂移且无编译关联的平行接口定义
