package com.relayflow.module.im.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.service.message.ImMessageService;
import com.relayflow.module.im.service.message.dto.RealtimeSendContext;
import com.relayflow.module.infra.api.realtime.RealtimeDomainMessageHandler;
import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImDomainMessageHandler implements RealtimeDomainMessageHandler {

    private final ImMessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public String domain() {
        return ImRealtimeTypes.DOMAIN;
    }

    @Override
    public void onMessage(RealtimeEnvelopeDTO envelope, RealtimeSessionContext session,
                          RealtimeSessionSender sessionSender) {
        if (!ImRealtimeTypes.MESSAGE_SEND.equals(envelope.getType())) {
            log.debug("Ignoring unsupported IM WebSocket type={}", envelope.getType());
            return;
        }
        SendMessageReqVO request = objectMapper.convertValue(envelope.getPayload(), SendMessageReqVO.class);
        RealtimeSendContext realtimeContext = new RealtimeSendContext(session, sessionSender, envelope.getRequestId());
        messageService.sendMessage(session.tenantId(), session.userId(), request, realtimeContext);
    }
}
