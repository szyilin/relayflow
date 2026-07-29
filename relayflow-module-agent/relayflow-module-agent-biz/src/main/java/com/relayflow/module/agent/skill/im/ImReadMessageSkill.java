package com.relayflow.module.agent.skill.im;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.skill.BuiltinSkill;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistryImpl;
import com.relayflow.module.agent.skill.SkillTier;
import com.relayflow.module.im.api.conversation.ImConversationReadApi;
import com.relayflow.module.im.api.conversation.dto.ImMessageSummaryDTO;
import com.relayflow.module.im.api.conversation.dto.ImReadPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImReadMessageSkill implements BuiltinSkill {

    public static final String CODE = "im.read_message";

    private final ImConversationReadApi conversationReadApi;

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("读取单条消息")
                .description("读取当前会话内指定 messageId 的消息摘要。仅限当前触发会话，不可跨群/跨私聊。")
                .inputSchema("""
                        {"type":"object","properties":{"messageId":{"type":"integer","description":"消息 ID"}},"required":["messageId"]}
                        """)
                .domain("im")
                .tier(SkillTier.HOT)
                .build();
    }

    @Override
    public ToolCallback toolCallback() {
        return FunctionToolCallback.builder(SkillRegistryImpl.toToolName(CODE), this::read)
                .description(descriptor().getDescription())
                .inputType(Input.class)
                .build();
    }

    ImMessageSummaryDTO read(Input input) {
        ImCarrierToolScope scope = ImCarrierToolContext.require();
        if (input == null || input.getMessageId() == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "im.read_message 需要 messageId");
        }
        ImReadPrincipal principal = ImReadPrincipal.of(
                scope.getTenantId(), scope.getTriggerUserId(), scope.getBotId(), scope.getConversationId());
        ImMessageSummaryDTO dto = conversationReadApi.getMessage(principal, input.getMessageId());
        if (dto == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "消息不存在或无权读取");
        }
        return dto;
    }

    @Data
    public static class Input {
        private Long messageId;
    }
}
