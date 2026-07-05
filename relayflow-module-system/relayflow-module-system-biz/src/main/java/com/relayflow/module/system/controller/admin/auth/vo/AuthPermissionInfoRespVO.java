package com.relayflow.module.system.controller.admin.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class AuthPermissionInfoRespVO {

    private Long userId;
    private String username;
    private String nickname;
    private List<RoleSimpleVO> roles;
    private List<String> permissions;

    @Data
    public static class RoleSimpleVO {

        private Long id;
        private String code;
        private String name;
    }
}
