## Context

Wave A of backend hygiene：不碰跨域拓扑，只修事务、异常、TraceId、吞异常日志。

## Goals / Non-Goals

**Goals**

- 任务多步写原子性
- 全局异常可排障、错误码统一
- 每个 HTTP 请求有 `traceId`（MDC + `X-Trace-Id`）
- best-effort catch 至少打日志

**Non-Goals**

- 领域事件 / Redis Streams / 拆 `@Lazy`（Wave B）
- God Service 拆分 / MapStruct 补齐（Wave C）
- Outbox、独立 MQ、登录锁定

## Decisions

1. **TraceIdFilter** 放在 `relayflow-spring-boot-starter-web`，`Ordered.HIGHEST_PRECEDENCE`，早于 TenantFilter。
2. 无请求头时生成 UUID；有则透传（截断至合理长度）。
3. 校验响应：HTTP 400，`msg` 取首条字段错误；完整明细 `log.warn`。
4. 系统异常用 `GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR`。
5. JWT / logout 无效 token 仍吞异常，但打 debug/warn，不打印 token 明文。

## Risks / Trade-offs

- 事务内调用 `taskDueNotifyService` 可能拉长事务；Wave A 接受，通知解耦留后续。
- TraceId 仅 Filter 生命周期；异步线程需以后传 MDC（本波不做）。
