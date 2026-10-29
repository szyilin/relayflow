package com.relayflow.framework.websocket.security;

import com.relayflow.framework.security.core.IdentityValidityService;
import com.relayflow.framework.security.core.IdentityValidationException;
import com.relayflow.framework.security.core.SessionIdentity;
import com.relayflow.framework.websocket.config.WebSocketProperties;
import com.relayflow.framework.websocket.core.WebSocketSessionAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final IdentityValidityService identityValidityService;
    private final WebSocketProperties webSocketProperties;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(webSocketProperties.getTokenQueryParam());
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            SessionIdentity identity = identityValidityService.authenticate(token);
            attributes.put(WebSocketSessionAttributes.TENANT_ID, identity.tenantId());
            attributes.put(WebSocketSessionAttributes.USER_ID, identity.userId());
            attributes.put(WebSocketSessionAttributes.IDENTITY, identity);
            return true;
        } catch (IdentityValidationException ex) {
            response.setStatusCode(HttpStatus.valueOf(ex.getHttpStatus()));
            return false;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
