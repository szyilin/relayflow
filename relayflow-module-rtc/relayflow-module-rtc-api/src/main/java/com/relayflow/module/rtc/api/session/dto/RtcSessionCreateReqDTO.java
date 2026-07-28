package com.relayflow.module.rtc.api.session.dto;

import lombok.Data;

@Data
public class RtcSessionCreateReqDTO {

    /** {@link com.relayflow.module.rtc.enums.RtcPurpose} name */
    private String purpose;

    private String ownerType;

    private Long ownerId;
}
