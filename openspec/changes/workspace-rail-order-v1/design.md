## Context

工作台左侧 rail（消息 / 日历 / 视频会议 / 任务 / 智能体 / 云文档 / 通讯录）顺序写死在 `useWorkspaceNav.ts` 的 `order` 数组。用户偏好体系（`sys_user_preference` + GET/PUT）已上线，含 `general` / `im` / `calendar` / `task`，尚无 workspace 导航序。任务看板已有 HTML5 drag + 顶边线落点提示，可复用语义。

## Goals / Non-Goals

**Goals:**

- 单一代码真源定义产品默认序，对所有未自定义用户生效
- 用户可在 rail 上拖拽重排；落点用插入线提示
- 个人序持久化到 `settings.workspace.railOrder`；登录后水合、拖拽后 PUT
- 合并：过滤未知 id；缺漏已注册 id 按默认序补到末尾

**Non-Goals:**

- 管理端 / 租户级「改全员默认」（Phase 2）
- 隐藏 / 显示某个 rail 项
- 新 DnD 库、新 REST 端点、Flyway 迁移
- 设置面板里再做一份排序 UI（本期仅 rail 拖拽）

## Decisions

### D1 — 默认序仍硬编码，个人序盖在上面

```text
DEFAULT_RAIL_ORDER (code)
    ↑ 被覆盖
settings.workspace.railOrder[]（用户拖拽后）
```

「系统默认对所有用户生效」= 产品代码默认，不是租户配置。管理员改全员默认另开 change。

### D2 — 偏好键 `settings.workspace.railOrder: string[]`

- 值为例：`["messages","calendar","meeting","tasks","agents","docs","contacts"]`
- 后端 `UserPreferenceDefaults`：codeDefaults 写入与前端一致的默认数组；sanitize 接受 string[]，非法元素丢弃，空数组回落默认
- 前端 `DEFAULT_USER_PREFERENCE` 与 merge 同步

### D3 — 前端解析算法（单一函数）

```ts
function resolveRailOrder(preferred: string[] | undefined, availableIds: string[]): string[] {
  const known = new Set(availableIds)
  const seen = new Set<string>()
  const out: string[] = []
  for (const id of preferred ?? []) {
    if (known.has(id) && !seen.has(id)) { out.push(id); seen.add(id) }
  }
  for (const id of DEFAULT_RAIL_ORDER) {
    if (known.has(id) && !seen.has(id)) { out.push(id); seen.add(id) }
  }
  // any leftover availableIds not in default (future) append
  for (const id of availableIds) {
    if (!seen.has(id)) { out.push(id); seen.add(id) }
  }
  return out
}
```

### D4 — HTML5 drag + 插入线（不引入库）

- `WorkspaceRail`：`draggable`、dragover 算 beforeId（上半插前 / 下半插后）
- 落点：目标项顶边或底边 `border-t-2` / 伪元素线（与 TaskBoard `border-t-primary` 同语义）
- drop 后乐观更新 preference → `setRailOrder` → syncToServer；失败 toast + 可回滚

### D5 — 点击 vs 拖拽

- 短按仍走 `RouterLink` 导航
- dragstart 时设 `dataTransfer`；拖动距离明显时不触发误导航（可用 `@click.prevent` + 手动 push，或 drag 期间标记 `didDrag` 吞掉 click）

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| RouterLink 与 drag 冲突导致误跳转 | drop 后标记 didDrag，click 时忽略一次 |
| 偏好 PUT 失败顺序丢失 | 乐观 UI + toast；保留上次成功缓存 |
| 后端 sanitize 过严丢字段 | 仅校验数组元素为已知 rail id 集合；未知丢弃不整包失败 |
| 新增 rail 项用户旧序缺 id | resolve 算法按默认补末尾 |

## Migration Plan

1. 部署后端：GET 即带 `workspace.railOrder` 默认；旧文档无该键时 merge 补齐
2. 部署前端：rail 可读偏好并拖拽
3. 回滚：回退前端；后端保留键无害

## Open Questions

- 无（系统默认 = 代码默认已在产品讨论中拍板）
