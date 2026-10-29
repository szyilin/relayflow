package com.relayflow.framework.security.core;

import com.relayflow.framework.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey secretKey;
    private final ObjectProvider<MemberIdentityProvider> memberProviders;

    /** 构造器内派生 secretKey，不适用 {@code @RequiredArgsConstructor}。 */
    public JwtTokenService(JwtProperties properties, ObjectProvider<MemberIdentityProvider> memberProviders) {
        this.properties = properties;
        this.memberProviders = memberProviders;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, Long tenantId, String userType) {
        long memberVersion;
        try {
            MemberIdentityProvider provider = memberProviders.getIfAvailable();
            if (provider == null) {
                throw IdentityValidationException.unavailable();
            }
            memberVersion = provider.requireActiveVersion(tenantId, userId);
        } catch (IdentityValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw IdentityValidationException.unavailable();
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTokenExpireSeconds());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(LoginUser.CLAIM_TENANT_ID, tenantId)
                .claim(LoginUser.CLAIM_USER_TYPE, userType)
                .claim(LoginUser.CLAIM_MEMBER_VERSION, memberVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Duration remainingLifetime(Claims claims) {
        if (claims == null || claims.getExpiration() == null) {
            return Duration.ZERO;
        }
        long millis = claims.getExpiration().getTime() - System.currentTimeMillis();
        return millis > 0 ? Duration.ofMillis(millis) : Duration.ZERO;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
