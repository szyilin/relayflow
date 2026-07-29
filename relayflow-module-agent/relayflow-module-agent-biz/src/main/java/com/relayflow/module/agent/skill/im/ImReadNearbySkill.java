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

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ImReadNearbySkill implements BuiltinSkill {

    public static final String CODE = "im.read_nearby";

    private final ImConversationReadApi conversationReadApi;

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("读取附近消息")
                .description("读取当前会话中某 seq 前后各若干条消息。conversationId 可省略；若传入必须等于当前会话。")
                .inputSchema("""
                        {"type":"object","properties":{"conversationId":{"type":"integer"},"aroundSeq":{"type":"integer"},"before":{"type":"integer","minimum":0,"maximum":20},"after":{"type":"integer","minimum":0,"maximum":20}},"required":["aroundSeq"]}
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

    List<ImMessageSummaryDTO> read(Input input) {
        ImCarrierToolScope scope = ImCarrierToolContext.require();
        if (input == null || input.getAroundSeq() == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "im.read_nearby 需要 aroundSeq");
        }
        if (input.getConversationId() != null
                && !Objects.equals(input.getConversationId(), scope.getConversationId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "禁止读取其他会话");
        }
        int before = clamp(input.getBefore(), 5, 20);
        int after = clamp(input.getAfter(), 5, 20);
        ImReadPrincipal principal = ImReadPrincipal.of(
                scope.getTenantId(), scope.getTriggerUserId(), scope.getBotId(), scope.getConversationId());
        return conversationReadApi.listNearby(
                principal, scope.getConversationId(), input.getAroundSeq(), before, after);
    }

    private static int clamp(Integer value, int defaultValue, int max) {
        if (value == null || value < 1) {
            return defaultValue;
        }
        return Math.min(value, max);
    }

    @Data
    public static class Input {
        private Long conversationId;
        private Long aroundSeq;
        private Integer before;
        private Integer after;
    }
}
