package com.relayflow.framework.security.handler;

import com.relayflow.common.exception.GlobalErrorCodeConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RelayflowAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String UNAUTHORIZED_JSON = String.format(
            "{\"code\":%d,\"msg\":\"%s\",\"data\":null}",
            GlobalErrorCodeConstants.UNAUTHORIZED.getCode(),
            GlobalErrorCodeConstants.UNAUTHORIZED.getMsg());

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(UNAUTHORIZED_JSON);
    }
}
