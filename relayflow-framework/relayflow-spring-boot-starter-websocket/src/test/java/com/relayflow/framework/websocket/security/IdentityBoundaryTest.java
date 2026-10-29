package com.relayflow.framework.websocket.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.framework.security.config.JwtProperties;
import com.relayflow.framework.security.core.*;
import com.relayflow.framework.security.filter.JwtAuthenticationFilter;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.framework.websocket.config.WebSocketProperties;
import com.relayflow.framework.websocket.core.*;
import com.relayflow.framework.websocket.sender.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityBoundaryTest {
    private static final String SECRET = "member-auth-test-secret-at-least-32-bytes-long";
    private final Map<Long, Long> versions = new HashMap<>();
    private final Set<String> revoked = new HashSet<>();
    private final AtomicBoolean unavailable = new AtomicBoolean();
    private JwtTokenService tokens;
    private IdentityValidityService validity;
    private JwtAuthenticationFilter filter;
    private DefaultListableBeanFactory beans;

    @BeforeEach
    void setup() {
        versions.put(1L, 0L);
        versions.put(2L, 7L);
        beans = new DefaultListableBeanFactory();
        beans.registerSingleton("members", (MemberIdentityProvider) (tenant, user) -> {
            if (unavailable.get()) throw new IllegalStateException("DB down");
            if (user != 100L || !versions.containsKey(tenant)) throw IdentityValidationException.inactiveMember();
            return versions.get(tenant);
        });
        beans.registerSingleton("revocations", new TokenRevocationStore() {
            public boolean isRevoked(String jti, Long tenant) {
                if (unavailable.get()) throw new IllegalStateException("Redis down");
                return revoked.contains(tenant + ":" + jti);
            }
            public void revoke(String jti, Long tenant, Duration ttl) { revoked.add(tenant + ":" + jti); }
        });
        beans.registerSingleton("permissions", (PermissionAuthoritiesLoader) (user, tenant) -> Set.of());
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        tokens = new JwtTokenService(properties, beans.getBeanProvider(MemberIdentityProvider.class));
        validity = new IdentityValidityService(tokens, beans.getBeanProvider(TokenRevocationStore.class),
                beans.getBeanProvider(MemberIdentityProvider.class));
        filter = new JwtAuthenticationFilter(validity, beans.getBeanProvider(PermissionAuthoritiesLoader.class));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void issuerReadsTargetVersionEveryTimeAndFailsClosed() {
        assertEquals(7L, tokens.parseClaims(issue(2L)).get(LoginUser.CLAIM_MEMBER_VERSION, Long.class));
        versions.put(2L, 8L);
        assertEquals(8L, tokens.parseClaims(issue(2L)).get(LoginUser.CLAIM_MEMBER_VERSION, Long.class));
        unavailable.set(true);
        assertEquals(503, assertThrows(IdentityValidationException.class, () -> issue(2L)).getHttpStatus());
    }

    @Test
    void missingBearerOnProtectedAdminRequestIsUnauthorizedBeforePortalGate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/system/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Must not route"));
        assertEquals(401, response.getStatus());
    }

    @Test
    void legacyVersionZeroWorksUntilDisableAndReenable() throws Exception {
        String legacy = signed(null, "legacy", Instant.now().plusSeconds(300));
        assertHttp(legacy, 200, true);
        versions.remove(1L);
        assertHttp(legacy, 403, false);
        versions.put(1L, 1L);
        assertHttp(legacy, 401, false);
        assertHttp(issue(1L), 200, true);
        assertHttp(issue(2L), 200, true);
    }

    @Test
    void revokedExpiredAndMalformedTokensAreUnauthorized() throws Exception {
        String token = issue(1L);
        revoked.add("1:" + tokens.parseClaims(token).getId());
        assertHttp(token, 401, false);
        assertHttp(signed(0L, "expired", Instant.now().minusSeconds(1)), 401, false);
        assertHttp("bad-token", 401, false);
        String missingJti = Jwts.builder().subject("100").claim("tenant_id", 1L).claim("user_type", "admin")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertHttp(missingJti, 401, false);
        assertHttp(signed(-1L, "negative", Instant.now().plusSeconds(20)), 401, false);
    }

    @Test
    void authorityFailuresAndMissingComponentsAreUnavailable() throws Exception {
        String token = issue(1L);
        unavailable.set(true);
        assertHttp(token, 503, false);
        unavailable.set(false);
        beans.destroySingleton("revocations");
        assertHttp(token, 503, false);
    }

    @Test
    void memberDbAndPermissionFailuresFailClosed() throws Exception {
        String token = issue(1L);
        beans.destroySingleton("members");
        beans.registerSingleton("brokenMembers", (MemberIdentityProvider) (tenant, user) -> {
            throw new IllegalStateException("DB down");
        });
        assertHttp(token, 503, false);
        beans.destroySingleton("brokenMembers");
        beans.registerSingleton("members", (MemberIdentityProvider) (tenant, user) -> 0L);
        beans.destroySingleton("permissions");
        beans.registerSingleton("permissions", (PermissionAuthoritiesLoader) (user, tenant) -> {
            throw new IllegalStateException("permissions down");
        });
        assertHttp(token, 503, false);
    }

    @Test
    void explicitAnonymousEntriesIgnoreInvalidBearerAndGrantNoIdentity() throws Exception {
        unavailable.set(true);
        for (String path : AnonymousEndpoints.PATHS) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path.replace("**", "example"));
            request.addHeader("Authorization", "Bearer invalid");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean called = new AtomicBoolean();
            filter.doFilter(request, response, (req, res) -> {
                called.set(true);
                assertNull(SecurityContextHolder.getContext().getAuthentication());
            });
            assertTrue(called.get(), path);
        }
    }

    @Test
    void handshakeMatchesHttpFailureClassesAndRetainsMetadataOnly() throws Exception {
        String token = issue(1L);
        Map<String, Object> attributes = new HashMap<>();
        assertEquals(200, handshake(token, attributes));
        assertInstanceOf(SessionIdentity.class, attributes.get(WebSocketSessionAttributes.IDENTITY));
        assertFalse(attributes.containsValue(token));
        revoked.add("1:" + tokens.parseClaims(token).getId());
        assertEquals(401, handshake(token, new HashMap<>()));
        revoked.clear();
        versions.remove(1L);
        assertEquals(403, handshake(token, new HashMap<>()));
        unavailable.set(true);
        assertEquals(503, handshake(token, new HashMap<>()));
    }

    @Test
    void localAndRedisFanoutRejectRevokedSessionsAtActualSendAndCleanupIsIdempotent() throws Exception {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        WebSocketOnlineService online = mock(WebSocketOnlineService.class);
        WebSocketSessionGuard guard = new WebSocketSessionGuard(validity, registry, online);
        ObjectMapper mapper = new ObjectMapper();
        LocalWebSocketMessageSender sender = new LocalWebSocketMessageSender(registry, mapper, guard);
        String revokedToken = issue(1L);
        WebSocketSession a = session("a", revokedToken, registry);
        WebSocketSession b = session("b", revokedToken, registry);
        WebSocketSession good = session("good", issue(1L), registry);
        WebSocketSession otherTenant = session("other", issue(2L), registry);
        revoked.add("1:" + tokens.parseClaims(revokedToken).getId());
        RealtimeEnvelope envelope = new RealtimeEnvelope();
        sender.send(1L, List.of(100L), envelope);
        verify(a, never()).sendMessage(any());
        verify(b, never()).sendMessage(any());
        verify(good).sendMessage(any());
        verify(otherTenant, never()).close(any());
        guard.disconnect(a);
        verify(a, times(1)).close(CloseStatus.POLICY_VIOLATION);
        assertTrue(registry.hasSessions(1L, 100L));

        // Remote Redis subscriber has the same final local send boundary.
        WebSocketInstanceIdProvider instance = mock(WebSocketInstanceIdProvider.class);
        when(instance.getInstanceId()).thenReturn("local");
        RedisWebSocketFanoutSubscriber subscriber = new RedisWebSocketFanoutSubscriber(
                mock(RedisMessageListenerContainer.class), mapper, sender, instance, new WebSocketProperties());
        revoked.add("1:" + ((SessionIdentity) good.getAttributes().get(WebSocketSessionAttributes.IDENTITY)).jti());
        RedisWebSocketMessageSender.WebSocketFanoutMessage fanout = new RedisWebSocketMessageSender.WebSocketFanoutMessage();
        fanout.setTenantId(1L); fanout.setUserIds(List.of(100L));
        fanout.setSourceInstanceId("remote"); fanout.setEnvelope(envelope);
        subscriber.onMessage(new DefaultMessage("t:1:ws:fanout".getBytes(StandardCharsets.UTF_8),
                mapper.writeValueAsBytes(fanout)), null);
        verify(good, times(1)).sendMessage(any()); // Only the earlier authorized delivery.
        assertFalse(registry.hasSessions(1L, 100L));
        assertTrue(registry.hasSessions(2L, 100L));
    }

    @Test
    void expiredRemainingConnectionDoesNotKeepUserOnlineAfterLastValidConnectionCloses() {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        var redis = mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        WebSocketOnlineService online = new WebSocketOnlineService(redis, new WebSocketProperties(), registry, validity);
        WebSocketSessionGuard guard = new WebSocketSessionGuard(validity, registry, online);
        WebSocketSession expired = session("expired", issue(1L), registry);
        expired.getAttributes().put(WebSocketSessionAttributes.IDENTITY, new SessionIdentity(1L, 100L, "admin",
                "expired-jti", Instant.now().minusSeconds(1), 0));
        WebSocketSession good = session("good", issue(1L), registry);
        guard.disconnect(good);
        assertTrue(registry.hasSessions(1L, 100L)); // Expired connection has not yet been scanned.
        verify(redis).delete("t:1:online:100"); // It must not count as an online identity.
    }

    @Test
    void idleScannerClosesExpiredDisabledAndUnavailableConnections() throws Exception {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        WebSocketSessionGuard guard = new WebSocketSessionGuard(validity, registry, mock(WebSocketOnlineService.class));
        WebSocketSession expired = session("expired", issue(1L), registry);
        SessionIdentity original = (SessionIdentity) expired.getAttributes().get(WebSocketSessionAttributes.IDENTITY);
        expired.getAttributes().put(WebSocketSessionAttributes.IDENTITY, new SessionIdentity(1L, 100L, "admin",
                original.jti(), Instant.now().minusSeconds(1), 0));
        WebSocketSession disabled = session("disabled", issue(1L), registry);
        WebSocketSession good = session("good", issue(2L), registry);
        versions.remove(1L);
        guard.inspectSessions();
        verify(expired).close(CloseStatus.POLICY_VIOLATION);
        verify(disabled).close(CloseStatus.POLICY_VIOLATION);
        verify(good, never()).close(any());
        unavailable.set(true);
        guard.inspectSessions();
        verify(good).close(CloseStatus.POLICY_VIOLATION);
        assertTrue(registry.getAllSessions().isEmpty());
    }

    private WebSocketSession session(String id, String token, WebSocketSessionRegistry registry) {
        WebSocketSession session = mock(WebSocketSession.class);
        SessionIdentity identity = validity.authenticate(token);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketSessionAttributes.IDENTITY, identity);
        attributes.put(WebSocketSessionAttributes.TENANT_ID, identity.tenantId());
        attributes.put(WebSocketSessionAttributes.USER_ID, identity.userId());
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(attributes);
        registry.register(session, identity.tenantId(), identity.userId());
        return session;
    }

    private int handshake(String token, Map<String, Object> attributes) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/infra/ws");
        request.setQueryString("token=" + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new JwtWebSocketHandshakeInterceptor(validity, new WebSocketProperties()).beforeHandshake(
                new ServletServerHttpRequest(request), new ServletServerHttpResponse(response),
                mock(WebSocketHandler.class), attributes);
        return response.getStatus();
    }

    private String issue(long tenant) { return tokens.createAccessToken(100L, "test", tenant, "admin"); }

    private String signed(Long version, String jti, Instant expiry) {
        var builder = Jwts.builder().subject("100").id(jti).claim("tenant_id", 1L).claim("user_type", "admin")
                .expiration(Date.from(expiry)).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)));
        if (version != null) builder.claim("member_ver", version);
        return builder.compact();
    }

    private void assertHttp(String token, int expected, boolean shouldRoute) throws Exception {
        clear();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app-api/test/protected");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean routed = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> {
            routed.set(true);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(tokens.parseClaims(token).get("tenant_id", Long.class), TenantContextHolder.get());
        });
        assertEquals(expected, response.getStatus());
        assertEquals(shouldRoute, routed.get());
        assertFalse(response.getContentAsString().contains(token));
    }
}
