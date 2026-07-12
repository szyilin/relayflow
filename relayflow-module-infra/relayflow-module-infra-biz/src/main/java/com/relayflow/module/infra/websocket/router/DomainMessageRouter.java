package com.relayflow.module.infra.websocket.router;

import com.relayflow.framework.websocket.core.RealtimeEnvelope;
import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import com.relayflow.module.infra.websocket.support.RealtimeEnvelopeConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DomainMessageRouter {

    private final Map<String, RealtimeDomainMessageHandler> handlersByDomain;
    private final RealtimeSessionSender sessionSender;

    public DomainMessageRouter(List<RealtimeDomainMessageHandler> handlers, RealtimeSessionSender sessionSender) {
        this.handlersByDomain = handlers.stream()
                .collect(Collectors.toMap(
                        handler -> handler.domain().toLowerCase(),
                        Function.identity(),
                        (left, right) -> left));
        this.sessionSender = sessionSender;
    }

    public void route(RealtimeEnvelope envelope, RealtimeSessionContext session) {
        RealtimeDomainMessageHandler handler = handlersByDomain.get(envelope.getDomain().toLowerCase());
        if (handler == null) {
            log.debug("No WebSocket handler for domain={}", envelope.getDomain());
            return;
        }
        handler.onMessage(RealtimeEnvelopeConverter.toApi(envelope), session, sessionSender);
    }
}
