package com.relayflow.module.system.service.auth;

import com.relayflow.framework.security.core.IdentityValidationException;
import com.relayflow.framework.security.core.MemberIdentityProvider;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserExtMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseMemberIdentityProvider implements MemberIdentityProvider {

    private final SysTenantUserExtMapper mapper;

    @Override
    public long requireActiveVersion(Long tenantId, Long userId) {
        SysTenantUserDO member = mapper.selectIdentity(tenantId, userId);
        if (member == null || member.getStatus() != TenantUserStatus.ACTIVE) {
            throw IdentityValidationException.inactiveMember();
        }
        if (member.getAuthVersion() == null || member.getAuthVersion() < 0) {
            throw IdentityValidationException.unavailable();
        }
        return member.getAuthVersion();
    }
}
