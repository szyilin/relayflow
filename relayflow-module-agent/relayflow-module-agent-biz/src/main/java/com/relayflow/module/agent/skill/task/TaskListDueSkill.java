package com.relayflow.module.agent.skill.task;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.skill.BuiltinSkill;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistryImpl;
import com.relayflow.module.agent.skill.SkillTier;
import com.relayflow.module.task.api.item.TaskItemApi;
import com.relayflow.module.task.api.item.dto.TaskDueRangeRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskListDueSkill implements BuiltinSkill {

    public static final String CODE = "task.list_due";

    private final TaskItemApi taskItemApi;

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("截止任务列表")
                .description("列出当前用户在指定时间范围内到期的待办任务（含截止时间）。")
                .inputSchema("""
                        {"type":"object","properties":{"from":{"type":"string","description":"ISO-8601 start inclusive"},"to":{"type":"string","description":"ISO-8601 end exclusive"},"limit":{"type":"integer","minimum":1,"maximum":50}},"required":["from","to"]}
                        """)
                .domain("task")
                .tier(SkillTier.HOT)
                .build();
    }

    @Override
    public ToolCallback toolCallback() {
        return FunctionToolCallback.builder(SkillRegistryImpl.toToolName(CODE), this::listDue)
                .description(descriptor().getDescription())
                .inputType(TaskListDueInput.class)
                .build();
    }

    List<TaskDueRangeRespDTO> listDue(TaskListDueInput input) {
        if (input == null || !StringUtils.hasText(input.getFrom()) || !StringUtils.hasText(input.getTo())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "task.list_due 需要 from 与 to（ISO-8601）");
        }
        OffsetDateTime from = parseOffset(input.getFrom().trim());
        OffsetDateTime to = parseOffset(input.getTo().trim());
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        int limit = TaskSearchSkill.normalizeLimit(input.getLimit(), 20);
        return taskItemApi.listDueRange(tenantId, userId, from, to, limit);
    }

    private static OffsetDateTime parseOffset(String raw) {
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_DEF_INVALID.getCode(),
                    "无法解析时间：" + raw);
        }
    }
}
