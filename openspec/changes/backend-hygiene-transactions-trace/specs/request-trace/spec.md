## ADDED Requirements

### Requirement: HTTP request TraceId

The system SHALL assign a TraceId to every HTTP request processed by `relayflow-server`, bind it to the logging MDC for the request duration, and expose it on the HTTP response.

#### Scenario: Missing incoming TraceId header

- **WHEN** a request arrives without an `X-Trace-Id` header
- **THEN** the server generates a non-empty TraceId, puts it in MDC under key `traceId`, and sets response header `X-Trace-Id` to that value
- **AND** MDC TraceId is cleared after the request completes

#### Scenario: Incoming TraceId header present

- **WHEN** a request arrives with a non-blank `X-Trace-Id` header
- **THEN** the server uses that value (after trimming / length safety) as the TraceId for MDC and the response header
