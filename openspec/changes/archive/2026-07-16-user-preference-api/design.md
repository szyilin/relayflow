## Context

`default-data-provisioning` 将用户偏好定为 **C 类**。`workspace-settings-web` 定义 UI 与 JSON 键。本 change 落地表与 app-api。

## Goals / Non-Goals

**Goals:**

- `sys_user_preference` + GET/PUT（或 PATCH）合并读写。
- 代码默认真源；无行合法；首次写入 upsert。
- Lane contract 与 web 草案对齐。

**Non-Goals:**

- `sys_tenant_preference`（V1 跳过）。
- 入企批量插偏好行。
- 管理端改他人偏好。

## Decisions

### D1. 表

`sys_user_preference`：

- `UNIQUE (tenant_id, user_id)`
- `settings JSONB NOT NULL DEFAULT '{}'`
- `schema_version INT NOT NULL DEFAULT 1`
- 标准公共字段

### D2. 合并策略

```text
effective = deepMerge(CodeDefaults, row.settings ?? {})
```

- GET 返回 `effective` + `schemaVersion`（是否回传「仅差异」可选；V1 返回完整 effective 即可）。
- PUT 提交完整 section 或整包；服务端与默认对比后可存整包或存差异——**V1 存客户端提交的 settings 整包（已校验键）**，读时仍 merge 默认以兼容新增键。

### D3. API

- `GET /app-api/system/user/preference`
- `PUT /app-api/system/user/preference` body: `{ settings, schemaVersion? }`
- 鉴权：当前 JWT 用户 + 当前租户；禁止改他人。

### D4. 默认键（与 web 对齐）

```json
{
  "general": {
    "themeMode": "light",
    "themeColor": "teal"
  },
  "im": {
    "chatBubbleLayout": "split"
  }
}
```

## Risks / Trade-offs

- [JSONB 校验过松] → 服务端白名单已知路径，未知键拒绝或忽略（选忽略并日志）。
- [schema 升级] → `schema_version` + 启动或读时迁移函数（V1 仅常量版本=1）。

## Migration Plan

1. Flyway 建表。
2. codegen DO/Mapper diff 合入。
3. Service + Controller + 单测/编译验证。

## Open Questions

- 无。
