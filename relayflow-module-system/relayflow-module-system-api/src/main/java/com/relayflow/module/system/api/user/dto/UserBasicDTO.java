package com.relayflow.module.system.api.user.dto;

import lombok.Data;

@Data
public class UserBasicDTO {

    private Long id;
    private String username;
    private String nickname;
    /** Tenant membership avatar (infra file id as string), nullable */
    private String avatarFileId;
}
