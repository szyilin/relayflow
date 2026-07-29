package com.relayflow.module.agent.skill.im;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.skill.BuiltinSkill;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistryImpl;
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
public class ImReadHistorySkill implements BuiltinSkill {

    public static final String CODE = "im.read_history";

    private final ImConversationReadApi conversationReadApi;

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("读取更早历史")
                .description("读取当前会话在 beforeSeq 之前的更早消息。conversationId 可省略；若传入必须等于当前会话。更早内容请用本工具，勿猜测其他群聊 ID。")
                .inputSchema("""
                        {"type":"object","properties":{"conversationId":{"type":"integer"},"beforeSeq":{"type":"integer","description":"只返回 seq 严格小于该值的消息"},"limit":{"type":"integer","minimum":1,"maximum":50}},"required":["beforeSeq"]}
                        """)
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
        if (input == null || input.getBeforeSeq() == null) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "im.read_history 需要 beforeSeq");
        }
        if (input.getConversationId() != null
                && !Objects.equals(input.getConversationId(), scope.getConversationId())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "禁止读取其他会话");
        }
        int limit = clamp(input.getLimit(), 20, 50);
        ImReadPrincipal principal = ImReadPrincipal.of(
                scope.getTenantId(), scope.getTriggerUserId(), scope.getBotId(), scope.getConversationId());
        return conversationReadApi.listHistory(
                principal, scope.getConversationId(), input.getBeforeSeq(), limit);
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
        private Long beforeSeq;
        private Integer limit;
    }
}
