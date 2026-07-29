package com.relayflow.module.system.api.user;

import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;

import java.util.List;

public interface MemberUserApi {

    List<MemberSearchRespDTO> searchMembers(Long tenantId, String keyword, int limit);

    /**
     * ACTIVE members for admin lists (optional nickname/username/mobile keyword).
     */
    List<MemberSearchRespDTO> pageActiveMembers(Long tenantId, String keyword, int offset, int limit);

    long countActiveMembers(Long tenantId, String keyword);

    /** Primary department id for the member in tenant; null if none. */
    Long getPrimaryDeptId(Long tenantId, Long userId);
}
