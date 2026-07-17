package com.relayflow.framework.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RedisDomainEventPublisher implements DomainEventPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        DomainEvent normalized = normalize(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    writeToStream(normalized);
                }
            });
            return;
        }
        writeToStream(normalized);
    }

    private DomainEvent normalize(DomainEvent event) {
        if (event == null || !StringUtils.hasText(event.getEventType())) {
            throw new IllegalArgumentException("DomainEvent.eventType is required");
        }
        String eventId = StringUtils.hasText(event.getEventId())
                ? event.getEventId()
                : UUID.randomUUID().toString().replace("-", "");
        return DomainEvent.builder()
                .eventId(eventId)
                .eventType(event.getEventType())
                .occurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now())
                .tenantId(event.getTenantId())
                .producer(event.getProducer())
                .schemaVersion(event.getSchemaVersion() > 0 ? event.getSchemaVersion() : 1)
                .payload(event.getPayload())
                .build();
    }

    private void writeToStream(DomainEvent event) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("eventId", event.getEventId());
            body.put("eventType", event.getEventType());
            body.put("occurredAt", event.getOccurredAt().toString());
            body.put("tenantId", event.getTenantId() != null ? String.valueOf(event.getTenantId()) : "");
            body.put("producer", event.getProducer() != null ? event.getProducer() : "");
            body.put("schemaVersion", String.valueOf(event.getSchemaVersion()));
            body.put("payload", objectMapper.writeValueAsString(event.getPayload()));

            String streamKey = DomainEventStreamKeys.streamKey(event.getEventType());
            MapRecord<String, String, String> record = StreamRecords.string(body).withStreamKey(streamKey);
            stringRedisTemplate.opsForStream().add(record);
            log.debug("Published domain event type={} id={} stream={}",
                    event.getEventType(), event.getEventId(), streamKey);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize domain event payload: " + event.getEventType(), ex);
        } catch (RuntimeException ex) {
            log.error("Failed to publish domain event type={} id={}",
                    event.getEventType(), event.getEventId(), ex);
            throw ex;
        }
    }
}
