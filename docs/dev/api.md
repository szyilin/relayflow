# API 约定

RelayFlow REST API 的统一响应、错误码与分页规范。行为级需求以 `openspec/specs/` 为准。

## 路径前缀

| 类型 | 前缀 | 示例 |
|------|------|------|
| 管理端 REST | `/admin-api/{module}/` | `/admin-api/system/auth/login` |
| 用户端 REST | `/app-api/{module}/` | `/app-api/im/conversation/list` |
| WebSocket | `/infra/ws` | 长连接入口 |

`{module}` 与 Maven 域模块对应：`system`、`infra`、`im`、`bpm`（V1.1）。

## 统一响应格式

业务成功时 **HTTP 状态码仍为 200**，通过 body 内 `code` 区分业务结果。

### 成功

```json
{
  "code": 0,
  "msg": "success",
  "data": { }
}
```

无业务数据时 `data` 可为 `null` 或省略（全项目择一，实现时统一）。

### 失败

```json
{
  "code": 1001001001,
  "msg": "用户名或密码错误",
  "data": null
}
```

| HTTP 状态 | 场景 |
|-----------|------|
| 200 | 请求已处理；`code != 0` 表示业务失败 |
| 400 | 请求体/参数格式非法（框架层校验失败） |
| 401 | 未登录或 Token 无效/过期 |
| 403 | 已登录但无权限 |
| 404 | 路由不存在 |
| 500 | 未预期服务器错误 |

**禁止**向客户端返回 SQL、堆栈跟踪等内部信息；详情写入服务端日志。

### 分页

请求参数（Query）：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `pageNo` | int | 1 | 页码，从 1 开始 |
| `pageSize` | int | 20 | 每页条数，上限 100 |
| `sortField` | string | — | 排序字段（白名单） |
| `sortOrder` | string | — | `asc` 或 `desc` |

响应 `data`：

```json
{
  "list": [],
  "total": 100
}
```

### 时间字段

JSON 中时间使用 **ISO-8601 字符串**（如 `2026-07-01T08:00:00Z`）。数据库存 UTC；前端按用户时区展示。

## 错误码

错误码定义在各模块 `*-api` 的 `ErrorCodeConstants` 中，全项目引用，禁止魔法数字散落。

### 分段规则（10 位）

```text
TT MMM SSS NNN
│  │   │   └── 具体错误序号（001–999）
│  │   └────── 子模块（如 auth=001, user=002）
│  └────────── 域模块（system=001, infra=002, im=003, bpm=004）
└───────────── 类型（1=业务，2=系统/框架）
```

示例：

| 错误码 | 含义 |
|--------|------|
| `1001001001` | system / auth — 用户名或密码错误 |
| `1001002001` | system / user — 用户不存在 |
| `2003001001` | infra / file — 文件大小超限 |
| `2000001001` | 系统级未知错误 |

用户可见文案走 `msg` 字段；日志须同时记录 `errorCode` 与 `traceId`。

## 实现要点

- 全局 `@ControllerAdvice` 捕获异常并转换为统一响应
- Controller **不直接返回 DO**；出参使用 RespVO（见 [`code-style.md`](code-style.md)）
- 跨模块调用仅使用 `*-api` 中的 DTO

## 与前端路由对齐

| 后端 | 前端（Vue Router） |
|------|-------------------|
| `/admin-api/**` | 路由必须以 **`/admin`** 开头 |
| `/app-api/**` | 用户端路由 V1 暂不限前缀 |

详见 [`code-style.md`](code-style.md) 前端路由一节。
