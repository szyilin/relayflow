package com.relayflow.module.system.api.user.dto;

import lombok.Data;

@Data
public class UserCreateReqDTO {

    private String username;
    private String password;
    private String nickname;
    private String mobile;
    private String email;
}
