package com.relayflow.module.system.api.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserInviteReqDTO {

    private String mobile;
    private String nickname;
    private String email;
    private Long deptId;
    private List<Long> roleIds;
}
