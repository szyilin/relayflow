# API 契约：admin-calendar-product-settings

> **变更**：`admin-calendar-product-settings-v1`  
> **鉴权**：JWT + 管理身份 + `calendar:product-settings:query|update`

## Admin REST

前缀：`/admin-api/calendar/product-settings`

### GET /

**Response `data`**：

```json
{
  "allowMemberCreateOwnedCalendar": true,
  "allStaffCalendars": [
    {
      "id": "1",
      "name": "公司假期",
      "color": "#F59E0B",
      "description": "",
      "ownerUserId": "1",
      "scopes": [{ "scopeType": "ALL", "scopeId": null, "scopeLabel": "全员" }]
    }
  ]
}
```

### PUT /

```json
{ "allowMemberCreateOwnedCalendar": false }
```

返回完整 settings（含 allStaffCalendars）。

### POST /all-staff

```json
{
  "name": "公司假期",
  "color": "#F59E0B",
  "description": "",
  "scopes": [{ "scopeType": "ALL" }]
}
```

`scopes`：`ALL`（全员）或 `USER`/`DEPT` + `scopeId`。含 `ALL` 时忽略其它项。

### PUT /all-staff

同 POST，另需 `id`。

### DELETE /all-staff?id=

日历下仍有未取消日程时拒绝。

## App 增量（本机色 + 全员可见）

见 [`workspace-calendar/contract.md`](../workspace-calendar/contract.md) `PUT /display-color`、`GET /org-policy` 与 list 字段 `defaultColor` / `displayColor` / `effectiveColor` / `type=ALL_STAFF`。

### GET /app-api/calendar/calendar/org-policy

成员可读；用于工作台隐藏「添加日历」。

```json
{ "allowMemberCreateOwnedCalendar": true }
```

创建 OWNED 仍以后端策略强制为准（错误码 `1_005_001_005`）。
