package com.relayflow.module.im.service.bot.ingress;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.service.bot.runtime.BotInboundContext;
import com.relayflow.module.im.service.message.ImContentHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * After a group user message is persisted, resolve bot mentions and invoke {@link BotIngress}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBotMentionDispatcher {

    private static final int BOT_STATUS_ENABLED = 1;

    private final ImContentHelper contentHelper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImBotMapper botMapper;
    private final BotIngress botIngress;

    /**
     * Best-effort. Must never throw to the message send path.
     */
    public void dispatchAfterUserMessage(Long tenantId, Long conversationId, Long triggerUserId,
                                         Long triggerMessageId, MessageContentVO content) {
        try {
            if (tenantId == null || conversationId == null || content == null) {
                return;
            }
            ImConversationDO conversation = conversationMapper.selectById(conversationId);
            if (conversation == null || !ImConversationType.GROUP.equals(conversation.getType())) {
                return;
            }
            List<ContentBlockVO> mentions = contentHelper.extractBotMentionBlocks(content);
            if (mentions.isEmpty()) {
                return;
            }

            Set<Long> memberBotIds = loadMemberBotIds(tenantId, conversationId);
            if (memberBotIds.isEmpty()) {
                return;
            }

            String inboundText = contentHelper.buildTextPreview(content);
            Set<Long> dispatched = new HashSet<>();
            for (ContentBlockVO mention : mentions) {
                ImBotDO bot = resolveBot(mention);
                if (bot == null || !Objects.equals(bot.getStatus(), BOT_STATUS_ENABLED)) {
                    continue;
                }
                if (!memberBotIds.contains(bot.getId())) {
                    log.debug("Ignore mention of non-member bot: code={}, conversationId={}",
                            bot.getCode(), conversationId);
                    continue;
                }
                if (!dispatched.add(bot.getId())) {
                    continue;
                }
                botIngress.onInbound(BotInboundContext.builder()
                        .tenantId(tenantId)
                        .conversationId(conversationId)
                        .botId(bot.getId())
                        .botCode(bot.getCode())
                        .handlerKind(bot.getHandlerKind())
                        .botName(bot.getName())
                        .triggerUserId(triggerUserId)
                        .triggerMessageId(triggerMessageId)
                        .inboundText(inboundText)
                        .build());
            }
        } catch (Exception ex) {
            log.warn("GroupBotMentionDispatcher failed: conversationId={}, triggerMessageId={}",
                    conversationId, triggerMessageId, ex);
        }
    }

    private Set<Long> loadMemberBotIds(Long tenantId, Long conversationId) {
        List<ImConversationMemberDO> members = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.BOT));
        Set<Long> ids = new HashSet<>();
        for (ImConversationMemberDO member : members) {
            if (member.getSubjectId() != null) {
                ids.add(member.getSubjectId());
            }
        }
        return ids;
    }

    private ImBotDO resolveBot(ContentBlockVO mention) {
        if (StringUtils.hasText(mention.getBotCode())) {
            return botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                    .eq(ImBotDO::getCode, mention.getBotCode().trim()));
        }
        if (mention.getSubjectId() != null) {
            return botMapper.selectById(mention.getSubjectId());
        }
        return null;
    }
}
