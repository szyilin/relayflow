package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEventDTO;

public interface RealtimeEventPublisher {

    void publish(RealtimeEventDTO event);
}
