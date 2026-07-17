package com.relayflow.module.task.controller.app.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskListMemberRespVO {

    private Long userId;

    private String nickname;

    private String avatarText;

    private String role;

    private OffsetDateTime joinTime;
}
