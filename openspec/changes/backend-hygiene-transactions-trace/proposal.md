## Why

任务多步写缺少事务、全局异常硬编码错误码且校验信息不透明、请求无 TraceId/MDC，吞异常无日志——影响数据正确性与线上排障。Wave A 先补齐这些低风险基线，不引入新中间件。

## What Changes

- 为 `TaskItemServiceImpl` 多步写路径补充 `@Transactional`
- `GlobalExceptionHandler` 使用 `GlobalErrorCodeConstants`，校验失败记录并返回字段级信息
- 新增 `TraceIdFilter`：MDC + 响应头 `X-Trace-Id`
- 将 `catch (Exception ignored)` 改为带 warn/debug 日志（登出/JWT 无效仍可幂等吞掉）

## Capabilities

### New Capabilities

- `request-trace`: HTTP 请求 TraceId 生成、MDC 绑定与响应头回传

### Modified Capabilities

- （无行为级产品规格变更；属平台卫生与可观测性）

## Impact

- Maven：`relayflow-spring-boot-starter-web`、`relayflow-module-task-biz`、少量 system/calendar/im/infra/security 日志补丁
- 无 Flyway、无新中间件、无 API 契约破坏
- 回滚：去掉 Filter / 事务注解即可；无数据迁移
