package com.relayflow.framework.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DomainEventListenerContainer {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final List<DomainEventHandler<?>> handlers;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final String consumerName = "c-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    public DomainEventListenerContainer(RedisConnectionFactory connectionFactory,
                                        StringRedisTemplate stringRedisTemplate,
                                        ObjectMapper objectMapper,
                                        List<DomainEventHandler<?>> handlers) {
        this.connectionFactory = connectionFactory;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.handlers = handlers != null ? handlers : List.of();
    }

    public void start() {
        if (handlers.isEmpty() || !started.compareAndSet(false, true)) {
            return;
        }
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();
        container = StreamMessageListenerContainer.create(connectionFactory, options);

        for (DomainEventHandler<?> handler : handlers) {
            String eventType = handler.eventType();
            if (!StringUtils.hasText(eventType)) {
                continue;
            }
            String streamKey = DomainEventStreamKeys.streamKey(eventType);
            ensureGroup(streamKey);
            Subscription subscription = container.receiveAutoAck(
                    Consumer.from(DomainEventStreamKeys.CONSUMER_GROUP, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                    message -> dispatch(handler, message));
            subscriptions.add(subscription);
            log.info("Domain event listener registered: type={} stream={} consumer={}",
                    eventType, streamKey, consumerName);
        }
        container.start();
    }

    @PreDestroy
    public void stop() {
        for (Subscription subscription : subscriptions) {
            try {
                subscription.cancel();
            } catch (Exception ex) {
                log.debug("Cancel domain-event subscription failed", ex);
            }
        }
        subscriptions.clear();
        if (container != null) {
            container.stop();
        }
        started.set(false);
    }

    private void ensureGroup(String streamKey) {
        try {
            stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                connection.streamCommands().xGroupCreate(
                        streamKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        DomainEventStreamKeys.CONSUMER_GROUP,
                        ReadOffset.from("0-0"),
                        true);
                return null;
            });
        } catch (Exception ex) {
            // Group may already exist (BUSYGROUP) — ignore.
            log.debug("Create consumer group skipped for {}: {}", streamKey, ex.getMessage());
        }
    }

    private void dispatch(DomainEventHandler<?> handler, MapRecord<String, String, String> message) {
        Map<String, String> body = message.getValue();
        String eventId = body.get("eventId");
        if (!StringUtils.hasText(eventId)) {
            log.warn("Domain event missing eventId on stream={}", message.getStream());
            return;
        }
        if (!claimIdempotency(eventId)) {
            log.debug("Skip duplicate domain event id={}", eventId);
            return;
        }
        try {
            DomainEvent envelope = DomainEvent.builder()
                    .eventId(eventId)
                    .eventType(body.get("eventType"))
                    .occurredAt(parseInstant(body.get("occurredAt")))
                    .tenantId(parseLong(body.get("tenantId")))
                    .producer(body.get("producer"))
                    .schemaVersion(parseInt(body.get("schemaVersion"), 1))
                    .build();
            Object payload = objectMapper.readValue(body.getOrDefault("payload", "null"), handler.payloadType());
            invoke(handler, envelope, payload);
        } catch (Exception ex) {
            log.error("Domain event handler failed: type={} id={}", handler.eventType(), eventId, ex);
            // Release idempotency so a later retry can reprocess after ops fix.
            stringRedisTemplate.delete(DomainEventStreamKeys.idempotencyKey(eventId));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void invoke(DomainEventHandler<T> handler, DomainEvent envelope, Object payload) {
        handler.handle(envelope, (T) payload);
    }

    private boolean claimIdempotency(String eventId) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(DomainEventStreamKeys.idempotencyKey(eventId), "1", IDEMPOTENCY_TTL);
        return Boolean.TRUE.equals(ok);
    }

    private static Instant parseInstant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ex) {
            return Instant.now();
        }
    }

    private static Long parseLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int parseInt(String raw, int defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
