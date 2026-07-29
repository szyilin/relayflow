package com.relayflow.module.im.service.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.MessageReactionAggVO;
import com.relayflow.module.im.controller.app.vo.ToggleReactionRespVO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.dataobject.ImMessageReactionDO;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.dal.mapper.ImMessageReactionMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImMessageReactionService {

    private static final int MAX_EMOJI_LENGTH = 32;
    private static final int MAX_DISTINCT_PER_USER = 20;

    private final ImMessageMapper messageMapper;
    private final ImMessageReactionMapper reactionMapper;
    private final ImConversationService conversationService;
    private final RealtimeTransportApi realtimeTransportApi;

    @Transactional
    public ToggleReactionRespVO toggle(Long messageId, String rawEmoji) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        String emoji = normalizeEmoji(rawEmoji);

        ImMessageDO message = messageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getTenantId(), tenantId)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_NOT_FOUND);
        }
        conversationService.requireMembership(tenantId, message.getConversationId(), userId);
        if (message.getRecalledAt() != null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_REACTION_DENIED);
        }

        ImMessageReactionDO existing = reactionMapper.selectOne(
                Wrappers.<ImMessageReactionDO>lambdaQuery()
                        .eq(ImMessageReactionDO::getTenantId, tenantId)
                        .eq(ImMessageReactionDO::getMessageId, messageId)
                        .eq(ImMessageReactionDO::getUserId, userId)
                        .eq(ImMessageReactionDO::getEmoji, emoji));
        if (existing != null) {
            reactionMapper.deleteById(existing.getId());
        } else {
            long distinct = reactionMapper.selectCount(
                    Wrappers.<ImMessageReactionDO>lambdaQuery()
                            .eq(ImMessageReactionDO::getTenantId, tenantId)
                            .eq(ImMessageReactionDO::getMessageId, messageId)
                            .eq(ImMessageReactionDO::getUserId, userId));
            if (distinct >= MAX_DISTINCT_PER_USER) {
                throw new ServiceException(ErrorCodeConstants.MESSAGE_REACTION_INVALID);
            }
            ImMessageReactionDO row = new ImMessageReactionDO();
            row.setTenantId(tenantId);
            row.setConversationId(message.getConversationId());
            row.setMessageId(messageId);
            row.setUserId(userId);
            row.setEmoji(emoji);
            row.setCreator(userId);
            row.setCreateTime(OffsetDateTime.now());
            reactionMapper.insert(row);
        }

        List<MessageReactionAggVO> forViewer = aggregateForMessage(tenantId, messageId, userId);
        fanoutReactionUpdated(tenantId, message.getConversationId(), messageId);
        ToggleReactionRespVO resp = new ToggleReactionRespVO();
        resp.setMessageId(messageId);
        resp.setConversationId(message.getConversationId());
        resp.setReactions(forViewer);
        return resp;
    }

    public Map<Long, List<MessageReactionAggVO>> aggregateForMessages(Long tenantId, Long viewerUserId,
                                                                      List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        List<ImMessageReactionDO> rows = reactionMapper.selectList(
                Wrappers.<ImMessageReactionDO>lambdaQuery()
                        .eq(ImMessageReactionDO::getTenantId, tenantId)
                        .in(ImMessageReactionDO::getMessageId, messageIds));
        Map<Long, List<ImMessageReactionDO>> byMessage = new HashMap<>();
        for (ImMessageReactionDO row : rows) {
            byMessage.computeIfAbsent(row.getMessageId(), ignored -> new ArrayList<>()).add(row);
        }
        Map<Long, List<MessageReactionAggVO>> result = new HashMap<>();
        for (Long messageId : messageIds) {
            result.put(messageId, toAgg(byMessage.getOrDefault(messageId, List.of()), viewerUserId));
        }
        return result;
    }

    public List<MessageReactionAggVO> aggregateForMessage(Long tenantId, Long messageId, Long viewerUserId) {
        return aggregateForMessages(tenantId, viewerUserId, List.of(messageId))
                .getOrDefault(messageId, List.of());
    }

    private void fanoutReactionUpdated(Long tenantId, Long conversationId, Long messageId) {
        List<Long> recipients = conversationService.listMemberUserIds(tenantId, conversationId);
        if (recipients.isEmpty()) {
            return;
        }
        List<ImMessageReactionDO> rows = reactionMapper.selectList(
                Wrappers.<ImMessageReactionDO>lambdaQuery()
                        .eq(ImMessageReactionDO::getTenantId, tenantId)
                        .eq(ImMessageReactionDO::getMessageId, messageId));
        for (Long recipientId : recipients) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversationId", conversationId);
            payload.put("messageId", messageId);
            payload.put("reactions", toAgg(rows, recipientId));
            RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                    .domain(ImRealtimeTypes.DOMAIN)
                    .type(ImRealtimeTypes.REACTION_UPDATED)
                    .ts(System.currentTimeMillis())
                    .payload(payload)
                    .build();
            realtimeTransportApi.sendToUsers(tenantId, List.of(recipientId), envelope);
        }
    }

    static String normalizeEmoji(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_REACTION_INVALID);
        }
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC);
        if (!StringUtils.hasText(normalized) || normalized.length() > MAX_EMOJI_LENGTH) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_REACTION_INVALID);
        }
        return normalized;
    }

    static List<MessageReactionAggVO> toAgg(List<ImMessageReactionDO> rows, Long viewerUserId) {
        Map<String, Set<Long>> usersByEmoji = new LinkedHashMap<>();
        for (ImMessageReactionDO row : rows) {
            usersByEmoji.computeIfAbsent(row.getEmoji(), ignored -> new HashSet<>()).add(row.getUserId());
        }
        List<MessageReactionAggVO> list = new ArrayList<>();
        for (Map.Entry<String, Set<Long>> entry : usersByEmoji.entrySet()) {
            MessageReactionAggVO vo = new MessageReactionAggVO();
            vo.setEmoji(entry.getKey());
            vo.setCount(entry.getValue().size());
            vo.setMe(viewerUserId != null && entry.getValue().contains(viewerUserId));
            list.add(vo);
        }
        list.sort(Comparator
                .comparing(MessageReactionAggVO::getCount, Comparator.reverseOrder())
                .thenComparing(MessageReactionAggVO::getEmoji));
        return list;
    }
}
