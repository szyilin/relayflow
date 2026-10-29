package com.relayflow.framework.security.filter;

import com.relayflow.framework.security.core.AnonymousEndpoints;
import com.relayflow.framework.security.core.IdentityValidationException;
import com.relayflow.framework.security.core.IdentityValidityService;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.PermissionAuthoritiesLoader;
import com.relayflow.framework.security.core.SessionIdentity;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IdentityValidityService identityValidityService;
    private final ObjectProvider<PermissionAuthoritiesLoader> permissionAuthoritiesLoader;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        AntPathMatcher matcher = new AntPathMatcher();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (String pattern : AnonymousEndpoints.PATHS) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            reject(response, IdentityValidationException.unauthorized());
            return;
        }
        try {
            authenticate(authorization.substring(7).trim());
        } catch (IdentityValidationException ex) {
            reject(response, ex);
            return;
        } catch (RuntimeException ex) {
            reject(response, IdentityValidationException.unavailable());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        SessionIdentity identity = identityValidityService.authenticate(token);
        TenantContextHolder.set(identity.tenantId());
        PermissionAuthoritiesLoader loader = permissionAuthoritiesLoader.getIfAvailable();
        if (loader == null) {
            throw IdentityValidationException.unavailable();
        }
        Set<String> permissionCodes = loader.loadPermissionCodes(identity.userId(), identity.tenantId());
        LoginUser loginUser = new LoginUser(identity.userId(), String.valueOf(identity.userId()),
                identity.tenantId(), identity.userType(), LoginUser.toAuthorities(permissionCodes));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUser, null, loginUser.getAuthorities()));
    }

    private void reject(HttpServletResponse response, IdentityValidationException ex) throws IOException {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        response.setStatus(ex.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}",
                ex.getCode(), ex.getMessage()));
    }
}
