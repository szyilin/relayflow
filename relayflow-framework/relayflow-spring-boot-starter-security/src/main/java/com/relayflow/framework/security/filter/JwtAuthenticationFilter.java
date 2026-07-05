package com.relayflow.framework.security.filter;

import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.PermissionAuthoritiesLoader;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ObjectProvider<PermissionAuthoritiesLoader> permissionAuthoritiesLoader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (!token.isEmpty()) {
                authenticate(token);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtTokenService.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            Long tenantId = claims.get(LoginUser.CLAIM_TENANT_ID, Long.class);
            String userType = claims.get(LoginUser.CLAIM_USER_TYPE, String.class);

            if (tenantId != null) {
                TenantContextHolder.set(tenantId);
            }

            Set<String> permissionCodes = Collections.emptySet();
            PermissionAuthoritiesLoader loader = permissionAuthoritiesLoader.getIfAvailable();
            if (loader != null && tenantId != null) {
                permissionCodes = loader.loadPermissionCodes(userId, tenantId);
            }

            LoginUser loginUser = new LoginUser(
                    userId,
                    claims.getSubject(),
                    tenantId,
                    userType,
                    LoginUser.toAuthorities(permissionCodes));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
