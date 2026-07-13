package com.relayflow.module.infra.api.realtime;

import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEventDTO;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RealtimeEventPublisherImplTest {

    @Mock
    private RealtimeTransportApi realtimeTransportApi;

    @InjectMocks
    private RealtimeEventPublisherImpl publisher;

    @Test
    void publishNotifyDeliversToTargetUsers() {
        RealtimeEventDTO event = RealtimeEventDTO.builder()
                .domain(RealtimeDomain.NOTIFY.getCode())
                .type("notify.new")
                .tenantId(1L)
                .targetUserIds(List.of(100L))
                .payload(Map.of("unreadCount", 3))
                .build();

        publisher.publish(event);

        ArgumentCaptor<RealtimeEnvelopeDTO> captor = ArgumentCaptor.forClass(RealtimeEnvelopeDTO.class);
        verify(realtimeTransportApi).sendToUsers(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(List.of(100L)), captor.capture());
        assertEquals("notify", captor.getValue().getDomain());
        assertEquals("notify.new", captor.getValue().getType());
    }

    @Test
    void publishNotifyWithoutTargetsIsNoOp() {
        RealtimeEventDTO event = RealtimeEventDTO.builder()
                .domain(RealtimeDomain.NOTIFY.getCode())
                .type("notify.new")
                .tenantId(1L)
                .targetUserIds(List.of())
                .payload(Map.of("unreadCount", 1))
                .build();

        publisher.publish(event);

        verifyNoInteractions(realtimeTransportApi);
    }
}
