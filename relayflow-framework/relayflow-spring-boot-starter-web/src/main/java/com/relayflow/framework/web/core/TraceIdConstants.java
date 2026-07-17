package com.relayflow.framework.web.core;

/**
 * HTTP TraceId / MDC keys aligned with {@code docs/dev/code-style.md}.
 */
public final class TraceIdConstants {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    /** Cap inbound header length to avoid log / header abuse. */
    public static final int MAX_LENGTH = 64;

    private TraceIdConstants() {
    }
}
