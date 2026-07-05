package com.relayflow.framework.security.core;

import com.relayflow.framework.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    /** 构造器内派生 secretKey，不适用 {@code @RequiredArgsConstructor}。 */
    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, Long tenantId, String userType) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTokenExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(LoginUser.CLAIM_TENANT_ID, tenantId)
                .claim(LoginUser.CLAIM_USER_TYPE, userType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
