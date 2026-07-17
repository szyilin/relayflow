## 1. Task transactions

- [x] 1.1 Add `@Transactional(rollbackFor = Exception.class)` to multi-step write methods in `TaskItemServiceImpl` (create/update/delete/toggleDone/createSubtask)

## 2. Global exception handler

- [x] 2.1 Use `GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR` instead of hardcoded system code
- [x] 2.2 Log validation field errors and return first field message in `msg`

## 3. TraceId

- [x] 3.1 Add `TraceIdFilter` + constants; register in `WebAutoConfiguration` at highest precedence
- [x] 3.2 Ensure MDC cleared in `finally`

## 4. Swallowed exceptions

- [x] 4.1 Add warn/debug logs for all `catch (Exception ignored)` sites (Auth logout, JWT filter, TaskCollab, CalEvent, CalCalendarShare, RealtimeSessionSender)

## 5. Verify

- [x] 5.1 `./mvnw -pl relayflow-server -am compile`
