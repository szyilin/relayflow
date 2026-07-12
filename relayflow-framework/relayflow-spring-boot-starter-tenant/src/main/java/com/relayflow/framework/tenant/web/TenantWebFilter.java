package com.relayflow.framework.tenant.web;

import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class TenantWebFilter extends OncePerRequestFilter {

    private final TenantProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Long tenantId = resolveTenantId(request);
            if (tenantId != null) {
                TenantContextHolder.set(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Long resolveTenantId(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return properties.getDefaultId();
        }

        String headerValue = request.getHeader(properties.getHeaderName());
        if (headerValue != null && !headerValue.isBlank()) {
            return Long.parseLong(headerValue.trim());
        }
        // enabled=true：未显式传 header 时不预设 tenant，由 JwtAuthenticationFilter 从 JWT 解析
        return null;
    }
}
