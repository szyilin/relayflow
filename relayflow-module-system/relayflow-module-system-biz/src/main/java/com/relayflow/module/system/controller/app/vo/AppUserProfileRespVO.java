package com.relayflow.module.system.controller.app.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AppUserProfileRespVO {

    private Long userId;
    private String username;
    private String nickname;
    /** Stored file id; empty when no custom avatar. */
    private String avatar;
    /** Personal signature on business card. */
    private String signature;
    /** Cover image file id; empty when unset. */
    private String coverFileId;
    private Long tenantId;
    private String tenantName;
    /** V1 always false — enterprise certification not implemented. */
    @JsonProperty("tenantVerified")
    private boolean tenantVerified;
    @JsonProperty("isAdmin")
    private boolean admin;
}
