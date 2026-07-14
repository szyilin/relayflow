package com.relayflow.module.system.service.member;

import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.api.user.MemberUserApi;
import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppMemberSearchService {

    private final MemberUserApi memberUserApi;

    public List<MemberSearchRespDTO> searchMembers(String keyword, int limit) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        return memberUserApi.searchMembers(tenantId, keyword, limit);
    }
}
