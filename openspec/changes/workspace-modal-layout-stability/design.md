## Context

工作台弹窗普遍用 Nuxt UI `UModal`，内容高度随状态变化时外壳跟着变，视口居中导致上下跳动。已有局部好例子（设置面板 `min-h`、群成员 `max-h-[60vh]`），但创建群组、邀请成员、添加机器人等未统一。

## Goals / Non-Goals

**Goals:**

- 提供可复用的工作台弹窗 body 稳定布局（限高 + 内滚 + 关键区 min-height）
- 改造创建群组、邀请成员、添加机器人及同类宽弹窗，消除明显闪动
- 内容仍完整可展示（区内滚动，不裁切不可达）

**Non-Goals:**

- 不重做管理端弹窗体系
- 不改弹窗业务逻辑 / API
- 不引入新 UI 库

## Decisions

1. **共享 `WorkspaceModalBody`（或等价 wrapper）**  
   - 默认：`max-h-[min(70vh,36rem)]`（宽弹窗可略调）+ `overflow-y-auto` + 可选 `min-h-*`  
   - 理由：比逐页复制 class 更易统一；对照已有 Settings / members 模式  
   - 备选：仅文档约定 → 易漂移，否决为唯一手段

2. **MemberPicker 宿主负责外壳稳定**  
   - Picker 内部已有 `min-h-[280px]`；宿主 modal body 再包一层限高，避免群类型切换时整窗跳动

3. **添加机器人三态同高**  
   - loading / list / empty 共用同一 `min-h` 容器，避免骨架与空态高度差

## Risks / Trade-offs

- [小屏内容多] → `max-h` + 内滚；footer 保持在 UModal footer 或 body 底固定区外  
- [过度限高裁切] → 使用 `min(70vh, …)` 并在真机核对创建群组宽弹窗

## Migration Plan

纯前端发布；无 DB。回滚还原组件即可。

## Open Questions

无（V1 按上述默认限高执行）。
