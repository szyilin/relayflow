package com.relayflow.module.system.controller.admin.auth.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AuthPermissionInfoRespVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private List<RoleSimpleVO> roles;
    private List<String> permissions;
    /** Whether the user has admin portal access (at least one permission code). */
    @JsonProperty("isAdmin")
    private boolean admin;

    @Data
    public static class RoleSimpleVO {

        private Long id;
        private String code;
        private String name;
    }
}
