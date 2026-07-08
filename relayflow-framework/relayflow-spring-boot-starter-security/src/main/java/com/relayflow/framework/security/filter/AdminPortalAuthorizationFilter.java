package com.relayflow.framework.security.filter;

import com.relayflow.framework.security.handler.RelayflowAccessDeniedHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * L0 admin portal gate: authenticated users without any admin permission MUST NOT
 * access {@code /admin-api/**} except explicit whitelist paths.
 */
public class AdminPortalAuthorizationFilter extends OncePerRequestFilter {

    private static final String ADMIN_API_PREFIX = "/admin-api/";

    private static final Set<String> WHITELIST = Set.of(
            "/admin-api/system/auth/login",
            "/admin-api/system/tenant/default",
            "/admin-api/system/auth/get-permission-info"
    );

    private final RelayflowAccessDeniedHandler accessDeniedHandler = new RelayflowAccessDeniedHandler();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith(ADMIN_API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isWhitelisted(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().isEmpty()) {
            accessDeniedHandler.handle(request, response, new AccessDeniedException("Admin portal required"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(HttpServletRequest request, String path) {
        if (!WHITELIST.contains(path)) {
            return false;
        }
        if ("/admin-api/system/auth/login".equals(path)) {
            return HttpMethod.POST.matches(request.getMethod());
        }
        return HttpMethod.GET.matches(request.getMethod());
    }
}
