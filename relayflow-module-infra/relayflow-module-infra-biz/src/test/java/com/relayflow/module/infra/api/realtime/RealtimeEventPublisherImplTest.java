package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEventDTO;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RealtimeEventPublisherImplTest {

    @Mock
    private RealtimeTransportApi realtimeTransportApi;

    @InjectMocks
    private RealtimeEventPublisherImpl publisher;

    @Test
    void publishNotifyIsNoOpWithoutBusinessWritePath() {
        RealtimeEventDTO event = RealtimeEventDTO.builder()
                .domain(RealtimeDomain.NOTIFY.getCode())
                .type("notify.new")
                .tenantId(1L)
                .targetUserIds(List.of(100L))
                .payload(Map.of("unreadCount", 3))
                .build();

        publisher.publish(event);

        verifyNoInteractions(realtimeTransportApi);
    }

    @Test
    void publishSystemDeliversToTargetUsers() {
        RealtimeEventDTO event = RealtimeEventDTO.builder()
                .domain(RealtimeDomain.SYSTEM.getCode())
                .type("system.ping")
                .tenantId(1L)
                .targetUserIds(List.of(100L))
                .payload(Map.of("ok", true))
                .build();

        publisher.publish(event);

        org.mockito.Mockito.verify(realtimeTransportApi).sendToUsers(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(List.of(100L)),
                org.mockito.ArgumentMatchers.any());
    }
}
