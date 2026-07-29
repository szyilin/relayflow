## ADDED Requirements

### Requirement: Calendar org settings replace placeholder

管理端产品设置中的日历入口 SHALL 提供可配置的组织级设置体验（非占位文案）。具体策略项清单 MUST 在本 change design 深化后写入本规格与 `calendar` delta。

#### Scenario: Admin opens calendar product settings
- **WHEN** 具备相应管理权限的管理员打开 `/admin/products/calendar`
- **THEN** 系统展示日历组织设置表单或分组导航（非「后续版本开放」占位）
