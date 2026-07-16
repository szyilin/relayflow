# API 契约：user-preference

> **状态**：已冻结（2026-07-16）  
> **起草**：`workspace-settings-web` change  
> **实现**：`user-preference-api` change  
> **惯例**：C 类（读合并 / 写 upsert），见 [`docs/dev/default-data-provisioning.md`](../../../docs/dev/default-data-provisioning.md)

## 背景

企业内用户工作台偏好：主题模式、主题色、会话气泡布局等。作用域 `(tenant_id, user_id)`；无行时返回代码默认，不强制 INSERT。

## 端点

### GET /app-api/system/user/preference

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT（当前租户有效成员） |
| 所需权限 | 无（非管理面 RBAC） |

**Response `data`**：

```json
{
  "schemaVersion": 1,
  "settings": {
    "general": {
      "themeMode": "light",
      "themeColor": "teal"
    },
    "im": {
      "chatBubbleLayout": "split"
    }
  }
}
```

| 字段 | 说明 |
|------|------|
| `schemaVersion` | 偏好文档版本；与代码默认真源对齐 |
| `settings.general.themeMode` | `light` \| `dark` \| `auto` |
| `settings.general.themeColor` | 如 `teal`、`blue`（与前端 primary 色板一致） |
| `settings.im.chatBubbleLayout` | `left`（全部左对齐）\| `split`（己方右、对方左） |

**行为**：

- 无 `sys_user_preference` 行 → 返回代码默认合并结果，**不**插入行。
- 有行 → `deepMerge(CodeDefaults, row.settings)` 后返回。

### PUT /app-api/system/user/preference

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | 无 |

**Request body**：

```json
{
  "schemaVersion": 1,
  "settings": {
    "general": {
      "themeMode": "dark",
      "themeColor": "blue"
    },
    "im": {
      "chatBubbleLayout": "left"
    }
  }
}
```

| 字段 | 约束 |
|------|------|
| `settings` | 必填；未知键可忽略；已知枚举非法则业务错误 |
| `schemaVersion` | 可选；缺省按服务端当前版本 |

**Response `data`**：同 GET（合并后的有效配置）。

**行为**：对当前 `(tenant_id, user_id)` upsert；仅本人可写。

**错误码**：

| code | 说明 |
|------|------|
| `1_001_002_011` | 用户偏好参数非法（枚举 / settings 空等） |

## curl 示例

```bash
# 读取（无行时仍 200 + 默认）
curl -sS -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/system/user/preference"

# 保存
curl -sS -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"schemaVersion":1,"settings":{"general":{"themeMode":"dark","themeColor":"teal"},"im":{"chatBubbleLayout":"split"}}}' \
  "$BASE/app-api/system/user/preference"
```

## 前端临时实现（-web）

- Store：`web/src/stores/userPreference.ts`
- 本地 `localStorage` 键：`relayflow-user-preference-local-v1`
- integrate 时改为调用本契约，删除本地持久或改为缓存。

## 表（api change）

`sys_user_preference`：`UNIQUE (tenant_id, user_id)`，`settings JSONB`，`schema_version INT`。
