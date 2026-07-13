package com.relayflow.module.system.api.user;

import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;

import java.util.List;

public interface MemberUserApi {

    List<MemberSearchRespDTO> searchMembers(Long tenantId, String keyword, int limit);
}
