# 默认数据初始化惯例（A / B / C）

> **何时读**：实现「入企自动建数据」「用户/企业默认配置」「实体骨架 ensure」前。  
> **原则**：**集中分类规则，分散落库**；禁止跨域上帝填充器。

与跨域边界配合阅读：[`architecture.md`](architecture.md)、[`cross-domain-messaging.md`](cross-domain-messaging.md)。

## 为什么要分类

「跟企业走 / 跟企业内用户走 / 跟某实体走」的默认数据需求会反复出现。若一律入企预插行，或做一个写入所有前缀表的 `DefaultDataService`，会带来：

- 与表前缀分域、禁止 `*-biz → *-biz` / 跨域直查他域表冲突
- 默认值演进时需要大规模回填
- 大量「与代码默认一模一样」的噪声行

因此按 **有无行时功能是否可解释** 分成三类。

## 三类策略

| 类 | 名称 | 含义 | 落库时机 | 例子 |
|----|------|------|----------|------|
| **A** | 必须物化 | 无持久行则功能坏（FK、树根、会话骨架） | 生命周期 **eager**，幂等 `ensure*` | 租户根部门 `getOrCreateRootDept` |
| **B** | 默认即无行 | 默认由代码或策略表达 | **不插行** | 系统 Bot 触达（无 enable 行也可达） |
| **C** | 偏好文档 | 读时与代码默认 **merge**；仅显式变更时 **upsert** | **lazy**（首次改写才插） | `sys_user_preference`（企业内用户设置） |

### 选型口诀

1. 没有这行，别的表/流程是否立刻挂掉？→ **A**
2. 「没改过」是否完全等于产品默认，且可用 if/策略表达？→ **B**
3. 用户（或管理员）会改、且要记住改过的值？→ **C**

新 change 的 design / tasks **应标明** 本能力属于 A/B/C。

## 集中什么、分散什么

```text
                    ┌─────────────────────────┐
                    │  文档：A/B/C + 禁止项     │  ← 集中（规则）
                    └───────────┬─────────────┘
                                │
         ┌──────────────────────┼──────────────────────┐
         ▼                      ▼                      ▼
   system ensure*          im ensure* / 策略      infra ensure*
   （只碰 sys_）            （只碰 im_）            （只碰 infra_）
         ▲                      ▲
         │ 同步 *-api / 异步领域消息 │
```

| 集中 | 分散 |
|------|------|
| A/B/C 定义与禁止项（本文） | 各域 `*EnsureService` / `*BootstrapService` |
| 生命周期事件契约（如 MemberActivated） | 各域监听器只处理 **本域 A 类** |
| | 跨域协作走 `*-api` 或领域消息 |

## 禁止

- **上帝填充器**：单一 Bean / Service 内直接 Mapper 写入多个域前缀表（`sys_` + `im_` + `infra_` …）完成「初始化」。
- **对 C 类入企灌满默认 JSON**：成员激活时批量插入与代码默认相同的偏好行。
- **用预插行表达 B 类默认**：能靠策略/常量说清就不要抄行。

## 与用户设置的关系

工作台「设置 → 通用」（主题、主题色、会话气泡布局）归属 **C 类**：

- 作用域：`(tenant_id, user_id)`（跟 **企业内用户** 走，不是全局账号一行）
- 存储：见 change `user-preference-api`（`sys_user_preference.settings` JSONB）
- UI：见 change `workspace-settings-web`
- 读：无行 → 返回代码默认；写：upsert
- **前端真源**：登录/切租户后以 GET preference 为准；localStorage 仅短时缓存（见 [`frontend-eng-hardening-v1`](../../openspec/changes/frontend-eng-hardening-v1/proposal.md)、[workspace-ui-patterns.md](workspace-ui-patterns.md)）
- 企业级默认表（`sys_tenant_preference`）V1 不做；若以后增加，merge 顺序为：`代码默认 ← 企业默认 ← 用户行`

## 检查清单（开 change / 写 ensure 时）

- [ ] 已标明 A / B / C
- [ ] A：有幂等 `ensure*`，挂在正确生命周期或显式调用点
- [ ] B：无「仅为默认」的 INSERT
- [ ] C：GET merge、PUT upsert；激活路径不灌表
- [ ] 无跨域直写他域表；需要时走 `*-api` / 领域消息

## 参考

- 根部门 ensure：`org-member-dept-default` 等归档 design
- 系统 Bot 触达策略：`im-bot-reach-policy-v1`
- 用户偏好：`openspec/changes/archive/2026-07-16-user-preference-api/`
- 设置 UI：`openspec/changes/archive/2026-07-16-workspace-settings-web/`
