## Context

- as-is：`ImBotApi.send` 要求 `im_bot_tenant_enablement` **且** `im_bot_user_enablement`；入企钩子按 tenant 已开通列表拷贝到 user 表；种子只给 `tenant_id=1` 写了 tenant enablement。
- 现象：新企业（如注册「乐云科技」）无 tenant 行 → 邀请发 Bot 失败 → 整单邀请失败并 toast「机器人未在该企业启用」。
- 产品拍板：系统内置 Bot 人人可达；企业/个人订阅是 **并集**（不必强制拷贝）；触达失败不得挡业务、不得吓唬用户。
- 约束：跨域仅 `*-api`；开发期 `0.x` 可硬切 schema；与 `im-bot-notify-foundation` / `im-bot-invite-migrate` 衔接。

## Goals / Non-Goals

**Goals:**

1. 用 `im_bot.type`（或等价）区分 **system** 与需订阅的 Bot。
2. 发送可达性：`system` → 免订阅；其它 → `tenant_sub ∪ user_sub`。
3. 停止「入企必须为 system Bot 写 user enablement」。
4. 业务产方（邀请）对 `ImBotApi.send` **best-effort**：失败只记日志，主 API 仍成功。
5. 文档与规格纠偏，避免后续再写「双层都要有」。

**Non-Goals:**

- 通用 TenantProvisioner 编排框架（另议；本切片不引入大流水线）。
- 企业可安装 Bot 市场、用户 opt-in UI。
- 历史 `im_bot_user_enablement` 全量清理迁移（可保留表供后续 opt-in）。
- 深度改造 `enable_policy` 语义（可与 `type` 并存一阶段，避免双改爆炸）。

## Decisions

### D1 — `im_bot.type`（分类）优于再加 `source`

| 选项 | 结论 |
|------|------|
| A. `type=system \| tenant`（推荐） | **采纳**：发送门禁一眼可读；system = 平台内置触达 |
| B. 仅用现有 `enable_policy=mandatory` | **不采纳**：policy 描述「怎么开」，不等于「免查订阅表」 |
| C. `source` 字段 | **不采纳**：与 type 语义重叠；本切片用 type |

约束建议：`type=system` 时发送路径 **忽略** tenant/user 订阅表。  
`type=tenant`（或非 system）走并集判定。

种子：`org-assistant`、`task-bot`、`approval-bot`、`account-security` → `system`。

### D2 — 可达性判定（并集）

```text
canDeliver(tenantId, userId, bot):
  if bot.type == system:
    return true   // 仍要求目标用户对该 tenant 有合理成员语境（见 D3）
  if tenantEnablement(tenantId, botId) exists & enabled:
    return true
  if userEnablement(tenantId, userId, botId) exists:
    return true
  return false
```

**不再**要求：tenant 开通 ⇒ 自动 insert user 行。  
企业开通即可推全员（该企业上下文下）；用户单独订阅可覆盖「企业未开但个人开」的未来场景。

### D3 — 扇出与成员语境

`ALL_ACTIVE_MEMBERSHIPS`：仍只投递用户 **ACTIVE** 的企业。  
`SINGLE`：目标 `(tenantId, userId)` 须是有效投递目标（邀请场景下 invitee 可能尚未 ACTIVE 于邀请企业——当前 invite 策略是扇出到已有 ACTIVE 企业；无 ACTIVE 则跳过发送。本切片 **不改变** 该产品策略，只改「有 ACTIVE 时为何发不出」）。

system Bot 免订阅 **不代表** 可对任意凭空 tenantId 写消息：会话仍落在具体 `tenant_id`，成员须存在于该租户会话模型（ensure bot_dm 时照旧建成员）。

### D4 — 入企钩子收缩

- `ensureUserEnablementsOnActive`：对 `type=system` **no-op**（或整体改为仅处理未来 opt-in 模板，本切片可直接跳过 system）。
- 不再依赖「建企时拷贝 tenant enablement」才能发 system Bot。
- 企业订阅表：仍可用于 **非 system** Bot；system 可不写、不强制种子给每个新租户。

### D5 — 产方 best-effort（邀请等）

```text
inviteMember 事务内：
  写 membership / 部门 / 角色  ← 必须成功
  try { ImBotApi.send(...) } catch (Exception e) {
    log.warn(...);  // 不得 throw，不得影响 CommonResult
  }
```

可选加固（本切片可选任务）：`ImBotApi.send` 文档约定「调用方应 best-effort」；或提供 `sendBestEffort` 永不抛业务异常（内部吞掉并返回空结果）。**优先** 在产方 catch，避免改 API 语义过猛。

前端：邀请成功 toast；触达失败用户无感知。

### D6 — 与「统一 Provisioner」的关系

本切片 **刻意不做** 大编排。system Bot 免订阅后，建企初始化压力下降。  
非 system 的企业开通仍可用后续 Provisioner；不在此绑定。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| system Bot 无法被企业「关掉」 | 产品接受；若以后要关，另加企业黑名单表，勿与订阅并集混淆 |
| `enable_policy` 与 `type` 并存短暂冗余 | 文档写明：发送门禁看 `type`；policy 留给可配置扩展 |
| 吞掉异常掩盖真故障 | warn + 指标/日志含 botCode、tenantId、userId、exception |
| 旧环境仍有半套 user enablement | 兼容：并集判定下多一行无害 |

## Migration Plan

1. Flyway：`im_bot.type` 默认 `system` 或按 code 回填种子 Bot。
2. 改 `ImBotServiceImpl` 判定；收缩入企钩子。
3. 邀请等产方 catch。
4. 本地清库或已有库跑迁移后回归：新企业邀请 ACTIVE 用户 → 邀请成功 + 组织助手有消息。
5. 回滚：恢复双层 require（不推荐）；或仅回滚产方 catch。

## Settled（产品拍板）

1. **`type` V1 仅** `system | tenant`；`installable` 后置，本切片不做。
2. **产方 best-effort**：先在调用方 `catch` + warn 日志；**不**新增 `sendBestEffort` API（观察后再定）。

## Open Questions（本切片可不实现）

1. 非 system Bot「仅企业订阅」时，用户退企是否清理 user 订阅行？（可惰性保留或按 tenant 删——后置）
