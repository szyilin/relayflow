package com.relayflow.module.meeting.controller.app.vo;

import lombok.Data;

@Data
public class MeetingJoinCredentialsReqVO {

    private Long meetingId;

    private String meetingCode;
}
