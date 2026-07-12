package com.relayflow.module.infra.websocket.handler;

import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoOpNotifyHandler implements RealtimeDomainMessageHandler {

    @Override
    public String domain() {
        return RealtimeDomain.NOTIFY.getCode();
    }

    @Override
    public void onMessage(RealtimeEnvelopeDTO envelope, RealtimeSessionContext session, RealtimeSessionSender sessionSender) {
        log.debug("Ignore notify domain message type={} from userId={}", envelope.getType(), session.userId());
    }
}
