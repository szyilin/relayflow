package com.relayflow.module.meeting.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MeetingIdReqVO {

    @NotNull
    private Long meetingId;
}
