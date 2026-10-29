package com.relayflow.framework.security.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;

/** Shared fail-closed authorization boundary for HTTP and realtime sessions. */
@RequiredArgsConstructor
public class IdentityValidityService {

    private final JwtTokenService jwtTokenService;
    private final ObjectProvider<TokenRevocationStore> revocationStores;
    private final ObjectProvider<MemberIdentityProvider> memberProviders;

    public SessionIdentity authenticate(String token) {
        SessionIdentity identity = parse(token);
        validate(identity);
        return identity;
    }

    public SessionIdentity parse(String token) {
        try {
            Claims claims = jwtTokenService.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            Long tenantId = claims.get(LoginUser.CLAIM_TENANT_ID, Long.class);
            String userType = claims.get(LoginUser.CLAIM_USER_TYPE, String.class);
            Long version = claims.get(LoginUser.CLAIM_MEMBER_VERSION, Long.class);
            if (userId <= 0 || tenantId == null || tenantId <= 0 || userType == null || userType.isBlank()
                    || claims.getId() == null || claims.getId().isBlank() || claims.getExpiration() == null
                    || (version != null && version < 0)) {
                throw IdentityValidationException.unauthorized();
            }
            return new SessionIdentity(tenantId, userId, userType, claims.getId(),
                    claims.getExpiration().toInstant(), version == null ? 0 : version);
        } catch (JwtException | IllegalArgumentException ex) {
            // Parser errors can contain claims/token fragments; do not log or propagate them.
            throw IdentityValidationException.unauthorized();
        }
    }

    public void validate(SessionIdentity identity) {
        if (identity == null || identity.expiresAt() == null || !identity.expiresAt().isAfter(Instant.now())) {
            throw IdentityValidationException.unauthorized();
        }
        try {
            TokenRevocationStore store = revocationStores.getIfAvailable();
            MemberIdentityProvider provider = memberProviders.getIfAvailable();
            if (store == null || provider == null) {
                throw IdentityValidationException.unavailable();
            }
            if (store.isRevoked(identity.jti(), identity.tenantId())) {
                throw IdentityValidationException.unauthorized();
            }
            if (provider.requireActiveVersion(identity.tenantId(), identity.userId()) != identity.memberVersion()) {
                throw IdentityValidationException.unauthorized();
            }
        } catch (IdentityValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw IdentityValidationException.unavailable();
        }
    }
}
