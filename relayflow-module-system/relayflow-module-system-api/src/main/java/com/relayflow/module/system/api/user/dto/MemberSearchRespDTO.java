package com.relayflow.module.system.api.user.dto;

import lombok.Data;

@Data
public class MemberSearchRespDTO {

    private Long userId;
    private String nickname;
    private String deptName;
    private Long deptId;
}
