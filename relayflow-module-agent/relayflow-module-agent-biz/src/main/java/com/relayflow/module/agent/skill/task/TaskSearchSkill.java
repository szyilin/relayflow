package com.relayflow.module.agent.skill.task;

import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.agent.skill.BuiltinSkill;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistryImpl;
import com.relayflow.module.agent.skill.SkillTier;
import com.relayflow.module.task.api.item.TaskItemApi;
import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskSearchSkill implements BuiltinSkill {

    public static final String CODE = "task.search";

    private final TaskItemApi taskItemApi;

    @Override
    public SkillDescriptor descriptor() {
        return SkillDescriptor.builder()
                .code(CODE)
                .name("搜索任务")
                .description("按关键字搜索当前用户可见的任务，返回任务 ID、标题与状态。")
                .inputSchema("""
                        {"type":"object","properties":{"keyword":{"type":"string"},"limit":{"type":"integer","minimum":1,"maximum":50}},"required":[]}
                        """)
                .domain("task")
                .tier(SkillTier.HOT)
                .build();
    }

    @Override
    public ToolCallback toolCallback() {
        return FunctionToolCallback.builder(SkillRegistryImpl.toToolName(CODE), this::search)
                .description(descriptor().getDescription())
                .inputType(TaskSearchInput.class)
                .build();
    }

    List<TaskSearchRespDTO> search(TaskSearchInput input) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        String keyword = input == null || !StringUtils.hasText(input.getKeyword())
                ? ""
                : input.getKeyword().trim();
        int limit = normalizeLimit(input == null ? null : input.getLimit(), 10);
        return taskItemApi.searchTasks(tenantId, userId, keyword, limit);
    }

    public static int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, 50);
    }
}
