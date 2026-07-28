package com.relayflow.module.rtc.api.session;

import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenRespDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionCreateReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionRespDTO;

/**
 * Cross-domain RTC session API. Callers (meeting / future im) authorize users first.
 * Browser clients MUST NOT call this directly — use product app-api that aggregates credentials.
 */
public interface RtcSessionApi {

    /**
     * Create a new ACTIVE session, or return the existing ACTIVE one for the same owner.
     */
    RtcSessionRespDTO ensureSession(RtcSessionCreateReqDTO request);

    RtcSessionRespDTO getSession(Long sessionId);

    RtcJoinTokenRespDTO issueJoinToken(RtcJoinTokenReqDTO request);

    void endSession(Long sessionId);
}
