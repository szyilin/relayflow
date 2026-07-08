# 设计：RBAC 内核 — 后端（system-rbac-kernel-api）

## Context

- JWT 登录已有；admin 种子账号绑 `super_admin`（全部 `system:*` 权限）
- DO/Mapper：`SysUserRole`、`SysRole`、`SysRolePermission`、`SysPermission` 已 codegen
- 前端 **不在本 change 修改**；按 contract 对接由 `system-rbac-kernel-web` 负责

## Goals / Non-Goals

**Goals:**

- 每个已认证请求：`LoginUser` 带 permission codes 作为 authorities
- `get-permission-info` 返回 contract 字段
- `user/page`、`user/create` 权限注解 + Security 白名单调整
- `DataScopeHelper` 接口 + 实现（可单测友好，user/page 暂不调）

**Non-Goals:**

- Redis 缓存 permissions
- 改 JWT payload 塞 permissions
- 动态 `sys_menu` API

## Decisions

### D1：权限加载时机

**决策**：`JwtAuthenticationFilter` 解析 JWT 后调用 `PermissionService.getPermissionCodes(userId, tenantId)` 写入 `LoginUser`。  
**理由**：权限变更无需 re-login；JWT 保持小。  
**替代**：JWT 内嵌 permissions → 拒绝。

### D2：Authority 字符串

**决策**：`GrantedAuthority` 的 authority 字符串 **等于** `sys_permission.code`（如 `system:user:list`），与 `@PreAuthorize("hasAuthority('system:user:list')")` 一致。

### D3：get-permission-info 鉴权

**决策**：仅需 authenticated，不额外 `@PreAuthorize`（用户总可读自己的权限集）。

### D4：PermissionService 位置

**决策**：`relayflow-module-system-biz` 内 `service/permission/PermissionService` + `PermissionServiceImpl`；查询只用本模块 Mapper。  
**Framework**：`LoginUser` 与 Security 配置留在 `relayflow-spring-boot-starter-security`。

### D5：403 响应

**决策**：沿用 Spring Security 403；若项目已有 `AccessDeniedHandler` 返回 JSON，则保持 `code != 0` 风格；否则新增最小 JSON handler（HTTP 403 + 业务码，见 `docs/dev/api.md`）。

### D6：DataScopeHelper

**决策**：`com.relayflow.module.system.service.permission.DataScopeHelper`（或 `framework.security` 若需跨模块 — V1 仅 system 用，放 system-biz）。

```text
输入：userId, tenantId
输出：DataScopeResult { all: boolean, deptIds: Set<Long>, selfOnly: boolean }

算法：
1. 查用户全部 role 的 data_scope + sys_role_dept
2. 任一 ALL → all=true
3. 否则合并 DEPT / DEPT_AND_CHILD / CUSTOM / SELF 对应 deptIds 与 self 标记
```

本切片实现类与方法签名即可；**不在 UserServiceImpl 调用**。

## 实现要点

### PermissionService

```text
Set<String> getPermissionCodes(Long userId, Long tenantId)
List<RoleSimple> getRoleList(Long userId, Long tenantId)
PermissionInfo getPermissionInfo(Long userId, Long tenantId)  // 组装 VO
```

SQL 路径：user_role → role → role_permission → permission（tenant 条件 + deleted=0）。

### AuthController

```java
@GetMapping("/get-permission-info")
public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo()
```

从 `SecurityContext` 取 `LoginUser` userId/tenantId。

### SecurityAutoConfiguration

- 移除 `/admin-api/system/user/create` 的 permitAll
- `@EnableMethodSecurity`

### UserController

```java
@PreAuthorize("hasAuthority('system:user:list')")
@GetMapping("/page")

@PreAuthorize("hasAuthority('system:user:create')")
@PostMapping("/create")
```

## 验证

```bash
./mvnw -pl relayflow-server -am compile

# 登录 + 权限信息
# 无 token → 401
# admin → get-permission-info 含 system:user:list
# user/page 无 token → 401；有 token → 200

openspec validate system-rbac-kernel-api --strict
```

测试账号：`admin` / `admin123`（`V0.1.0.3__seed_admin_user.sql`）。

## Risks

| 风险 | 缓解 |
|------|------|
| 每请求查库 | V1 可接受；后续 Redis |
| super_admin 缺权限 | 种子已绑全部 permission |
