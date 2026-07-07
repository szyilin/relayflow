package com.relayflow.module.system.api.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserCreateReqDTO {

    private String username;
    private String password;
    private String nickname;
    private String mobile;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
}
