package com.relayflow.module.im.websocket;

import com.relayflow.module.im.enums.PresenceRealtimeTypes;
import com.relayflow.module.im.service.typing.ImTypingService;
import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles presence-domain typing events. Replaces infra NoOpPresenceHandler when IM module is loaded
 * (DomainMessageRouter keeps the first registered bean per domain — see {@code NoOpPresenceHandler}).
 */
@Slf4j
@Component("imPresenceDomainHandler")
@RequiredArgsConstructor
public class ImPresenceDomainHandler implements RealtimeDomainMessageHandler {

    private final ImTypingService typingService;

    @Override
    public String domain() {
        return RealtimeDomain.PRESENCE.getCode();
    }

    @Override
    public void onMessage(RealtimeEnvelopeDTO envelope, RealtimeSessionContext session,
                          RealtimeSessionSender sessionSender) {
        String type = envelope.getType();
        if (!PresenceRealtimeTypes.TYPING_START.equals(type)
                && !PresenceRealtimeTypes.TYPING_STOP.equals(type)) {
            log.debug("Ignoring unsupported presence type={}", type);
            return;
        }
        Long conversationId = readConversationId(envelope.getPayload());
        if (conversationId == null) {
            log.debug("Ignore typing without conversationId from userId={}", session.userId());
            return;
        }
        if (PresenceRealtimeTypes.TYPING_START.equals(type)) {
            typingService.startTyping(session.tenantId(), session.userId(), conversationId);
        } else {
            typingService.stopTyping(session.tenantId(), session.userId(), conversationId);
        }
    }

    private static Long readConversationId(Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return null;
        }
        Object raw = map.get("conversationId");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
