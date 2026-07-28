package com.relayflow.module.meeting.controller.app.vo;

import lombok.Data;

@Data
public class MeetingJoinCredentialsRespVO {

    private Long meetingId;

    private String meetingCode;

    private String title;

    private String livekitUrl;

    private String token;

    private String roomName;

    private String identity;
}
