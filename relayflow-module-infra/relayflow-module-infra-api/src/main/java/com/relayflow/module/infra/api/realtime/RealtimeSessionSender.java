package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;

public interface RealtimeSessionSender {

    void send(RealtimeSessionContext session, RealtimeEnvelopeDTO envelope);
}
