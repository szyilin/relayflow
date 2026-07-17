package com.relayflow.framework.web.filter;

import com.relayflow.framework.web.core.TraceIdConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Binds a TraceId to MDC and echoes it on {@code X-Trace-Id}.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TraceIdConstants.HEADER));
        MDC.put(TraceIdConstants.MDC_KEY, traceId);
        response.setHeader(TraceIdConstants.HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdConstants.MDC_KEY);
        }
    }

    static String resolveTraceId(String incoming) {
        if (!StringUtils.hasText(incoming)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String trimmed = incoming.trim();
        if (trimmed.length() > TraceIdConstants.MAX_LENGTH) {
            return trimmed.substring(0, TraceIdConstants.MAX_LENGTH);
        }
        return trimmed;
    }
}
