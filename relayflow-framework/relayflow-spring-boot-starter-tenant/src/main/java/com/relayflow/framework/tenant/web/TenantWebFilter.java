package com.relayflow.framework.tenant.web;

import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantWebFilter extends OncePerRequestFilter {

    private final TenantProperties properties;

    public TenantWebFilter(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TenantContextHolder.set(resolveTenantId(request));
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private long resolveTenantId(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return properties.getDefaultId();
        }

        String headerValue = request.getHeader(properties.getHeaderName());
        if (headerValue == null || headerValue.isBlank()) {
            return properties.getDefaultId();
        }
        return Long.parseLong(headerValue.trim());
    }
}
