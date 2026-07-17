## Why

跨模块 ApiImpl 依赖 Controller VO、日历 list 在 Controller 编排、CalEventServiceImpl 过大——违反分层与可维护性约定。本 change 做第一刀边界修整，不拆 User。

## What Changes

- Task/IM ApiImpl 改为基于 DO / 内部模型映射到 api DTO
- 日历可见列表合并下沉到 Service；Controller 变薄
- 抽出 `CalEventRecurrenceSupport`；新增最小 MapStruct Convert
- **不做**：UserServiceImpl 全拆、全面 MapStruct、独立 MQ

## Capabilities

### New Capabilities

- （无新产品能力；平台分层卫生）

### Modified Capabilities

- （无产品行为变更）

## Impact

- task-biz、im-biz、calendar-biz
- HTTP/跨域 API 契约不变
