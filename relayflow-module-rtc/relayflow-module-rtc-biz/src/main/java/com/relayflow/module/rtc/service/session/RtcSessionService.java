package com.relayflow.module.rtc.service.session;

import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenRespDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionCreateReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionRespDTO;
import com.relayflow.module.rtc.dal.dataobject.RtcSessionDO;

public interface RtcSessionService {

    RtcSessionRespDTO ensureSession(RtcSessionCreateReqDTO request);

    RtcSessionRespDTO getSession(Long sessionId);

    RtcJoinTokenRespDTO issueJoinToken(RtcJoinTokenReqDTO request);

    void endSession(Long sessionId);

    RtcSessionDO findByRoomName(String roomName);

    void applyParticipantDelta(String roomName, int delta);

    void markEndedByRoomName(String roomName);
}
