package com.relayflow.module.im.service.typing;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.enums.PresenceRealtimeTypes;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImTypingServiceImpl implements ImTypingService {

    static final Duration TYPING_TTL = Duration.ofSeconds(5);

    private final ImConversationService conversationService;
    private final RealtimeTransportApi realtimeTransportApi;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void startTyping(Long tenantId, Long userId, Long conversationId) {
        if (!ensureMember(tenantId, conversationId, userId)) {
            return;
        }
        String key = redisKey(tenantId, conversationId, userId);
        stringRedisTemplate.opsForValue().set(key, "1", TYPING_TTL);
        fanout(tenantId, conversationId, userId, true);
    }

    @Override
    public void stopTyping(Long tenantId, Long userId, Long conversationId) {
        if (!ensureMember(tenantId, conversationId, userId)) {
            return;
        }
        stringRedisTemplate.delete(redisKey(tenantId, conversationId, userId));
        fanout(tenantId, conversationId, userId, false);
    }

    private boolean ensureMember(Long tenantId, Long conversationId, Long userId) {
        try {
            conversationService.requireMembership(tenantId, conversationId, userId);
            return true;
        } catch (ServiceException ex) {
            log.debug("Ignore typing from non-member userId={} conversationId={}", userId, conversationId);
            return false;
        }
    }

    private void fanout(Long tenantId, Long conversationId, Long userId, boolean typing) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("userId", userId);
        payload.put("typing", typing);

        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(PresenceRealtimeTypes.DOMAIN)
                .type(PresenceRealtimeTypes.TYPING_UPDATED)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();

        List<Long> recipients = conversationService.listOtherMemberUserIds(tenantId, conversationId, userId);
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
    }

    static String redisKey(Long tenantId, Long conversationId, Long userId) {
        return "im:typing:" + tenantId + ":" + conversationId + ":" + userId;
    }
}
