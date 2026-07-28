package com.relayflow.module.rtc.api.session;

import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenRespDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionCreateReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionRespDTO;
import com.relayflow.module.rtc.service.session.RtcSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RtcSessionApiImpl implements RtcSessionApi {

    private final RtcSessionService rtcSessionService;

    @Override
    public RtcSessionRespDTO ensureSession(RtcSessionCreateReqDTO request) {
        return rtcSessionService.ensureSession(request);
    }

    @Override
    public RtcSessionRespDTO getSession(Long sessionId) {
        return rtcSessionService.getSession(sessionId);
    }

    @Override
    public RtcJoinTokenRespDTO issueJoinToken(RtcJoinTokenReqDTO request) {
        return rtcSessionService.issueJoinToken(request);
    }

    @Override
    public void endSession(Long sessionId) {
        rtcSessionService.endSession(sessionId);
    }
}
