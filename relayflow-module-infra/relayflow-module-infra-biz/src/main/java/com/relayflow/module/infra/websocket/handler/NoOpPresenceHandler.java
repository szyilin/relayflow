package com.relayflow.module.infra.websocket.handler;

import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fallback when no IM (or other) presence handler is registered.
 * IM module provides {@code ImPresenceDomainHandler} for typing.*; that bean wins via
 * {@link ConditionalOnMissingBean} / router merge preferring the non-noop handler.
 */
@Slf4j
@Component
@Order
@ConditionalOnMissingBean(name = "imPresenceDomainHandler")
public class NoOpPresenceHandler implements RealtimeDomainMessageHandler {

    @Override
    public String domain() {
        return RealtimeDomain.PRESENCE.getCode();
    }

    @Override
    public void onMessage(RealtimeEnvelopeDTO envelope, RealtimeSessionContext session, RealtimeSessionSender sessionSender) {
        log.debug("Ignore presence domain message type={} from userId={}", envelope.getType(), session.userId());
    }
}
