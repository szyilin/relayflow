package com.relayflow.module.agent.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.skill.task.TaskSearchSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillRegistryImplTest {

    private BuiltinSkill searchSkill;
    private SkillRegistryImpl registry;

    @BeforeEach
    void setUp() {
        searchSkill = mock(BuiltinSkill.class);
        when(searchSkill.descriptor()).thenReturn(SkillDescriptor.builder()
                .code(TaskSearchSkill.CODE)
                .name("搜索任务")
                .description("desc")
                .domain("task")
                .tier(com.relayflow.module.agent.skill.SkillTier.HOT)
                .build());
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("task_search")
                .description("desc")
                .inputSchema("{}")
                .build());
        when(searchSkill.toolCallback()).thenReturn(callback);
        registry = new SkillRegistryImpl(List.of(searchSkill), new ObjectMapper());
    }

    @Test
    void resolve_onlyBoundSkills() {
        List<ToolCallback> tools = registry.resolveToolCallbacks(List.of(TaskSearchSkill.CODE, "unknown.x"));
        assertEquals(1, tools.size());
        assertEquals("task_search", tools.get(0).getToolDefinition().name());
    }

    @Test
    void validate_rejectsUnknown() {
        assertThrows(ServiceException.class,
                () -> registry.validateSkillCodes(List.of(TaskSearchSkill.CODE, "nope")));
    }

    @Test
    void parseAndSerialize_roundTrip() {
        String json = registry.toSkillIdsJson(List.of(TaskSearchSkill.CODE, " "));
        assertEquals("[\"task.search\"]", json);
        assertEquals(List.of(TaskSearchSkill.CODE), registry.parseSkillIdsJson(json));
        assertTrue(registry.parseSkillIdsJson("[]").isEmpty());
        assertFalse(registry.isKnown("nope"));
    }

    @Test
    void toToolName_replacesDots() {
        assertEquals("task_list_due", SkillRegistryImpl.toToolName("task.list_due"));
    }
}
