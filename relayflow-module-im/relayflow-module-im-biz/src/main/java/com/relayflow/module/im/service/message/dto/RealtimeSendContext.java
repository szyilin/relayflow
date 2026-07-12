package com.relayflow.module.im.service.message.dto;

import com.relayflow.module.infra.api.realtime.RealtimeSessionSender;
import com.relayflow.module.infra.api.realtime.dto.RealtimeSessionContext;

public record RealtimeSendContext(
        RealtimeSessionContext session,
        RealtimeSessionSender sessionSender,
        String requestId) {
}
