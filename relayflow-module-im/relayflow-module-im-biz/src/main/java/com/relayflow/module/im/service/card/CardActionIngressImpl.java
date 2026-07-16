package com.relayflow.module.im.service.card;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.api.card.CardActionContext;
import com.relayflow.module.im.api.card.CardActionHandler;
import com.relayflow.module.im.api.card.CardActionResult;
import com.relayflow.module.im.controller.app.vo.CardActionReqVO;
import com.relayflow.module.im.controller.app.vo.CardActionRespVO;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.card.CardActionItemVO;
import com.relayflow.module.im.controller.app.vo.card.CardBehaviorVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CardActionIngressImpl implements CardActionIngress {

    private final ImConversationService conversationService;
    private final ImMessageMapper messageMapper;
    private final ImConversationMapper conversationMapper;
    private final ImBotMapper botMapper;
    private final ImContentHelper contentHelper;
    private final CardActionHandlerRegistry handlerRegistry;
    private final RealtimeTransportApi realtimeTransportApi;

    /** V1 single-instance idempotency store: tenantId:clientActionId → response. */
    private final ConcurrentHashMap<String, CardActionRespVO> idempotencyStore = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public CardActionRespVO handle(CardActionReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        validateRequest(request);

        String idempotencyKey = tenantId + ":" + request.getClientActionId().trim();
        CardActionRespVO cached = idempotencyStore.get(idempotencyKey);
        if (cached != null) {
            return cached;
        }

        conversationService.requireMembership(tenantId, request.getConversationId(), userId);

        ImMessageDO message = messageMapper.selectOne(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getId, request.getMessageId())
                        .eq(ImMessageDO::getConversationId, request.getConversationId()));
        if (message == null) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }

        MessageContentVO content = contentHelper.fromJson(message.getContentJson());
        ContentBlockVO cardBlock = contentHelper.firstCardBlock(content);
        if (cardBlock == null) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }

        assertNotExpired(cardBlock);
        CardActionItemVO action = findAction(cardBlock, request.getActionId());
        CardBehaviorVO behavior = action.getBehavior();
        if (behavior == null || !ImBotCardSupport.BEHAVIOR_CALLBACK.equals(behavior.getType())) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }
        if (!Objects.equals(request.getActionKey().trim(),
                behavior.getActionKey() != null ? behavior.getActionKey().trim() : null)) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }

        String botCode = resolveBotCode(message);
        CardActionHandler handler = handlerRegistry.require(request.getActionKey().trim());
        CardActionResult result = handler.handle(CardActionContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .botCode(botCode)
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .actionId(action.getId())
                .actionKey(request.getActionKey().trim())
                .payload(request.getPayload() != null ? request.getPayload() : behavior.getPayload())
                .formValues(request.getFormValues())
                .clientActionId(request.getClientActionId().trim())
                .build());

        MessageItemRespVO updatedMessage = null;
        if (result != null && result.getCard() != null) {
            updatedMessage = patchCardAndPush(tenantId, message, content, cardBlock, result.getCard(), botCode);
        }

        CardActionRespVO response = new CardActionRespVO();
        if (result != null && result.getToast() != null) {
            CardActionRespVO.CardToastRespVO toast = new CardActionRespVO.CardToastRespVO();
            toast.setType(result.getToast().getType());
            toast.setContent(result.getToast().getContent());
            response.setToast(toast);
        }
        response.setMessage(updatedMessage);

        idempotencyStore.put(idempotencyKey, response);
        return response;
    }

    private MessageItemRespVO patchCardAndPush(Long tenantId, ImMessageDO message, MessageContentVO content,
                                               ContentBlockVO oldCard, Map<String, Object> newCardMap,
                                               String botCode) {
        ContentBlockVO replacement = ImBotCardSupport.fromMap(newCardMap);
        List<ContentBlockVO> blocks = new ArrayList<>();
        boolean replaced = false;
        for (ContentBlockVO block : content.getBlocks()) {
            if (!replaced && ImContentHelper.BLOCK_TYPE_CARD.equals(block.getType())
                    && Objects.equals(block.getCardId(), oldCard.getCardId())) {
                blocks.add(replacement);
                replaced = true;
            } else if (!replaced && ImContentHelper.BLOCK_TYPE_CARD.equals(block.getType())) {
                blocks.add(replacement);
                replaced = true;
            } else {
                blocks.add(block);
            }
        }
        if (!replaced) {
            blocks.add(replacement);
        }
        content.setBlocks(blocks);

        String contentJson = contentHelper.toJson(content);
        message.setContentJson(contentJson);
        messageMapper.updateById(message);

        String preview = contentHelper.buildPreview(ImContentHelper.MESSAGE_TYPE_CARD, content);
        ImConversationDO conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null) {
            conversation.setLastMsgPreview(preview);
            conversationMapper.updateById(conversation);
        }

        MessageItemRespVO payload = new MessageItemRespVO();
        payload.setId(message.getId());
        payload.setConversationId(message.getConversationId());
        payload.setSenderId(message.getSenderId());
        payload.setSenderType(message.getSenderType());
        payload.setType(message.getType());
        payload.setContent(content);
        payload.setClientMsgId(message.getClientMsgId());
        payload.setSeq(message.getSeq());
        payload.setCreateTime(message.getCreateTime());
        if (StringUtils.hasText(botCode)) {
            ImBotDO bot = botMapper.selectOne(
                    Wrappers.<ImBotDO>lambdaQuery().eq(ImBotDO::getCode, botCode));
            if (bot != null) {
                payload.setSenderNickname(bot.getName());
            }
        }

        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(ImRealtimeTypes.MESSAGE_UPDATED)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();
        List<Long> recipients = conversationService.listMemberUserIds(tenantId, message.getConversationId());
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
        return payload;
    }

    private void validateRequest(CardActionReqVO request) {
        if (request == null || request.getMessageId() == null || request.getConversationId() == null
                || !StringUtils.hasText(request.getActionId())
                || !StringUtils.hasText(request.getActionKey())
                || !StringUtils.hasText(request.getClientActionId())) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }
    }

    private void assertNotExpired(ContentBlockVO cardBlock) {
        if (cardBlock.getMeta() == null || !StringUtils.hasText(cardBlock.getMeta().getExpiresAt())) {
            return;
        }
        try {
            OffsetDateTime expiresAt = OffsetDateTime.parse(cardBlock.getMeta().getExpiresAt().trim());
            if (expiresAt.isBefore(OffsetDateTime.now())) {
                throw new ServiceException(ErrorCodeConstants.CARD_ACTION_EXPIRED);
            }
        } catch (DateTimeParseException ex) {
            throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
        }
    }

    private CardActionItemVO findAction(ContentBlockVO cardBlock, String actionId) {
        if (CollectionUtils.isEmpty(cardBlock.getActions()) || !StringUtils.hasText(actionId)) {
            throw new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID);
        }
        return cardBlock.getActions().stream()
                .filter(action -> action != null && actionId.trim().equals(action.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(ErrorCodeConstants.CARD_ACTION_INVALID));
    }

    private String resolveBotCode(ImMessageDO message) {
        if (!ImSenderType.BOT.equals(message.getSenderType()) || message.getSenderId() == null) {
            return null;
        }
        ImBotDO bot = botMapper.selectById(message.getSenderId());
        return bot != null ? bot.getCode() : null;
    }
}
