package com.relayflow.module.im.api.conversation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.api.conversation.dto.ImMessageSummaryDTO;
import com.relayflow.module.im.api.conversation.dto.ImReadPrincipal;
import com.relayflow.module.im.config.ImBotCarrierProperties;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImConversationReadApiImpl implements ImConversationReadApi {

    private static final int ABSOLUTE_LIMIT = 50;

    private final ImMessageMapper messageMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImBotMapper botMapper;
    private final ImConversationService conversationService;
    private final ImContentHelper contentHelper;
    private final UserApi userApi;
    private final ImBotCarrierProperties carrierProperties;

    @Override
    public ImMessageSummaryDTO getMessage(ImReadPrincipal principal, Long messageId) {
        assertPrincipal(principal);
        if (messageId == null) {
            return null;
        }
        authorize(principal);
        ImMessageDO message = messageMapper.selectById(messageId);
        if (message == null
                || !Objects.equals(message.getTenantId(), principal.getTenantId())
                || !Objects.equals(message.getConversationId(), principal.getAllowedConversationId())) {
            return null;
        }
        return toSummary(message, loadNames(List.of(message)));
    }

    @Override
    public List<ImMessageSummaryDTO> listNearby(ImReadPrincipal principal, Long conversationId,
                                                long aroundSeq, int before, int after) {
        assertPrincipal(principal);
        assertConversationMatch(principal, conversationId);
        authorize(principal);
        int beforeN = clampWindow(before, carrierProperties.getGroupNearbyWindow());
        int afterN = clampWindow(after, carrierProperties.getGroupNearbyWindow());
        if (beforeN <= 0 && afterN <= 0) {
            return List.of();
        }

        List<ImMessageDO> rows = new ArrayList<>();
        if (beforeN > 0) {
            List<ImMessageDO> older = messageMapper.selectList(Wrappers.<ImMessageDO>lambdaQuery()
                    .eq(ImMessageDO::getTenantId, principal.getTenantId())
                    .eq(ImMessageDO::getConversationId, conversationId)
                    .lt(ImMessageDO::getSeq, aroundSeq)
                    .orderByDesc(ImMessageDO::getSeq)
                    .last("LIMIT " + beforeN));
            Collections.reverse(older);
            rows.addAll(older);
        }
        if (afterN > 0) {
            List<ImMessageDO> newer = messageMapper.selectList(Wrappers.<ImMessageDO>lambdaQuery()
                    .eq(ImMessageDO::getTenantId, principal.getTenantId())
                    .eq(ImMessageDO::getConversationId, conversationId)
                    .gt(ImMessageDO::getSeq, aroundSeq)
                    .orderByAsc(ImMessageDO::getSeq)
                    .last("LIMIT " + afterN));
            rows.addAll(newer);
        }
        return toSummaries(rows);
    }

    @Override
    public List<ImMessageSummaryDTO> listHistory(ImReadPrincipal principal, Long conversationId,
                                                 long beforeSeq, int limit) {
        assertPrincipal(principal);
        assertConversationMatch(principal, conversationId);
        authorize(principal);
        int safeLimit = clampHistory(limit);
        if (safeLimit <= 0 || beforeSeq <= 1) {
            return List.of();
        }
        List<ImMessageDO> older = messageMapper.selectList(Wrappers.<ImMessageDO>lambdaQuery()
                .eq(ImMessageDO::getTenantId, principal.getTenantId())
                .eq(ImMessageDO::getConversationId, conversationId)
                .lt(ImMessageDO::getSeq, beforeSeq)
                .orderByDesc(ImMessageDO::getSeq)
                .last("LIMIT " + safeLimit));
        Collections.reverse(older);
        return toSummaries(older);
    }

    private void authorize(ImReadPrincipal principal) {
        if (principal.getUserId() == null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_READ_DENIED);
        }
        try {
            conversationService.requireMembership(
                    principal.getTenantId(), principal.getAllowedConversationId(), principal.getUserId());
        } catch (ServiceException ex) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_READ_DENIED);
        }
        if (principal.getBotId() != null && !isBotMember(
                principal.getTenantId(), principal.getAllowedConversationId(), principal.getBotId())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_READ_DENIED);
        }
    }

    private boolean isBotMember(Long tenantId, Long conversationId, Long botId) {
        ImConversationMemberDO member = conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.BOT)
                        .eq(ImConversationMemberDO::getSubjectId, botId)
                        .last("LIMIT 1"));
        return member != null;
    }

    private void assertPrincipal(ImReadPrincipal principal) {
        if (principal == null
                || principal.getTenantId() == null
                || principal.getAllowedConversationId() == null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_READ_DENIED);
        }
    }

    private void assertConversationMatch(ImReadPrincipal principal, Long conversationId) {
        if (conversationId == null
                || !Objects.equals(conversationId, principal.getAllowedConversationId())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_READ_DENIED);
        }
    }

    private int clampWindow(int requested, int defaultValue) {
        int value = requested > 0 ? requested : defaultValue;
        int hard = Math.max(1, carrierProperties.getNearbyHardCap());
        return Math.min(value, Math.min(hard, ABSOLUTE_LIMIT));
    }

    private int clampHistory(int limit) {
        int soft = carrierProperties.getDmHistoryLimit();
        int hard = Math.min(ABSOLUTE_LIMIT, Math.max(1, carrierProperties.getDmHistoryHardCap()));
        int value = limit > 0 ? limit : soft;
        return Math.min(value, hard);
    }

    private List<ImMessageSummaryDTO> toSummaries(List<ImMessageDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names = loadNames(rows);
        List<ImMessageSummaryDTO> out = new ArrayList<>(rows.size());
        for (ImMessageDO row : rows) {
            out.add(toSummary(row, names));
        }
        return out;
    }

    private ImMessageSummaryDTO toSummary(ImMessageDO message, Map<Long, String> names) {
        ImMessageSummaryDTO dto = new ImMessageSummaryDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSeq(message.getSeq());
        dto.setSenderType(message.getSenderType());
        dto.setSenderId(message.getSenderId());
        dto.setCreateTime(message.getCreateTime());
        dto.setReplyToMsgId(message.getReplyToMsgId());
        if (message.getSenderId() != null) {
            dto.setSenderName(names.get(message.getSenderId()));
        }
        if (message.getRecalledAt() != null) {
            dto.setTextPreview("消息已撤回");
        } else {
            var content = contentHelper.fromJson(message.getContentJson());
            dto.setTextPreview(contentHelper.buildPreview(message.getType(), content));
        }
        return dto;
    }

    private Map<Long, String> loadNames(List<ImMessageDO> messages) {
        Map<Long, String> names = new HashMap<>();
        Set<Long> botIds = new HashSet<>();
        for (ImMessageDO message : messages) {
            if (message.getSenderId() == null || message.getSenderId() <= 0) {
                continue;
            }
            if (ImSenderType.USER.equals(message.getSenderType())) {
                if (names.containsKey(message.getSenderId())) {
                    continue;
                }
                UserBasicDTO user = userApi.getUserBasic(message.getSenderId());
                if (user != null && StringUtils.hasText(user.getNickname())) {
                    names.put(message.getSenderId(), user.getNickname());
                }
            } else if (ImSenderType.BOT.equals(message.getSenderType())) {
                botIds.add(message.getSenderId());
            }
        }
        if (!botIds.isEmpty()) {
            List<ImBotDO> bots = botMapper.selectBatchIds(botIds);
            for (ImBotDO bot : bots) {
                if (bot != null && bot.getId() != null && StringUtils.hasText(bot.getName())) {
                    names.put(bot.getId(), bot.getName());
                }
            }
        }
        return names;
    }
}
