package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;

public interface RealtimeDomainMessageHandler {

    String domain();

    void onMessage(RealtimeEnvelopeDTO envelope, RealtimeSessionContext session, RealtimeSessionSender sessionSender);
}
