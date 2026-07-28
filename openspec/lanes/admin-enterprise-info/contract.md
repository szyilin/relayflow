# API 契约：admin-enterprise-info

> **状态**：`-integrate` 完成（前端接真实 API + geo）  
> **变更**：`admin-enterprise-info-v1`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端「企业信息」双 Tab：企业档案（只读企业编号、无域名/注销）与企业地址（多类型、中国四级区划、启停）。Geo 懒加载复用 infra 已有 API。

## 鉴权

| 面 | 路径前缀 | 鉴权 |
|----|----------|------|
| 企业档案/地址 | `/admin-api/system/enterprise/*` | JWT + 管理身份 + `system:enterprise:query`（读）/ `system:enterprise:update`（写） |
| 行政区划 | `/admin-api/infra/geo/children` | JWT + `infra:geo:query`（integrate 时地址表单必接） |

前端：`api/admin/enterprise.ts` · `api/admin/geo.ts` → `stores/enterprise` → `/admin/system/enterprise`。

## 企业档案

### GET /admin-api/system/enterprise/profile

**Response `data`**

```json
{
  "code": "RFDEMO7K9X2",
  "name": "RelayFlow 演示企业",
  "industry": "INTERNET_IT",
  "scale": "SIZE_10_49",
  "dataResidency": "中国大陆",
  "logoFileId": null,
  "contactName": "张经理",
  "contactEmail": "contact@example.com",
  "contactMobile": "13800138000",
  "contactLandline": "010-88886666"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 企业编号；创建时生成（11 位无歧义大写字母数字），**只读** |
| `name` | string | 企业名称 |
| `industry` | string \| null | 行业 code（见前端枚举） |
| `scale` | string \| null | 规模 code |
| `dataResidency` | string | V1 固定「中国大陆」，只读 |
| `logoFileId` | string \| null | 企业头像 infra 公开文件 id |
| `contactName` / `contactEmail` / `contactMobile` / `contactLandline` | string \| null | 联系人 |

### PUT /admin-api/system/enterprise/profile

**Request body**：可编辑字段（**不含** `code`、`dataResidency`）。

```json
{
  "name": "RelayFlow 演示企业",
  "industry": "INTERNET_IT",
  "scale": "SIZE_10_49",
  "logoFileId": "1234567890123456789",
  "contactName": "张经理",
  "contactEmail": "contact@example.com",
  "contactMobile": "13800138000",
  "contactLandline": "010-88886666"
}
```

**Response `data`**：同 GET。

- 请求体携带 `code` 变更时服务端 MUST 忽略或拒绝。
- `logoFileId` 为空或省略语义：空/空白清除头像；前端保存档案时 MUST 回传当前 `logoFileId` 以免误清。
- V1 **不实现**企业域名、注销企业。

### GET /admin-api/system/enterprise/overview

鉴权同 profile GET。用于管理端概览卡。

**Response `data`**

```json
{
  "code": "RFDEMO7K9X2",
  "name": "RelayFlow 演示企业",
  "logoFileId": null,
  "memberCount": 2,
  "deptCount": 1,
  "superAdminCount": 1,
  "subAdminCount": 0
}
```

| 字段 | 说明 |
|------|------|
| `memberCount` | ACTIVE 成员数 |
| `deptCount` | 未删除部门数 |
| `superAdminCount` | 绑定 `super_admin` 的去重用户数 |
| `subAdminCount` | 绑定 CUSTOM 角色的去重用户数 |

## 企业地址

地址类型枚举（多选）：`JOB`（职位）、`INTERVIEW`（面试）、`STORE`（门店）。

### GET /admin-api/system/enterprise/address/list

| Query | 说明 |
|-------|------|
| `keyword` | 可选；匹配 `regionPath` / `detail` |
| `types` | 可选；逗号分隔或重复参数，如 `JOB,INTERVIEW` |
| `status` | 可选；`ENABLED` / `DISABLED`；省略为全部 |

**Response `data`**：数组，按 `createTime` 降序。

```json
[
  {
    "id": "1234567890123456789",
    "types": ["JOB", "INTERVIEW"],
    "regionId": "440305",
    "regionPath": "中国 / 广东省 / 深圳市 / 南山区",
    "detail": "科技园南路 88 号",
    "status": "ENABLED",
    "createTime": "2026-07-20T08:00:00Z"
  }
]
```

### POST /admin-api/system/enterprise/address/create

**Request**

```json
{
  "types": ["STORE"],
  "regionId": "330106",
  "regionPath": "中国 / 浙江省 / 杭州市 / 西湖区",
  "detail": "文三路 199 号"
}
```

- `regionId` MUST 经 `GeoRegionApi` 校验为有效叶子或约定层级节点。
- 新建默认 `status=ENABLED`。

### PUT /admin-api/system/enterprise/address/update?id={id}

**Request body**：同 create 字段。

### PUT /admin-api/system/enterprise/address/status?id={id}

**Request**

```json
{ "status": "DISABLED" }
```

### DELETE /admin-api/system/enterprise/address/delete?id={id}

V1 可选；当前 UI 以启停为主，删除 API 预留。

## 行政区划（已有 · integrate 必接）

### GET /admin-api/infra/geo/children?parentId=

| Query | 说明 |
|-------|------|
| `parentId` | 省略或 `0` 为根（中国国家节点） |

**Response `data`**

```json
[
  {
    "id": "440000",
    "code": "440000",
    "name": "广东省",
    "level": "province",
    "parentId": 1,
    "hasChildren": true
  }
]
```

Integrate 后前端 MUST 直接调用真实 geo；不再回退本地演示数据。

## curl（验收）

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/admin-api/system/enterprise/profile" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X PUT "http://localhost:8080/admin-api/system/enterprise/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"演示企业","industry":"INTERNET_IT","scale":"SIZE_10_49","contactName":"张经理","contactEmail":"a@b.com","contactMobile":"13800138000","contactLandline":null}'

curl -s "http://localhost:8080/admin-api/system/enterprise/address/list?status=ENABLED" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/admin-api/infra/geo/children" \
  -H "Authorization: Bearer $TOKEN"
```

## 前端路由

| 路由 | 权限 |
|------|------|
| `/admin/system/enterprise` | `system:enterprise:query`；写操作需 `system:enterprise:update` |

侧栏入口：`useAdminNav` → 系统管理 → 企业信息。
