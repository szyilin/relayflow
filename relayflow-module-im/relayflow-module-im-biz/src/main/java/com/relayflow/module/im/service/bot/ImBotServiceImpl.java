package com.relayflow.module.im.service.bot;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendResult;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.service.card.ImBotCardSupport;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImBotTenantEnablementDO;
import com.relayflow.module.im.dal.dataobject.ImBotUserEnablementDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImBotTenantEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImBotUserEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImBotEnablePolicy;
import com.relayflow.module.im.enums.ImBotType;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImBotServiceImpl implements ImBotService {

    private static final int BOT_STATUS_ENABLED = 1;
    private static final int TENANT_ENABLED = 1;
    private static final String MESSAGE_TYPE_TEXT = "text";
    private static final String MESSAGE_TYPE_CARD = ImContentHelper.MESSAGE_TYPE_CARD;
    private static final String BLOCK_TYPE_TEXT = "text";
    private static final String BLOCK_TYPE_DEEPLINK = "deeplink";
    private static final String DEDUPE_CLIENT_PREFIX = "bot-dedupe:";

    private final ImBotMapper botMapper;
    private final ImBotTenantEnablementMapper tenantEnablementMapper;
    private final ImBotUserEnablementMapper userEnablementMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImMessageMapper messageMapper;
    private final ImConversationService conversationService;
    private final ImContentHelper contentHelper;
    private final RealtimeTransportApi realtimeTransportApi;
    private final TenantMemberApi tenantMemberApi;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImBotSendResult send(ImBotSendCommand command) {
        validateCommand(command);
        ImBotDO bot = requireActiveBot(command.getBotCode().trim());
        String scope = resolveScope(command.getTarget());
        Long userId = command.getTarget().getUserId();

        List<Long> tenantIds;
        if (ImBotSendTarget.SCOPE_ALL_ACTIVE_MEMBERSHIPS.equals(scope)) {
            tenantIds = tenantMemberApi.listActiveTenantIds(userId);
            if (tenantIds.isEmpty()) {
                throw new ServiceException(ErrorCodeConstants.BOT_SEND_INVALID);
            }
        } else {
            Long tenantId = command.getTarget().getTenantId();
            if (tenantId == null) {
                throw new ServiceException(ErrorCodeConstants.BOT_SEND_INVALID);
            }
            tenantIds = List.of(tenantId);
        }

        ImBotSendResult result = new ImBotSendResult();
        boolean allDedupe = true;
        for (Long tenantId : tenantIds) {
            SingleDelivery delivery = deliverInTenant(tenantId, userId, bot, command,
                    ImBotSendTarget.SCOPE_ALL_ACTIVE_MEMBERSHIPS.equals(scope));
            result.getMessageIds().add(delivery.messageId());
            allDedupe = allDedupe && delivery.dedupeHit();
        }
        result.setDedupeHit(allDedupe);
        return result;
    }

    @Override
    @Transactional
    public void ensureUserEnablementsOnActive(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        Long previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(tenantId);
            List<ImBotTenantEnablementDO> tenantEnablements = tenantEnablementMapper.selectList(
                    Wrappers.<ImBotTenantEnablementDO>lambdaQuery()
                            .eq(ImBotTenantEnablementDO::getTenantId, tenantId)
                            .eq(ImBotTenantEnablementDO::getEnabled, TENANT_ENABLED));
            for (ImBotTenantEnablementDO tenantEnablement : tenantEnablements) {
                ImBotDO bot = botMapper.selectById(tenantEnablement.getBotId());
                if (bot == null || !Objects.equals(bot.getStatus(), BOT_STATUS_ENABLED)) {
                    continue;
                }
                // System bots do not use user enablement rows.
                if (ImBotType.isSystem(bot.getType())) {
                    continue;
                }
                if (!ImBotEnablePolicy.autoEnableOnActive(bot.getEnablePolicy())) {
                    continue;
                }
                ensureUserEnablement(tenantId, userId, bot.getId());
            }
        } finally {
            restoreTenantContext(previous);
        }
    }

    private SingleDelivery deliverInTenant(Long tenantId, Long userId, ImBotDO bot, ImBotSendCommand command,
                                           boolean fanoutAutoEnable) {
        Long previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(tenantId);
            requireReachable(tenantId, userId, bot);
            // Non-system + fanout: optional lazy user enablement for opt-in bookkeeping only.
            if (fanoutAutoEnable
                    && !ImBotType.isSystem(bot.getType())
                    && ImBotEnablePolicy.autoEnableOnActive(bot.getEnablePolicy())) {
                ensureUserEnablement(tenantId, userId, bot.getId());
            }

            if (StringUtils.hasText(command.getDedupeKey())) {
                ImMessageDO existing = findByDedupeClientMsgId(tenantId, bot.getId(), userId, command.getDedupeKey());
                if (existing != null) {
                    return new SingleDelivery(existing.getId(), true);
                }
            }

            Long conversationId = ensureBotDm(tenantId, bot.getId(), userId);
            conversationService.lockConversation(tenantId, conversationId);

            MessageContentVO content = buildContent(command);
            String messageType = resolveMessageType(command);
            String contentJson = contentHelper.toJson(content);
            String preview = contentHelper.buildPreview(messageType, content);
            OffsetDateTime now = OffsetDateTime.now();
            long nextSeq = nextSeq(tenantId, conversationId);

            ImMessageDO message = new ImMessageDO();
            message.setTenantId(tenantId);
            message.setConversationId(conversationId);
            message.setSenderId(bot.getId());
            message.setSenderType(ImSenderType.BOT);
            message.setType(messageType);
            message.setContentJson(contentJson);
            if (StringUtils.hasText(command.getDedupeKey())) {
                message.setClientMsgId(buildDedupeClientMsgId(bot.getId(), userId, command.getDedupeKey()));
            }
            message.setSeq(nextSeq);
            message.setCreateTime(now);

            try {
                messageMapper.insert(message);
            } catch (DuplicateKeyException ex) {
                ImMessageDO raced = findByDedupeClientMsgId(tenantId, bot.getId(), userId, command.getDedupeKey());
                if (raced != null) {
                    return new SingleDelivery(raced.getId(), true);
                }
                throw ex;
            }

            ImConversationDO conversation = conversationMapper.selectById(conversationId);
            conversation.setLastMsgId(message.getId());
            conversation.setLastMsgAt(now);
            conversation.setLastMsgPreview(preview);
            conversationMapper.updateById(conversation);

            incrementUnreadForUser(tenantId, conversationId, userId);
            dispatchMessageNew(tenantId, userId, message, content, bot);

            return new SingleDelivery(message.getId(), false);
        } finally {
            restoreTenantContext(previous);
        }
    }

    private Long ensureBotDm(Long tenantId, Long botId, Long userId) {
        ImConversationDO existing = conversationMapper.selectOne(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .eq(ImConversationDO::getType, ImConversationType.BOT_DM)
                        .eq(ImConversationDO::getBotPeerBotId, botId)
                        .eq(ImConversationDO::getBotPeerUserId, userId));
        if (existing != null) {
            ensureBotDmMembers(tenantId, existing.getId(), botId, userId);
            return existing.getId();
        }

        ImConversationDO conversation = new ImConversationDO();
        conversation.setTenantId(tenantId);
        conversation.setType(ImConversationType.BOT_DM);
        conversation.setBotPeerBotId(botId);
        conversation.setBotPeerUserId(userId);
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException ex) {
            ImConversationDO raced = conversationMapper.selectOne(
                    Wrappers.<ImConversationDO>lambdaQuery()
                            .eq(ImConversationDO::getTenantId, tenantId)
                            .eq(ImConversationDO::getType, ImConversationType.BOT_DM)
                            .eq(ImConversationDO::getBotPeerBotId, botId)
                            .eq(ImConversationDO::getBotPeerUserId, userId));
            if (raced == null) {
                throw ex;
            }
            ensureBotDmMembers(tenantId, raced.getId(), botId, userId);
            return raced.getId();
        }

        insertMember(tenantId, conversation.getId(), ImMemberSubjectType.USER, userId, "member");
        insertMember(tenantId, conversation.getId(), ImMemberSubjectType.BOT, botId, "member");
        return conversation.getId();
    }

    private void ensureBotDmMembers(Long tenantId, Long conversationId, Long botId, Long userId) {
        ensureSubjectMember(tenantId, conversationId, ImMemberSubjectType.USER, userId);
        ensureSubjectMember(tenantId, conversationId, ImMemberSubjectType.BOT, botId);
    }

    private void ensureSubjectMember(Long tenantId, Long conversationId, String subjectType, Long subjectId) {
        Long count = conversationMemberMapper.selectCount(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, subjectType)
                        .eq(ImConversationMemberDO::getSubjectId, subjectId));
        if (count == null || count == 0) {
            insertMember(tenantId, conversationId, subjectType, subjectId, "member");
        }
    }

    private void insertMember(Long tenantId, Long conversationId, String subjectType, Long subjectId, String role) {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setTenantId(tenantId);
        member.setConversationId(conversationId);
        member.setSubjectType(subjectType);
        member.setSubjectId(subjectId);
        member.setRole(role);
        member.setReadSeq(0L);
        member.setUnreadCount(0);
        member.setJoinTime(OffsetDateTime.now());
        member.setPinned(0);
        conversationMemberMapper.insert(member);
    }

    private void ensureUserEnablement(Long tenantId, Long userId, Long botId) {
        Long count = userEnablementMapper.selectCount(
                Wrappers.<ImBotUserEnablementDO>lambdaQuery()
                        .eq(ImBotUserEnablementDO::getTenantId, tenantId)
                        .eq(ImBotUserEnablementDO::getUserId, userId)
                        .eq(ImBotUserEnablementDO::getBotId, botId));
        if (count != null && count > 0) {
            return;
        }
        ImBotUserEnablementDO row = new ImBotUserEnablementDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setBotId(botId);
        try {
            userEnablementMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // concurrent insert — already enabled
        }
    }

    /**
     * System bots: always reachable. Non-system: tenant enablement ∪ user enablement.
     */
    private void requireReachable(Long tenantId, Long userId, ImBotDO bot) {
        if (ImBotType.isSystem(bot.getType())) {
            return;
        }
        if (isTenantEnabled(tenantId, bot.getId()) || isUserEnabled(tenantId, userId, bot.getId())) {
            return;
        }
        throw new ServiceException(ErrorCodeConstants.BOT_NOT_ENABLED_FOR_TENANT);
    }

    private boolean isTenantEnabled(Long tenantId, Long botId) {
        ImBotTenantEnablementDO enablement = tenantEnablementMapper.selectOne(
                Wrappers.<ImBotTenantEnablementDO>lambdaQuery()
                        .eq(ImBotTenantEnablementDO::getTenantId, tenantId)
                        .eq(ImBotTenantEnablementDO::getBotId, botId)
                        .eq(ImBotTenantEnablementDO::getEnabled, TENANT_ENABLED));
        return enablement != null;
    }

    private boolean isUserEnabled(Long tenantId, Long userId, Long botId) {
        Long count = userEnablementMapper.selectCount(
                Wrappers.<ImBotUserEnablementDO>lambdaQuery()
                        .eq(ImBotUserEnablementDO::getTenantId, tenantId)
                        .eq(ImBotUserEnablementDO::getUserId, userId)
                        .eq(ImBotUserEnablementDO::getBotId, botId));
        return count != null && count > 0;
    }

    private ImBotDO requireActiveBot(String botCode) {
        ImBotDO bot = botMapper.selectOne(
                Wrappers.<ImBotDO>lambdaQuery()
                        .eq(ImBotDO::getCode, botCode)
                        .eq(ImBotDO::getStatus, BOT_STATUS_ENABLED));
        if (bot == null) {
            throw new ServiceException(ErrorCodeConstants.BOT_NOT_FOUND);
        }
        return bot;
    }

    private void validateCommand(ImBotSendCommand command) {
        if (command == null || !StringUtils.hasText(command.getBotCode())
                || command.getTarget() == null
                || command.getTarget().getUserId() == null) {
            throw new ServiceException(ErrorCodeConstants.BOT_SEND_INVALID);
        }
        boolean hasText = StringUtils.hasText(command.getText());
        boolean hasCard = command.getCard() != null;
        if (!hasText && !hasCard) {
            throw new ServiceException(ErrorCodeConstants.BOT_SEND_INVALID);
        }
        if (hasCard) {
            ImBotCardSupport.validate(command.getCard());
        }
        String scope = resolveScope(command.getTarget());
        if (!ImBotSendTarget.SCOPE_SINGLE.equals(scope)
                && !ImBotSendTarget.SCOPE_ALL_ACTIVE_MEMBERSHIPS.equals(scope)) {
            throw new ServiceException(ErrorCodeConstants.BOT_SEND_INVALID);
        }
    }

    private String resolveMessageType(ImBotSendCommand command) {
        return command.getCard() != null ? MESSAGE_TYPE_CARD : MESSAGE_TYPE_TEXT;
    }

    private String resolveScope(ImBotSendTarget target) {
        if (target == null || !StringUtils.hasText(target.getScope())) {
            return ImBotSendTarget.SCOPE_SINGLE;
        }
        return target.getScope().trim();
    }

    private MessageContentVO buildContent(ImBotSendCommand command) {
        MessageContentVO content = new MessageContentVO();
        content.setVersion(1);
        List<ContentBlockVO> blocks = new ArrayList<>();
        if (command.getCard() != null) {
            blocks.add(ImBotCardSupport.toContentBlock(command.getCard()));
            content.setBlocks(blocks);
            return content;
        }
        ContentBlockVO text = new ContentBlockVO();
        text.setType(BLOCK_TYPE_TEXT);
        text.setText(command.getText().trim());
        blocks.add(text);
        if (StringUtils.hasText(command.getRoute())
                || StringUtils.hasText(command.getEntityType())
                || StringUtils.hasText(command.getEntityId())) {
            ContentBlockVO link = new ContentBlockVO();
            link.setType(BLOCK_TYPE_DEEPLINK);
            link.setRoute(command.getRoute());
            link.setEntityType(command.getEntityType());
            link.setEntityId(command.getEntityId());
            blocks.add(link);
        }
        content.setBlocks(blocks);
        return content;
    }

    private void incrementUnreadForUser(Long tenantId, Long conversationId, Long userId) {
        ImConversationMemberDO member = conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (member == null) {
            return;
        }
        int unread = member.getUnreadCount() != null ? member.getUnreadCount() : 0;
        member.setUnreadCount(unread + 1);
        conversationMemberMapper.updateById(member);
    }

    private void dispatchMessageNew(Long tenantId, Long userId, ImMessageDO message, MessageContentVO content,
                                    ImBotDO bot) {
        MessageItemRespVO payload = new MessageItemRespVO();
        payload.setId(message.getId());
        payload.setConversationId(message.getConversationId());
        payload.setSenderId(message.getSenderId());
        payload.setSenderType(message.getSenderType());
        payload.setSenderNickname(bot.getName());
        payload.setType(message.getType());
        payload.setContent(content);
        payload.setClientMsgId(message.getClientMsgId());
        payload.setSeq(message.getSeq());
        payload.setCreateTime(message.getCreateTime());

        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(ImRealtimeTypes.MESSAGE_NEW)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        realtimeTransportApi.sendToUsers(tenantId, List.of(userId), envelope);
    }

    private ImMessageDO findByDedupeClientMsgId(Long tenantId, Long botId, Long userId, String dedupeKey) {
        if (!StringUtils.hasText(dedupeKey)) {
            return null;
        }
        return messageMapper.selectOne(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getClientMsgId, buildDedupeClientMsgId(botId, userId, dedupeKey)));
    }

    private String buildDedupeClientMsgId(Long botId, Long userId, String dedupeKey) {
        return DEDUPE_CLIENT_PREFIX + botId + ":" + userId + ":" + dedupeKey.trim();
    }

    private long nextSeq(Long tenantId, Long conversationId) {
        ImMessageDO latest = messageMapper.selectOne(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getConversationId, conversationId)
                        .orderByDesc(ImMessageDO::getSeq)
                        .last("LIMIT 1"));
        return latest == null ? 1L : latest.getSeq() + 1;
    }

    private void restoreTenantContext(Long previous) {
        if (previous != null) {
            TenantContextHolder.set(previous);
        } else {
            TenantContextHolder.clear();
        }
    }

    private record SingleDelivery(Long messageId, boolean dedupeHit) {
    }
}
