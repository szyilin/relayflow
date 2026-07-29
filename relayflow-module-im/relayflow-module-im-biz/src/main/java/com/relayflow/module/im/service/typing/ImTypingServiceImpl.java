package com.relayflow.module.im.service.typing;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.PresenceRealtimeTypes;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
    /** Bot generation can take longer than human typing; renew via Runtime if needed. */
    static final Duration BOT_TYPING_TTL = Duration.ofSeconds(60);

    private final ImConversationService conversationService;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final RealtimeTransportApi realtimeTransportApi;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void startTyping(Long tenantId, Long userId, Long conversationId) {
        if (!ensureMember(tenantId, conversationId, userId)) {
            return;
        }
        String key = redisKey(tenantId, conversationId, userId);
        stringRedisTemplate.opsForValue().set(key, "1", TYPING_TTL);
        fanoutUser(tenantId, conversationId, userId, true);
    }

    @Override
    public void stopTyping(Long tenantId, Long userId, Long conversationId) {
        if (!ensureMember(tenantId, conversationId, userId)) {
            return;
        }
        stringRedisTemplate.delete(redisKey(tenantId, conversationId, userId));
        fanoutUser(tenantId, conversationId, userId, false);
    }

    @Override
    public void startBotTyping(Long tenantId, Long botId, Long conversationId) {
        if (!ensureBotMember(tenantId, conversationId, botId)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(botRedisKey(tenantId, conversationId, botId), "1", BOT_TYPING_TTL);
        fanoutBot(tenantId, conversationId, botId, true);
    }

    @Override
    public void stopBotTyping(Long tenantId, Long botId, Long conversationId) {
        if (!ensureBotMember(tenantId, conversationId, botId)) {
            return;
        }
        stringRedisTemplate.delete(botRedisKey(tenantId, conversationId, botId));
        fanoutBot(tenantId, conversationId, botId, false);
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

    private boolean ensureBotMember(Long tenantId, Long conversationId, Long botId) {
        if (tenantId == null || conversationId == null || botId == null) {
            return false;
        }
        ImConversationMemberDO member = conversationMemberMapper.selectOne(Wrappers.<ImConversationMemberDO>lambdaQuery()
                .eq(ImConversationMemberDO::getTenantId, tenantId)
                .eq(ImConversationMemberDO::getConversationId, conversationId)
                .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.BOT)
                .eq(ImConversationMemberDO::getSubjectId, botId)
                .last("LIMIT 1"));
        if (member == null) {
            log.debug("Ignore bot typing: botId={} not member of conversationId={}", botId, conversationId);
            return false;
        }
        return true;
    }

    private void fanoutUser(Long tenantId, Long conversationId, Long userId, boolean typing) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("userId", userId);
        payload.put("senderType", "user");
        payload.put("typing", typing);
        sendTyping(tenantId, conversationId, userId, payload);
    }

    private void fanoutBot(Long tenantId, Long conversationId, Long botId, boolean typing) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("botId", botId);
        payload.put("senderType", "bot");
        payload.put("typing", typing);
        // Exclude nobody by userId; fanout to all user members
        List<Long> recipients = conversationService.listMemberUserIds(tenantId, conversationId);
        if (recipients.isEmpty()) {
            return;
        }
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(PresenceRealtimeTypes.DOMAIN)
                .type(PresenceRealtimeTypes.TYPING_UPDATED)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
    }

    private void sendTyping(Long tenantId, Long conversationId, Long excludeUserId, Map<String, Object> payload) {
        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(PresenceRealtimeTypes.DOMAIN)
                .type(PresenceRealtimeTypes.TYPING_UPDATED)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        List<Long> recipients = conversationService.listOtherMemberUserIds(tenantId, conversationId, excludeUserId);
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
    }

    static String redisKey(Long tenantId, Long conversationId, Long userId) {
        return "im:typing:" + tenantId + ":" + conversationId + ":" + userId;
    }

    static String botRedisKey(Long tenantId, Long conversationId, Long botId) {
        return "im:typing:" + tenantId + ":" + conversationId + ":bot:" + botId;
    }
}
