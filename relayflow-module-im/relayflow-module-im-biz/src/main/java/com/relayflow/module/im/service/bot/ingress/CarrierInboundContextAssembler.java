package com.relayflow.module.im.service.bot.ingress;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.agent.api.carrier.dto.AgentCarrierHistoryMessageDTO;
import com.relayflow.module.im.api.conversation.ImConversationReadApi;
import com.relayflow.module.im.api.conversation.dto.ImMessageSummaryDTO;
import com.relayflow.module.im.api.conversation.dto.ImReadPrincipal;
import com.relayflow.module.im.config.ImBotCarrierProperties;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImSenderType;
import com.relayflow.module.im.service.bot.runtime.BotInboundContext;
import com.relayflow.module.im.service.message.ImContentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles short history + quote appendix for Agent carrier inbound.
 */
@Service
@RequiredArgsConstructor
public class CarrierInboundContextAssembler {

    private final ImConversationReadApi conversationReadApi;
    private final ImConversationMapper conversationMapper;
    private final ImMessageMapper messageMapper;
    private final ImContentHelper contentHelper;
    private final ImBotCarrierProperties properties;

    public Assembled assemble(BotInboundContext context) {
        if (context == null || context.getTenantId() == null || context.getConversationId() == null) {
            return Assembled.empty(context == null ? null : context.getInboundText());
        }
        ImConversationDO conversation = conversationMapper.selectById(context.getConversationId());
        if (conversation == null) {
            return Assembled.empty(context.getInboundText());
        }

        ImMessageDO trigger = context.getTriggerMessageId() == null
                ? null
                : messageMapper.selectById(context.getTriggerMessageId());
        long triggerSeq = trigger != null && trigger.getSeq() != null ? trigger.getSeq() : Long.MAX_VALUE;

        ImReadPrincipal principal = ImReadPrincipal.of(
                context.getTenantId(),
                context.getTriggerUserId(),
                context.getBotId(),
                context.getConversationId());

        List<ImMessageSummaryDTO> window;
        try {
            if (ImConversationType.BOT_DM.equals(conversation.getType())) {
                window = conversationReadApi.listHistory(
                        principal, context.getConversationId(), triggerSeq, properties.getDmHistoryLimit());
            } else if (ImConversationType.GROUP.equals(conversation.getType())) {
                int m = properties.getGroupNearbyWindow();
                window = conversationReadApi.listNearby(
                        principal, context.getConversationId(), triggerSeq, m, m);
            } else {
                window = List.of();
            }
        } catch (Exception ex) {
            window = List.of();
        }

        List<AgentCarrierHistoryMessageDTO> history = toHistory(window, properties.getHistoryMaxChars());
        String inboundText = buildInboundText(context.getInboundText(), trigger);
        return new Assembled(inboundText, history);
    }

    private String buildInboundText(String raw, ImMessageDO trigger) {
        String base = raw == null ? "" : raw.trim();
        if (trigger == null || trigger.getReplyToMsgId() == null) {
            return base;
        }
        ImMessageDO quoted = messageMapper.selectById(trigger.getReplyToMsgId());
        if (quoted == null
                || !quoted.getConversationId().equals(trigger.getConversationId())
                || !quoted.getTenantId().equals(trigger.getTenantId())) {
            return base;
        }
        String preview;
        if (quoted.getRecalledAt() != null) {
            preview = "消息已撤回";
        } else {
            preview = contentHelper.buildPreview(quoted.getType(), contentHelper.fromJson(quoted.getContentJson()));
        }
        int max = Math.max(20, properties.getReplyPreviewMaxChars());
        if (preview != null && preview.length() > max) {
            preview = preview.substring(0, max) + "…";
        }
        String name = resolveSenderLabel(quoted);
        String appendix = "[引用消息 id=" + quoted.getId()
                + " 发送者=" + name
                + "]\n" + (preview == null ? "" : preview);
        if (!StringUtils.hasText(base)) {
            return appendix;
        }
        return base + "\n\n" + appendix;
    }

    private String resolveSenderLabel(ImMessageDO message) {
        if (ImSenderType.BOT.equals(message.getSenderType())) {
            return "bot:" + message.getSenderId();
        }
        if (ImSenderType.SYSTEM.equals(message.getSenderType())) {
            return "system";
        }
        return "user:" + message.getSenderId();
    }

    private List<AgentCarrierHistoryMessageDTO> toHistory(List<ImMessageSummaryDTO> window, int maxChars) {
        if (window == null || window.isEmpty()) {
            return List.of();
        }
        List<AgentCarrierHistoryMessageDTO> turns = new ArrayList<>();
        int used = 0;
        // Prefer keeping newer messages when truncating: walk from end.
        List<ImMessageSummaryDTO> chron = window;
        List<AgentCarrierHistoryMessageDTO> reversed = new ArrayList<>();
        for (int i = chron.size() - 1; i >= 0; i--) {
            ImMessageSummaryDTO row = chron.get(i);
            String content = formatContent(row);
            if (!StringUtils.hasText(content)) {
                continue;
            }
            if (used + content.length() > maxChars && !reversed.isEmpty()) {
                break;
            }
            AgentCarrierHistoryMessageDTO turn = new AgentCarrierHistoryMessageDTO();
            turn.setRole(ImSenderType.BOT.equals(row.getSenderType()) ? "assistant" : "user");
            turn.setContent(content);
            reversed.add(turn);
            used += content.length();
        }
        for (int i = reversed.size() - 1; i >= 0; i--) {
            turns.add(reversed.get(i));
        }
        return turns;
    }

    private String formatContent(ImMessageSummaryDTO row) {
        String preview = row.getTextPreview() == null ? "" : row.getTextPreview().trim();
        if (!StringUtils.hasText(preview)) {
            return "";
        }
        if (ImSenderType.BOT.equals(row.getSenderType())) {
            return preview;
        }
        String name = StringUtils.hasText(row.getSenderName()) ? row.getSenderName() : "成员";
        return name + ": " + preview;
    }

    public record Assembled(String inboundText, List<AgentCarrierHistoryMessageDTO> history) {
        static Assembled empty(String inboundText) {
            return new Assembled(inboundText, List.of());
        }
    }
}
