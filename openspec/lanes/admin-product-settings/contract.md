# API 契约：admin-product-settings

> **状态**：web UI 已就绪；Admin/App API 实现中  
> **变更**：`admin-product-settings-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端「产品设置」：云文档 / 即时消息 / 日历入口；IM 三块组织策略（群规模、群权限、消息权限）。个人偏好仍走 `user-preference`。

## 鉴权

| 面 | 路径 | 鉴权 |
|----|------|------|
| Admin | `/admin-api/im/product-settings*` | JWT + 管理身份 + `im:product-settings:query` / `update` |
| App | `/app-api/im/settings/effective` | JWT + 有效成员 |

前端：`api/admin/imProductSettings.ts` → `stores/imProductSettings` → `/admin/products/**`。

## GET /admin-api/im/product-settings

**Response `data`**

```json
{
  "groupMemberLimitDefault": 50,
  "userRules": [
    {
      "subjectType": "USER",
      "subjectId": "1001",
      "subjectLabel": "张三",
      "memberLimit": 100
    }
  ],
  "groupRules": [
    {
      "conversationId": "2001",
      "groupName": "产品群",
      "memberLimit": 30
    }
  ],
  "allowPublicGroup": true,
  "deptGroupLeavePolicy": "ALLOW",
  "memberExportPolicy": "DENY_ALL",
  "memberExportDesignatedUserIds": [],
  "recallEnabled": true,
  "recallWithinHours": 24,
  "editEnabled": true,
  "editWithinHours": 24
}
```

无行时返回代码默认（上限 50；公开允许；部门群退出允许；导出禁止；撤回/编辑允许且 24h）。

## PUT /admin-api/im/product-settings

Request body 同 GET。全量替换主配置 + 用户/群组规则集。

校验：

- `groupMemberLimitDefault` / 各 `memberLimit` ≥ 2
- `userRules[].subjectType` ∈ `DEPT|USER`；主体属当前租户
- `groupRules[].conversationId` 为本租户 `type=group` 会话
- `memberExportPolicy=ALLOW_DESIGNATED` 时 `memberExportDesignatedUserIds` 非空
- `recallWithinHours` / `editWithinHours` ∈ `2|24|168`

## GET /admin-api/im/product-settings/groups?keyword=

管理端群组规则集选择器。仅当前租户对话群。

```json
[
  { "conversationId": "2001", "name": "产品群", "memberCount": 12 }
]
```

## GET /app-api/im/settings/effective

工作台菜单显隐摘要（强制执行仍以后端写路径为准）。

```json
{
  "groupMemberLimitDefault": 50,
  "allowPublicGroup": true,
  "deptGroupLeavePolicy": "ALLOW",
  "memberExportPolicy": "DENY_ALL",
  "recallEnabled": true,
  "recallWithinHours": 24,
  "editEnabled": true,
  "editWithinHours": 24
}
```

## 建群 / 加成员强制

- `POST /app-api/im/group/create`：初始人数（含自己）不得超过发起人生效上限
- `POST /app-api/im/group/members/add`：加人后总人数不得超过该群生效上限
- 解析：群组规则 > 用户规则（用户优先于部门；多部门取最大）> 企业默认
- 下调上限不踢人；超限错误码 `GROUP_MEMBER_LIMIT_EXCEEDED`

## 前端路由

| 路径 | 说明 |
|------|------|
| `/admin/products` | 产品目录 |
| `/admin/products/im/group-size` | 群规模 |
| `/admin/products/im/group-permission` | 群权限 |
| `/admin/products/im/message-permission` | 消息权限 |
| `/admin/products/docs` · `/admin/products/calendar` | 占位 |

## curl 草案

```bash
# 读设置
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/im/product-settings

# 搜群
curl -s -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/admin-api/im/product-settings/groups?keyword=产品'

# 生效策略
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/app-api/im/settings/effective
```
